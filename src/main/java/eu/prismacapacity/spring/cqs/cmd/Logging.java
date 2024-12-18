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

import jakarta.annotation.Nullable;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.spi.DefaultLoggingEventBuilder;

@UtilityClass
@Slf4j
public class Logging {
  public static final String LOG_ATTRIBUTE_COMMAND = "cqs.command";
  public static final String LOG_ATTRIBUTE_RESULT = "cqs.result";

  static void logAndThrow(
      @NonNull ICommandHandler<?> handler,
      @NonNull String commandType,
      @NonNull String renderedCommand,
      @NonNull RuntimeException e)
      throws RuntimeException {
    DefaultLoggingEventBuilder builder =
        new DefaultLoggingEventBuilder(LoggerFactory.getLogger(handler.getClass()), Level.WARN);
    builder.setMessage("Failed to execute {} [{}].");
    builder.addArgument(commandType);
    builder.addArgument(e.getMessage());
    builder.addKeyValue(LOG_ATTRIBUTE_COMMAND, renderedCommand);
    builder.setCause(e);
    builder.log();
    throw e;
  }

  static void logSuccess(
      @NonNull ICommandHandler<?> handler,
      @NonNull String commandType,
      @NonNull String renderedCommand,
      @Nullable Object result) {
    DefaultLoggingEventBuilder builder =
        new DefaultLoggingEventBuilder(LoggerFactory.getLogger(handler.getClass()), Level.INFO);
    builder.setMessage("Successfully executed {}.");
    builder.addArgument(commandType);
    builder.addKeyValue(LOG_ATTRIBUTE_COMMAND, renderedCommand);
    if (result != null)
      try {
        builder.addKeyValue(LOG_ATTRIBUTE_RESULT, LogRenderer.renderDefault(result));
      } catch (Exception e) {
        log.warn(
            "A command-result of {} failed to render for logging. This is a bug, please report to https://github.com/prisma-capacity/spring-cqs/issues . Command execution is not impaired, though.",
            result.getClass(),
            e);
      }
    builder.log();
  }
}
