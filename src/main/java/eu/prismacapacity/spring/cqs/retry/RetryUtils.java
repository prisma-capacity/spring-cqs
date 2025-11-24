/*
 * Copyright © 2022-2025 PRISMA European Capacity Platform GmbH 
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

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;

@UtilityClass
public class RetryUtils {
  private static final Map<Class<?>, Optional<RetryTemplate>> CACHE = new ConcurrentHashMap<>();

  private Optional<RetryTemplate> from(Class<?> clazz) {
    return CACHE.computeIfAbsent(clazz, RetryUtils::getRetryTemplate);
  }

  @SneakyThrows
  public <R> R withOptionalRetry(Class<?> handler, Function<Integer, R> fn) {
    final Optional<RetryTemplate> template = from(handler);

    if (template.isPresent()) {
      final AtomicInteger counter = new AtomicInteger(0);
      try {
        return template
            .get()
            .execute(
                () -> {
                  try {
                    return fn.apply(counter.get());
                  } finally {
                    counter.incrementAndGet();
                  }
                });
      } catch (RetryException e) {
        throw e.getCause();
      }
    } else {
      return fn.apply(0);
    }
  }

  Optional<RetryTemplate> getRetryTemplate(Class<?> clazz) {
    return Optional.ofNullable(clazz.getAnnotation(RetryConfiguration.class))
        .map(
            config -> {
              final RetryPolicy.Builder tplBuilder = RetryPolicy.builder();

              tplBuilder.maxRetries(config.maxAttempts());

              final long interval = config.interval();
              final long maxInterval = config.exponentialBackoffMaxInterval();

              tplBuilder.delay(Duration.ofMillis(interval));
              if (maxInterval != 0) {
                tplBuilder.multiplier(1.2);
                tplBuilder.maxDelay(Duration.ofMillis(maxInterval));
              }

              final Class<? extends Throwable>[] notRetryOn = config.notRetryOn();
              if (notRetryOn != null && notRetryOn.length > 0) {
                tplBuilder.excludes(notRetryOn);
              }

              return new RetryTemplate(tplBuilder.build());
            });
  }
}
