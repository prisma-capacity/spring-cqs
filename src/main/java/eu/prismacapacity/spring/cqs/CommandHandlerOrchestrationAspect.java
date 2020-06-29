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
package eu.prismacapacity.spring.cqs;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import eu.prismacapacity.spring.cqs.cmd.*;

@Aspect
@SuppressWarnings("unchecked")
public final class CommandHandlerOrchestrationAspect {
	protected final Validator validator;

	CommandHandlerOrchestrationAspect() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Around("execution(* eu.prismacapacity.spring.cqs.cmd.CommandHandler.handle(..))")
	public Object orchestrate(ProceedingJoinPoint joinPoint) throws Throwable {
		return process(joinPoint);
	}

	private <C extends Command> Object process(ProceedingJoinPoint joinPoint) throws CommandHandlingException {

		C cmd = (C) joinPoint.getArgs()[0];
		CommandHandler<C, ?> target = (CommandHandler<C, ?>) joinPoint.getTarget();

		// validator based validate
		Set<ConstraintViolation<C>> violations = validator.validate(cmd);
		if (!violations.isEmpty()) {
			throw new CommandValidationException(violations);
		}

		// custom validate
		try {
			target.validate(cmd);
		} catch (Exception e) {
			if (e instanceof CommandValidationException) {
				throw (CommandValidationException) e;
			} else {
				throw new CommandValidationException(e);
			}
		}

		// verification
		try {
			target.verify(cmd);
		} catch (Exception e) {
			if (e instanceof CommandVerificationException) {
				throw (CommandVerificationException) e;
			} else {
				throw new CommandVerificationException(e);
			}
		}

		// execution
		try {
			CommandResponse result = target.handle(cmd);
			if (result == null) {
				throw new CommandHandlingException("Response must not be null");
			}
			return result;
		} catch (Exception e) {
			if (e instanceof CommandHandlingException) {
				throw (CommandHandlingException) e;
			} else {
				throw new CommandHandlingException(e);
			}

		}
	}
}
