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
package eu.prismacapacity.spring.cqs.metrics;

import java.util.function.Supplier;

import lombok.NonNull;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

public class CqsMetrics implements CommandMetrics, QueryMetrics {
	private final MeterRegistry meterRegistry;
	private final String queryHandlerTimerMetricName;
	private final String queryHandlerTimeoutMetricName;
	private final String commandHandlerTimerMetricName;

	public CqsMetrics(@NonNull MeterRegistry meterRegistry, @NonNull String queryHandlerTimerMetricName,
			@NonNull String queryHandlerTimeoutMetricName, @NonNull String commandHandlerTimerMetricName) {
		this.meterRegistry = meterRegistry;
		this.queryHandlerTimerMetricName = queryHandlerTimerMetricName;
		this.queryHandlerTimeoutMetricName = queryHandlerTimeoutMetricName;
		this.commandHandlerTimerMetricName = commandHandlerTimerMetricName;

		meterRegistry.timer(queryHandlerTimerMetricName);
		meterRegistry.counter(queryHandlerTimeoutMetricName);
		meterRegistry.timer(commandHandlerTimerMetricName);
	}

	@Override
	public <T> T timedCommand(@NonNull String commandHandlerClass, @NonNull Supplier<T> fn) {
		return meterRegistry.timer(commandHandlerTimerMetricName, Tags.of(Tags.of("class", commandHandlerClass)))
				.record(fn);
	}

	@Override
	public <T> T timedQuery(String queryHandlerClass, Supplier<T> fn) {
		return meterRegistry.timer(queryHandlerTimerMetricName, Tags.of(Tags.of("class", queryHandlerClass)))
				.record(fn);
	}

	@Override
	public void logTimeout() {
		meterRegistry.counter(queryHandlerTimeoutMetricName).increment();
	}
}
