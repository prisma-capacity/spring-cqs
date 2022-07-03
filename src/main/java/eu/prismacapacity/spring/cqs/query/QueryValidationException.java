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

import java.util.HashSet;
import java.util.Set;

import jakarta.validation.ConstraintViolation;

import lombok.Getter;
import lombok.NonNull;

import eu.prismacapacity.spring.cqs.Violations;

/**
 * happened while bean validation of the incoming command, or during execution
 * of the validate method of a ({@link QueryHandler}
 */
public class QueryValidationException extends QueryHandlingException {
	@Getter
	private Set<? extends ConstraintViolation<? extends Query>> violations = new HashSet<>();

	public QueryValidationException(@NonNull Set<? extends ConstraintViolation<? extends Query>> violations) {
		super(Violations.render(violations));
		this.violations = violations;
	}

	public QueryValidationException(@NonNull String msg, @NonNull Throwable e) {
		super(msg, e);
	}

	public QueryValidationException(@NonNull Throwable e) {
		super(e);
	}
}
