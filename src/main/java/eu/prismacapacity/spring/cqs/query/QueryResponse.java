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

import java.util.Optional;

import lombok.Setter;
import lombok.experimental.Accessors;

import eu.prismacapacity.spring.cqs.StateToken;

@Setter
@Accessors(chain = true)
// beware, both members are nullable intentionally
public final class QueryResponse<T> {
	private T value;
	private StateToken token;

	private QueryResponse(StateToken token, T value) {
		this.token = token;
		this.value = value;
	}

	public Optional<StateToken> getToken() {
		return Optional.ofNullable(token);
	}

	public Optional<T> getValue() {
		return Optional.ofNullable(value);
	}

	QueryResponse<T> withValue(T value) {
		return new QueryResponse<>(token, value);
	}

	public static <T> QueryResponse<T> of(StateToken token, T value) {
		return new QueryResponse<>(token, value);
	}

	public static <T> QueryResponse<T> of(T value) {
		return new QueryResponse<>(null, value);
	}

	public static <T> QueryResponse<T> of(StateToken token) {
		return new QueryResponse<>(token, null);
	}

	public static <T> QueryResponse<T> empty() {
		return new QueryResponse<>(null, null);
	}

}
