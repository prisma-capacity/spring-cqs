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

import lombok.NonNull;

/**
 * happened during execution of the verify method of a ({@link QueryHandler}
 */
public class QueryVerificationException extends QueryHandlingException {

	public QueryVerificationException(@NonNull Throwable e) {
		super(e);
	}

	public QueryVerificationException(@NonNull String msg, @NonNull Throwable e) {
		super(msg, e);
	}

	public QueryVerificationException(@NonNull String msg) {
		super(msg);
	}
}
