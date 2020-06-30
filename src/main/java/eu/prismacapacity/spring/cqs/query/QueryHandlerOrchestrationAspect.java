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
package eu.prismacapacity.spring.cqs.query;

import java.util.Set;
import java.util.concurrent.TimeoutException;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import lombok.RequiredArgsConstructor;
import lombok.val;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import eu.prismacapacity.spring.cqs.cmd.CommandHandlingException;
import eu.prismacapacity.spring.cqs.cmd.CommandVerificationException;
import eu.prismacapacity.spring.cqs.metrics.QueryMetrics;

/**
 * Orchestrates the validation/verification/execution handling of a QueryHandler
 * and also maps exceptions if necessary. Using an aspect in this way is kind of
 * a stretch. However, we had aspects, then an abstract class, then aspects
 * again. This is the current incarnation :D
 */
@Aspect
@SuppressWarnings("unchecked")
@RequiredArgsConstructor
public final class QueryHandlerOrchestrationAspect {
	protected final Validator validator;

	protected final QueryMetrics metrics;

	@Around("execution(* eu.prismacapacity.spring.cqs.query.QueryHandler.handle(..))")
	public Object orchestrate(ProceedingJoinPoint joinPoint) throws Throwable {
		val signature = joinPoint.getStaticPart().getSignature();
		val clazz = signature.getDeclaringTypeName();

		return metrics.timedQuery(clazz, () -> process(joinPoint));
	}

	protected <Q extends Query> Object process(ProceedingJoinPoint joinPoint) throws CommandHandlingException {

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
			val result = joinPoint.proceed();
			if (result == null) {
				throw new QueryHandlingException("Returned object must not be null");
			}
			return result;

		} catch (Throwable e) {
			if (e instanceof TimeoutException) {
				metrics.logTimeout();
				throw new QueryTimeoutException((TimeoutException) e);
			}

			if (e instanceof QueryHandlingException) {
				throw (QueryHandlingException) e;
			}

			throw new QueryHandlingException(e);
		}
	}
}
