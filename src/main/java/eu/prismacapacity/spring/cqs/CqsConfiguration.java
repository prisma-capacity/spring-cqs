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

import javax.validation.Validator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import eu.prismacapacity.spring.cqs.cmd.CommandHandlerOrchestrationAspect;
import eu.prismacapacity.spring.cqs.metrics.CommandMetrics;
import eu.prismacapacity.spring.cqs.metrics.CqsMetrics;
import eu.prismacapacity.spring.cqs.metrics.QueryMetrics;
import eu.prismacapacity.spring.cqs.query.QueryHandlerOrchestrationAspect;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class CqsConfiguration {
	@Bean
	public CommandHandlerOrchestrationAspect commandHandlerOrchestrationAspect(Validator v, CommandMetrics metrics) {
		return new CommandHandlerOrchestrationAspect(v, metrics);
	}

	@Bean
	public QueryHandlerOrchestrationAspect queryHandlerOrchestrationAspect(Validator v, QueryMetrics metrics) {
		return new QueryHandlerOrchestrationAspect(v, metrics);
	}

	@Bean
	@ConditionalOnMissingBean
	public CqsMetrics metrics(
			MeterRegistry meterRegistry,
			@Value("${cqs.command.timer-name:commandHandler.timed}") String commandHandlerTimerName,
			@Value("${cqs.query.timer-name:queryHandler.timed}") String queryHandlerTimerName,
			@Value("${cqs.query.timeout-name:queryHandler.timeOutDuringExecution}") String timeoutDuringQueryCounterName
	) {
		return new CqsMetrics(meterRegistry, queryHandlerTimerName, timeoutDuringQueryCounterName,
				commandHandlerTimerName);

	}

}
