/*
 * Copyright © 2024 PRISMA European Capacity Platform GmbH 
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
package eu.prismacapacity.spring.cqs.cmd;

import eu.prismacapacity.spring.cqs.cmd.logging.*;
import jakarta.annotation.Nullable;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
@SuppressWarnings({"java:S3011", "rawtypes"})
public class LogRenderer {

  private static final String OPEN = "(";
  private static final String CLOSE = ")";
  private static final String DELIMITER = ", ";

  private static final CircularDependencyBreaker cdb = new CircularDependencyBreaker();

  public String renderDefault(@NonNull Object object) {
    boolean initialized = cdb.initialize();
    try {
      cdb.get().put(object, object);
      StringBuilder sb = new StringBuilder(getType(object)).append(OPEN);
      sb.append(
          getAllFields(object.getClass()).stream()
              .map(f -> renderField(f, object))
              .filter(Objects::nonNull)
              .collect(Collectors.joining(DELIMITER)));
      return sb.append(CLOSE).toString();
    } finally {
      if (initialized) cdb.remove();
    }
  }

  private String renderValue(@Nullable Object object) {
    // no nulls
    if (object == null) return null;

    // no recursion
    if (cdb.get().put(object, object) != null) return null;

    if (object instanceof LogRenderable) return ((LogRenderable) object).toLogString();
    else {

      if (Collection.class.isAssignableFrom(object.getClass())) {
        return renderCollection((Collection) object);
      }

      return object.toString();
    }
  }

  @SuppressWarnings("unchecked")
  private static String renderCollection(Collection object) {
    if (object.isEmpty()) return null; // dont render empty collections
    else {
      Stream stream = object.stream().map(LogRenderer::renderValue);
      return (String) stream.collect(Collectors.joining(DELIMITER, OPEN, CLOSE));
    }
  }

  private static String renderField(Field field, Object target) {
    if (field.getAnnotation(LogExclude.class) == null) {
      return renderKeyValue(field.getName(), getValue(field, target));
    } else return null;
  }

  private static Object getValue(Field field, Object target) {
    try {
      field.setAccessible(true);
      return field.get(target);
    } catch (RuntimeException | ReflectiveOperationException e) {
      // we want to log that here, but do not throw an exception
      log.warn("An Exception occured when getting the value of {}", field, e);
      return null;
    }
  }

  @Nullable
  private static String renderKeyValue(@NonNull String name, @Nullable Object o) {
    if (o == null) return null; // don't log nulls, we're opinionated here.

    String value = renderValue(o);
    if (value == null) return null;
    else {
      return name + "=" + value;
    }
  }

  public static List<Field> getAllFields(@NonNull final Class<?> cls) {
    final List<Field> allFields = new ArrayList<>();
    Class<?> currentClass = cls;
    while (currentClass != null) {
      final Field[] declaredFields = currentClass.getDeclaredFields();
      Collections.addAll(allFields, declaredFields);
      currentClass = currentClass.getSuperclass();
    }
    allFields.removeIf(f -> Modifier.isStatic(f.getModifiers()));
    return allFields;
  }

  private static String getType(@NonNull Object object) {
    // may be a good hook to unwrap ...
    return object.getClass().getSimpleName();
  }

  /** this complexity is sadly needed to prevent infinite recursion */
  static class CircularDependencyBreaker extends ThreadLocal<IdentityHashMap<Object, Object>> {
    public boolean initialize() {
      if (get() == null) {
        set(new IdentityHashMap<>());
        return true;
      } else return false;
    }
  }
}
