/*
 * Copyright © 2023 PRISMA European Capacity Platform GmbH 
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
package eu.prismacapacity.spring.cqs.itest;

import eu.prismacapacity.spring.cqs.cmd.CommandValidationException;
import eu.prismacapacity.spring.cqs.query.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class CqsIntegrationTest {
  @Autowired MyTestQueryHandler myTestQueryHandler;
  @Autowired MyTestCommandHandler myTestCommandHandler;

  @Nested
  class QueryHandler {
    @Test
    void shouldValidate() {
      Assertions.assertThat(myTestQueryHandler.handle(new MyTestQuery(1))).isTrue();
    }

    @Test
    void shouldVThrow() {
      final MyTestQuery query = new MyTestQuery(0);
      Assertions.assertThatThrownBy(() -> myTestQueryHandler.handle(query))
          .isInstanceOf(QueryValidationException.class);
    }
  }

  @Nested
  class CommandHandler {
    @Test
    void shouldValidate() {
      myTestCommandHandler.handle(new MyTestCommand(1));
    }

    @Test
    void shouldVThrow() {
      final MyTestCommand query = new MyTestCommand(0);
      Assertions.assertThatThrownBy(() -> myTestCommandHandler.handle(query))
          .isInstanceOf(CommandValidationException.class);
    }
  }
}
