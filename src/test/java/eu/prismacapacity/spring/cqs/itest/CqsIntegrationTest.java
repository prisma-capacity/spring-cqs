/*
 * Copyright © 2023-2024 PRISMA European Capacity Platform GmbH 
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import eu.prismacapacity.spring.cqs.cmd.*;
import eu.prismacapacity.spring.cqs.cmd.Logging;
import eu.prismacapacity.spring.cqs.query.*;
import java.util.*;
import nl.altindag.log.LogCaptor;
import nl.altindag.log.model.LogEvent;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;
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
      assertThatThrownBy(() -> myTestQueryHandler.handle(query))
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
      assertThatThrownBy(() -> myTestCommandHandler.handle(query))
          .isInstanceOf(CommandValidationException.class);
    }

    @Test
    void logSuccess() {
      LogCaptor logCaptor = LogCaptor.forClass(MyTestCommandHandler.class);
      myTestCommandHandler.handle(new MyTestCommand(27));
      org.assertj.core.api.Assertions.assertThat(
              logCaptor.getInfoLogs().stream().findFirst().orElse("nothing"))
          .contains("MyTestCommand")
          .containsIgnoringCase("success");

      List<LogEvent> logEvents = logCaptor.getLogEvents();
      LogEvent success =
          logEvents.stream()
              .filter(e -> e.getLoggerName().contains("MyTestCommandHandler"))
              .findFirst()
              .orElseThrow(() -> new AssertionFailedError("success message not found"));
      String renderedCommand =
          success.getKeyValuePairs().stream()
              .filter(kv -> kv.getKey().equals(Logging.LOG_ATTRIBUTE_COMMAND))
              .findFirst()
              .map(kv -> kv.getValue().toString())
              .orElseThrow(
                  () ->
                      new AssertionFailedError(
                          "success message does not contain rendered command"));
      Assertions.assertThat(renderedCommand).contains("age=27");
    }

    @Test
    void logValidationFailure() {
      LogCaptor logCaptor = LogCaptor.forClass(MyTestCommandHandler.class);

      MyTestCommand cmd = new MyTestCommand(-3);
      assertThatThrownBy(() -> myTestCommandHandler.handle(cmd))
          .isInstanceOf(CommandValidationException.class);

      org.assertj.core.api.Assertions.assertThat(
              logCaptor.getWarnLogs().stream().findFirst().orElse("nothing"))
          .contains("MyTestCommand")
          .containsIgnoringCase("fail");

      List<LogEvent> logEvents = logCaptor.getLogEvents();
      LogEvent failure =
          logEvents.stream()
              .filter(e -> e.getLoggerName().contains("MyTestCommandHandler"))
              .findFirst()
              .orElseThrow(() -> new AssertionFailedError("failure message not found"));
      String renderedCommand =
          failure.getKeyValuePairs().stream()
              .filter(kv -> kv.getKey().equals(Logging.LOG_ATTRIBUTE_COMMAND))
              .findFirst()
              .map(kv -> kv.getValue().toString())
              .orElseThrow(
                  () ->
                      new AssertionFailedError(
                          "failure message does not contain rendered command"));
      Assertions.assertThat(failure.getThrowable())
          .hasValueSatisfying(
              t -> Assertions.assertThat(t).isInstanceOf(CommandValidationException.class));
      Assertions.assertThat(renderedCommand).contains("age=-3");
    }

    @Test
    void logVerificationFailureThrowingRuntime() {
      LogCaptor logCaptor = LogCaptor.forClass(MyTestCommandHandler.class);

      MyTestCommand query = new MyTestCommand(118);
      assertThatThrownBy(() -> myTestCommandHandler.handle(query))
          .isInstanceOf(CommandVerificationException.class);

      org.assertj.core.api.Assertions.assertThat(
              logCaptor.getWarnLogs().stream().findFirst().orElse("nothing"))
          .contains("MyTestCommand")
          .containsIgnoringCase("fail");

      List<LogEvent> logEvents = logCaptor.getLogEvents();
      LogEvent failure =
          logEvents.stream()
              .filter(e -> e.getLoggerName().contains("MyTestCommandHandler"))
              .findFirst()
              .orElseThrow(() -> new AssertionFailedError("failure message not found"));
      String renderedCommand =
          failure.getKeyValuePairs().stream()
              .filter(kv -> kv.getKey().equals(Logging.LOG_ATTRIBUTE_COMMAND))
              .findFirst()
              .map(kv -> kv.getValue().toString())
              .orElseThrow(
                  () ->
                      new AssertionFailedError(
                          "failure message does not contain rendered command"));
      Assertions.assertThat(failure.getThrowable())
          .hasValueSatisfying(
              t -> Assertions.assertThat(t).isInstanceOf(CommandVerificationException.class));
      Assertions.assertThat(renderedCommand).contains("age=118");
    }

    @Test
    void logExecutionFailure() {
      LogCaptor logCaptor = LogCaptor.forClass(MyTestCommandHandler.class);

      MyTestCommand query = new MyTestCommand(119);
      assertThatThrownBy(() -> myTestCommandHandler.handle(query))
          .isInstanceOf(CommandHandlingException.class);

      org.assertj.core.api.Assertions.assertThat(
              logCaptor.getWarnLogs().stream().findFirst().orElse("nothing"))
          .contains("MyTestCommand")
          .containsIgnoringCase("fail");

      List<LogEvent> logEvents = logCaptor.getLogEvents();
      LogEvent failure =
          logEvents.stream()
              .filter(e -> e.getLoggerName().contains("MyTestCommandHandler"))
              .findFirst()
              .orElseThrow(() -> new AssertionFailedError("failure message not found"));
      String renderedCommand =
          failure.getKeyValuePairs().stream()
              .filter(kv -> kv.getKey().equals(Logging.LOG_ATTRIBUTE_COMMAND))
              .findFirst()
              .map(kv -> kv.getValue().toString())
              .orElseThrow(
                  () ->
                      new AssertionFailedError(
                          "failure message does not contain rendered command"));
      Assertions.assertThat(failure.getThrowable())
          .hasValueSatisfying(
              t -> Assertions.assertThat(t).isInstanceOf(CommandHandlingException.class));
      Assertions.assertThat(renderedCommand).contains("age=119");
    }
  }
}
