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
import java.util.concurrent.TimeoutException;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import eu.prismacapacity.spring.cqs.cmd.CommandHandlingException;
import eu.prismacapacity.spring.cqs.cmd.CommandVerificationException;
import eu.prismacapacity.spring.cqs.query.*;

@Aspect
@SuppressWarnings("unchecked")
public class QueryHandlerOrchestrationAspect {
	protected final Validator validator;

	QueryHandlerOrchestrationAspect() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Around("execution(* eu.prismacapacity.spring.cqs.query.QueryHandler.handle(..))")
	public Object orchestrate(ProceedingJoinPoint joinPoint) throws Throwable {
		return process(joinPoint);
	}

	private <Q extends Query> Object process(ProceedingJoinPoint joinPoint) throws CommandHandlingException {

		Q cmd = (Q) joinPoint.getArgs()[0];
		QueryHandler<Q, ?> target = (QueryHandler<Q, ?>) joinPoint.getTarget();

		// validator based validate
		Set<ConstraintViolation<Q>> violations = validator.validate(cmd);
		if (!violations.isEmpty()) {
			throw new QueryValidationException(violations);
		}

		// custom validate
		try {
			target.validate(cmd);
		} catch (Exception e) {
			if (e instanceof QueryValidationException) {
				throw (QueryValidationException) e;
			} else {
				throw new QueryValidationException(e);
			}
		}

		// verification
		try {
			target.verify(cmd);
		} catch (Exception e) {
			if (e instanceof QueryVerificationException) {
				throw (QueryVerificationException) e;
			} else {
				throw new CommandVerificationException(e);
			}
		}

		// execution
		try {
			QueryResponse<?> result = target.handle(cmd);
			if (result == null) {
				throw new QueryHandlingException("Response must not be null");
			}
			return result;

		} catch (Exception e) {

			// might be using sneakythrows... so this is intentional
			if (e instanceof TimeoutException) {
				throw new QueryTimeoutException((TimeoutException) e);
			}

			if (e instanceof QueryHandlingException) {
				throw (QueryHandlingException) e;
			}

			throw new QueryHandlingException(e);

		}
	}
}
