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
import java.util.*;
import lombok.*;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("ALL")
@ExtendWith(MockitoExtension.class)
class LogRendererTest {

  @Data
  static class C implements LogRenderable {
    Object a;
  }

  @AllArgsConstructor
  static class B implements LogRenderable {
    C c;
  }

  @AllArgsConstructor
  static class A implements LogRenderable {
    B b;
  }

  @Nested
  class WhenRenderingDefault {
    @Test
    void rendersExample() {
      Assertions.assertThat(LogRenderer.renderDefault(new Example()))
          .isEqualTo(
              "Example(topLevel=_topLevel, inner=Inner(innerField=_innerField, r=selfRendered), list=(1, 2.1, foo, true, ShouldDefaultToToString(x=x, y=y, z=null)))");
    }

    @Test
    void clearsThreadLocal() {
      LogRenderer.renderDefault(new Example());
      Assertions.assertThat(new LogRenderer.AlreadyVisitedObjectsHolder().get()).isNull();
    }

    @Test
    void breaksCircularDependency() {
      C c = new C();
      B b = new B(c);
      A a = new A(b);
      c.a = a;
      // c should be empty, because a needs to be skipped
      Assertions.assertThat(a.toLogString()).isEqualTo("A(b=B(c=C()))");
    }
  }

  @Nested
  class WhenGettingAllFields {

    @Test
    void findsAll() {
      Assertions.assertThat(LogRenderer.getAllFields(GrandChild.class)).hasSize(3);
    }
  }

  static class Base {
    private static final String MUST_BE_IGNORED = "someValue";
    private final String f3 = "someValue";
  }

  static class Child extends Base {
    private String f2;
  }

  static class GrandChild extends Child implements LogRenderable {
    String f1;
  }

  static class Example implements LogRenderable {
    String topLevel = "_topLevel";
    String nullField = null;
    Inner inner = new Inner();
    List<Object> list = Lists.newArrayList(1, 2.1, "foo", true, new ShouldDefaultToToString());

    static class Inner implements LogRenderable {
      @LogExclude String mustBeSkipped = "whatTheHell";
      List<?> emptyCollection = new ArrayList<>();
      String nullField = null;
      String innerField = "_innerField";
      Renderable r = new Renderable();
    }
  }
}

@ToString
class ShouldDefaultToToString {
  String x = "x";
  String y = "y";
  String z = null;
}

class Renderable implements LogRenderable {
  @Override
  public String toLogString() {
    return "selfRendered";
  }
}
