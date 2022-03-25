/*
 * Copyright © 2022 PRISMA European Capacity Platform GmbH 
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
package eu.prismacapacity.spring.cqs.retry;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.retry.support.RetryTemplateBuilder;

@UtilityClass
public class RetryableUtils {
  private static final Map<Class<?>, Optional<RetryTemplate>> CACHE = new ConcurrentHashMap<>();

  private Optional<RetryTemplate> from(Class<?> clazz) {
    return CACHE.computeIfAbsent(clazz, RetryableUtils::getRetryTemplate);
  }

  @SneakyThrows
  public <R> R withOptionalRetry(Class<?> handler, Supplier<R> fn) {
    final Optional<RetryTemplate> template = from(handler);

    if (template.isPresent()) {
      return template.get().execute((RetryCallback<R, Throwable>) retryContext -> fn.get());
    } else {
      return fn.get();
    }
  }

  Optional<RetryTemplate> getRetryTemplate(Class<?> clazz) {
    return Optional.ofNullable(clazz.getAnnotation(Retryable.class))
        .map(
            config -> {
              final RetryTemplateBuilder tplBuilder = new RetryTemplateBuilder();

              tplBuilder.maxAttempts(config.maxAttempts());

              final long interval = config.interval();
              final long maxInterval = config.exponentialBackoffMaxInterval();

              if (maxInterval != 0) {
                tplBuilder.exponentialBackoff(interval, 1.2, maxInterval);
              } else {
                tplBuilder.fixedBackoff(interval);
              }

              final Class<? extends Throwable>[] notRetryOn = config.notRetryOn();

              if (notRetryOn != null && notRetryOn.length > 0) {
                Arrays.stream(notRetryOn).forEach(tplBuilder::notRetryOn);
                tplBuilder.traversingCauses();
              }

              return tplBuilder.build();
            });
  }
}
