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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

public class ReflectionHelper {
	public static TypeElement getTypeElement(Element e) {
		return getTypeElement(e.asType());
	}

	public static TypeElement getTypeElement(TypeMirror typeMirror) {
		if (typeMirror instanceof DeclaredType) {
			DeclaredType dt = (DeclaredType) typeMirror;
			Element te = dt.asElement();
			if (te instanceof TypeElement) {
				return (TypeElement) te;
			}
		}
		return null;
	}

	public static ExecutableElement asMethodElement(Element v) {
		return (ExecutableElement) v;
	}

	public static boolean isGenerated(TypeElement te) {
		return te.getAnnotationMirrors().stream()
				.anyMatch(a -> "Generated".equals(a.getAnnotationType().asElement().getSimpleName().toString()));
	}

	public static boolean isAbstract(TypeElement te) {
		return (te.getModifiers().contains(Modifier.ABSTRACT)) || (te.getKind() == ElementKind.INTERFACE);
	}

	static Name getCanonicalName(TypeMirror i) {
		return ((TypeElement) ((DeclaredType) i).asElement()).getQualifiedName();
	}

	static Set<TypeMirror> findInterfaces(TypeElement te) {
		if (te == null) {
			return Collections.emptySet();
		}

		Set<TypeMirror> cached = interfaceCache.get(te);
		if (cached != null) {
			return cached;
		}

		Set<TypeMirror> interfaces = new HashSet<>(te.getInterfaces());

		for (TypeMirror i : new ArrayList<>(interfaces)) {
			interfaces.addAll(findInterfaces(getTypeElement(i)));
		}

		TypeMirror s = te.getSuperclass();
		if (s != null) {
			TypeElement ste = getTypeElement(s);
			if (ste != null) {
				interfaces.addAll(findInterfaces(ste));
			}
		}

		// yes, it is not guranteed, that interfaceCache does not have it already.
		interfaceCache.put(te, interfaces);

		return interfaces;
	}

	private static final Map<TypeElement, Set<TypeMirror>> interfaceCache = new ConcurrentHashMap<>();

}
