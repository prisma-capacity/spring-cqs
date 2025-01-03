/*
 * Copyright © 2020-2024 PRISMA European Capacity Platform GmbH 
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

import eu.prismacapacity.spring.cqs.metrics.CommandMetrics;
import eu.prismacapacity.spring.cqs.retry.RetryUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * Orchestrates the validation/verification/execution handling of a (Responding)CommandHandler and
 * also maps exceptions if necessary. Using an aspect in this way is kind of a stretch. However, we
 * had aspects, then an abstract class, then aspects again. This is the current incarnation :D
 */
@Aspect
@SuppressWarnings({"unchecked", "java:S1141"})
@RequiredArgsConstructor
@Slf4j
public final class CommandHandlerOrchestrationAspect {

  private static final String PC_CommandHandler =
      "execution(* eu.prismacapacity.spring.cqs.cmd.CommandHandler.handle(..))";
  private static final String PC_RespondingCommandHandler =
      "execution(* eu.prismacapacity.spring.cqs.cmd.RespondingCommandHandler.handle(..))";
  private static final String PC_TokenCommandHandler =
      "execution(* eu.prismacapacity.spring.cqs.cmd.TokenCommandHandler.handle(..))";

  final Validator validator;

  final CommandMetrics metrics;

  @Around(
      PC_CommandHandler + " || " + PC_RespondingCommandHandler + " || " + PC_TokenCommandHandler)
  public Object orchestrate(ProceedingJoinPoint joinPoint) throws Throwable {
    val signature = joinPoint.getStaticPart().getSignature();
    val clazz = signature.getDeclaringTypeName();

    return RetryUtils.withOptionalRetry(
        joinPoint.getTarget().getClass(),
        (count) -> metrics.timedCommand(clazz, count, () -> process(joinPoint)));
  }

  @VisibleForTesting
  <C extends Command> Object process(ProceedingJoinPoint joinPoint)
      throws CommandHandlingException {

    C cmd = (C) joinPoint.getArgs()[0];
    String commandType = LogRenderer.getType(cmd);
    ICommandHandler<C> target = (ICommandHandler<C>) joinPoint.getTarget();
    String renderedCommand;
    try {
      // happens before executing, so that possible modifications are not reflected
      renderedCommand = cmd.toLogString();
    } catch (Throwable e) {
      log.warn(
          "A command of {} failed to render for logging. This is a bug, please report to https://github.com/prisma-capacity/spring-cqs/issues . Command execution is not impaired, though.",
          cmd.getClass(),
          e);
      renderedCommand = cmd.getClass().getName() + "( failed to render )";
    }
    // validator based validate
    Set<ConstraintViolation<C>> violations = validator.validate(cmd);
    if (!violations.isEmpty()) {
      Logging.logAndThrow(
          target, commandType, renderedCommand, new CommandValidationException(violations));
    }

    // custom validate
    try {
      target.validate(cmd);
    } catch (Exception e) {
      Logging.logAndThrow(target, commandType, renderedCommand, CommandValidationException.wrap(e));
    }

    // verification
    try {
      target.verify(cmd);
    } catch (Exception e) {
      Logging.logAndThrow(
          target, commandType, renderedCommand, CommandVerificationException.wrap(e));
    }

    // execution
    try {
      val result = joinPoint.proceed();
      if (result == null) {
        if (joinPoint.getTarget() instanceof CommandHandler) {
          // ok for a void return
        } else {
          Logging.logAndThrow(
              target,
              commandType,
              renderedCommand,
              new CommandHandlingException("Response must not be null"));
        }
      }
      Logging.logSuccess(target, commandType, renderedCommand, result);
      return result;
    } catch (Throwable e) {
      Logging.logAndThrow(target, commandType, renderedCommand, CommandHandlingException.wrap(e));
    }
    return null; // dead code
  }
}
