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
package eu.prismacapacity.spring.cqs.cmd;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import eu.prismacapacity.spring.cqs.metrics.CommandMetrics;
import lombok.RequiredArgsConstructor;
import lombok.val;

/**
 * Orchestrates the validation/verification/execution handling of a
 * (Responding)CommandHandler and also maps exceptions if necessary. Using an
 * aspect in this way is kind of a stretch. However, we had aspects, then an
 * abstract class, then aspects again. This is the current incarnation :D
 */
@Aspect
@SuppressWarnings("unchecked")
@RequiredArgsConstructor
public final class CommandHandlerOrchestrationAspect {

    private static final String PC_CommandHandler = "execution(* eu.prismacapacity.spring.cqs.cmd.CommandHandler.handle(..))";
    private static final String PC_RespondingCommandHandler = "execution(* eu.prismacapacity.spring.cqs.cmd.RespondingCommandHandler.handle(..))";

    protected final Validator validator;

    protected final CommandMetrics metrics;

    @Around(PC_CommandHandler + " || " + PC_RespondingCommandHandler)
    public Object orchestrate(ProceedingJoinPoint joinPoint) throws Throwable {
        val signature = joinPoint.getStaticPart().getSignature();
        val clazz = signature.getDeclaringTypeName();

        return metrics.timedCommand(clazz, () -> process(joinPoint));
    }

    @VisibleForTesting
    protected <C extends Command> Object process(ProceedingJoinPoint joinPoint) throws CommandHandlingException {

        C cmd = (C) joinPoint.getArgs()[0];
        ICommandHandler<C> target = (ICommandHandler<C>) joinPoint.getTarget();

        // validator based validate
        Set<ConstraintViolation<C>> violations = validator.validate(cmd);
        if (!violations.isEmpty()) {
            throw new CommandValidationException(violations);
        }

        // custom validate
        try {
            target.validate(cmd);
        } catch (CommandValidationException e) {
            throw e;
        } catch (Throwable e) {
            throw new CommandValidationException(e);
        }

        // verification
        try {
            target.verify(cmd);
        } catch (CommandVerificationException e) {
            throw e;
        } catch (Throwable e) {
            throw new CommandVerificationException(e);
        }

        // execution
        try {
            val result = joinPoint.proceed();
            if (result == null) {
                if (joinPoint.getTarget() instanceof CommandHandler) {
                    // ok for a void return
                }
                else {
                    throw new CommandHandlingException("Response must not be null");
                }
            }
            return result;
        } catch (CommandHandlingException e) {
            throw e;
        } catch (Throwable e) {
            throw new CommandHandlingException(e);
        }

    }
}
