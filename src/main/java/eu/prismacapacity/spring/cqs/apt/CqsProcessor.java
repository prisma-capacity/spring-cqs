/*
 * Copyright © 2020 PRISMA European Capacity Platform GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.prismacapacity.spring.cqs.apt;

import java.io.*;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import lombok.AllArgsConstructor;
import lombok.val;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.google.auto.service.AutoService;

import eu.prismacapacity.spring.cqs.cmd.CommandHandler;
import eu.prismacapacity.spring.cqs.cmd.RespondingCommandHandler;
import eu.prismacapacity.spring.cqs.query.QueryHandler;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions(CqsProcessor.VERBOSE)
@AutoService(Processor.class)
public class CqsProcessor extends javax.annotation.processing.AbstractProcessor {
	public static final String VERBOSE = "cqs.verbose";

	private static final String GENERATED_TYPE_SUFFIX = "$GeneratedCqsComponent";
	private Messager messager;
	private final Set<TypeElement> alreadyGenerated = new HashSet<>();
	private final HashMap<String, String> cqsInterfaces = new HashMap<>();

	static Set<HandlerInfo> findImplementors(ProcessingEnvironment processingEnv, Set<? extends Element> rootElements,
			Set<String> cqsInterfaces) {
		HashSet<HandlerInfo> ret = new HashSet<>();

		for (Element codeElement : rootElements) {
			TypeElement te = ReflectionHelper.getTypeElement(codeElement);
			if (te != null && !ReflectionHelper.isGenerated(te) && !ReflectionHelper.isAbstract(te)) {

				Set<? extends TypeMirror> interfaces = ReflectionHelper.findInterfaces(te);
				if (!interfaces.isEmpty()) {
					Optional<? extends TypeMirror> found = interfaces.stream().filter(cn -> {
						return cqsInterfaces.contains(ReflectionHelper.getCanonicalName(cn).toString());
					}).findFirst();

					found.ifPresent(r -> {

						ExecutableElement interfaceHandleMethod = ReflectionHelper.asMethodElement(
								processingEnv.getElementUtils().getAllMembers(ReflectionHelper.getTypeElement(r))
										.stream().filter(m -> m.getKind() == ElementKind.METHOD)
										.filter(m -> "handle".equals(m.getSimpleName().toString())).findFirst()
										.orElseThrow(() -> new IllegalStateException("no handle method on " + r)));

						ExecutableElement implHandleMethod = ReflectionHelper.asMethodElement(processingEnv
								.getElementUtils().getAllMembers(te).stream()
								.filter(m -> m.getKind() == ElementKind.METHOD).map(m -> (ExecutableElement) m)
								.filter(m -> processingEnv.getElementUtils().overrides(m, interfaceHandleMethod, te))
								.findFirst().orElseThrow(() -> new IllegalStateException("no handle method on " + te)));

						TypeElement parType = ReflectionHelper
								.getTypeElement(implHandleMethod.getParameters().get(0).asType());
						TypeElement retType = ReflectionHelper.getTypeElement(implHandleMethod.getReturnType());

						ret.add(new HandlerInfo(te, ReflectionHelper.getTypeElement(r), parType, retType));
					});
				}
			}
		}
		return ret;
	}

	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);

		messager = processingEnv.getMessager();
		messager.printMessage(Kind.NOTE, "CqsProcessor " + ManifestHelper.getVersion("CQS Spring"));
		cqsInterfaces.put(QueryHandler.class.getCanonicalName(), "/QueryHandler.java.tpl");
		cqsInterfaces.put(CommandHandler.class.getCanonicalName(), "/CommandHandler.java.tpl");
		cqsInterfaces.put(RespondingCommandHandler.class.getCanonicalName(), "/CommandHandler.java.tpl");
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		long start = System.currentTimeMillis();
		try {
			if (!roundEnv.processingOver()) {
				findImplementors(processingEnv, roundEnv.getRootElements(), cqsInterfaces.keySet())
						.forEach(this::createSource);
			}
		} catch (Throwable e) {
			error(e);
		} finally {
			log("CqsProcessor runtime: " + (System.currentTimeMillis() - start) + "ms");
		}
		return false;
	}

	private void error(Throwable e) {
		error("unexpected Exception", e);
	}

	private void error(String s, Throwable e) {
		messager.printMessage(Kind.ERROR, s + "\n" + render(e));
	}

	private void error(String s, Element t) {
		messager.printMessage(Kind.ERROR, s, t);
	}

	private void warn(String s, Element t) {
		messager.printMessage(Kind.WARNING, s, t);
	}

	private void log(String msg) {
		if (verbose()) {
			messager.printMessage(Kind.NOTE, msg);
		}
	}

	private String render(Throwable e) {
		StringWriter w = new StringWriter();
		PrintWriter pw = new PrintWriter(w);
		e.printStackTrace(pw);
		pw.flush();
		return w.toString();
	}

	@AllArgsConstructor
	static class HandlerInfo {
		TypeElement handlerType;
		TypeElement cqsInterfaceType;
		TypeElement paramType;
		TypeElement returnType;
	}

	private void createSource(HandlerInfo info) {
		TypeElement type = info.handlerType;
		TypeElement qt = info.paramType;
		TypeElement rt = info.returnType;
		String templateFileName = cqsInterfaces.get(info.cqsInterfaceType.getQualifiedName().toString());

		if (ReflectionHelper.isAbstract(type)) {
			return;
		}

		warnIfAnnotedWithComponent(type);
		errorIfManyConstructors(type);

		if (alreadyGenerated.add(type)) {
			StringBuilder extraConstrParams = new StringBuilder();
			StringBuilder superCallParams = new StringBuilder();

			extractConstructorParameters(type, extraConstrParams, superCallParams);

			val className = type.getQualifiedName().toString();

			try {
				String packageName = null;
				int lastDot = className.lastIndexOf('.');
				if (lastDot > 0) {
					packageName = className.substring(0, lastDot);
				}

				String simpleClassName = className.substring(lastDot + 1);
				String targetClassName = className + GENERATED_TYPE_SUFFIX;
				String targetSimpleClassName = targetClassName.substring(lastDot + 1);

				JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(targetClassName);

				try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {

					String m = getResourceFileAsString(templateFileName);
					// maybe replace by a decent template engine?
					String meth = String.format(m, packageName, targetSimpleClassName, simpleClassName,
							qt != null ? qt.getQualifiedName() : null, rt != null ? rt.getQualifiedName() : null,
							this.getClass().getCanonicalName(), ZonedDateTime.now(), extraConstrParams.toString(),
							superCallParams.toString());
					out.println(meth);
				}
				log("generated " + targetClassName);
			} catch (IOException e) {
				error("While creating source file: ", e);
			}
		}
	}

	private void extractConstructorParameters(TypeElement type, StringBuilder extraConstrParams,
			StringBuilder superCallParams) {
		Optional<? extends Element> constr = type.getEnclosedElements().stream()
				.filter(e -> e.getKind() == ElementKind.CONSTRUCTOR).findFirst();
		constr.ifPresent(c -> {
			List<? extends VariableElement> parameters = ((ExecutableElement) c).getParameters();
			parameters.forEach(p -> {
				String pname = "_" + p.getSimpleName().toString();
				String ptype = ReflectionHelper.getTypeElement(p).getQualifiedName().toString();

				extraConstrParams.append(", ").append(ptype).append(" ").append(pname);
				if (superCallParams.length() > 0) {
					superCallParams.append(", ");
				}
				superCallParams.append(pname);
			});
		});
	}

	private void errorIfManyConstructors(TypeElement type) {
		if (type.getEnclosedElements().stream().filter(e -> e.getKind() == ElementKind.CONSTRUCTOR).count() > 1) {
			error("Handlers should have exactly one constructor", type);
		}
	}

	private void warnIfAnnotedWithComponent(TypeElement type) {
		if (type.getAnnotationMirrors().stream().map(AnnotationMirror::getAnnotationType).map(DeclaredType::asElement)
				.map(ReflectionHelper::getTypeElement)
				.anyMatch(e -> Component.class.getCanonicalName().equals(e.getQualifiedName().toString())
						|| Service.class.getCanonicalName().equals(e.getQualifiedName().toString()))) {
			warn("Handlers should not be annotated with @Component", type);
		}
	}

	private boolean verbose() {
		return Boolean.parseBoolean(processingEnv.getOptions().get(VERBOSE));
	}

	private final Map<String, String> templates = new ConcurrentHashMap<String, String>();

	private String getResourceFileAsString(String fileName) throws IOException {
		return templates.computeIfAbsent(fileName, fn -> {
			try (InputStream is = CqsProcessor.class.getResourceAsStream(fn)) {
				if (is == null) {
					return null;
				}
				try (InputStreamReader isr = new InputStreamReader(is);
						BufferedReader reader = new BufferedReader(isr)) {
					return reader.lines().collect(Collectors.joining(System.lineSeparator()));
				}
			} catch (Throwable e) {
				error("While locating templates", e);
				return null;
			}
		});
	}

}
