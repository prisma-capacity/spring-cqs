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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import eu.prismacapacity.spring.cqs.cmd.CommandHandlingException;
import eu.prismacapacity.spring.cqs.cmd.CommandValidationException;
import eu.prismacapacity.spring.cqs.query.QueryHandlingException;
import eu.prismacapacity.spring.cqs.query.QueryValidationException;
import java.util.function.Supplier;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;

@ExtendWith(MockitoExtension.class)
class RetryUtilsTest {
  @Mock private Supplier<String> fn;
  @Mock private Supplier<String> fn2;

  private LogCaptor logCaptor = LogCaptor.forClass(ExponentialBackOffPolicy.class);

  @Test
  void test_happyCase() {
    when(fn.get()).thenReturn("foo");

    assertEquals("foo", RetryUtils.withOptionalRetry(RetryWithDefaults.class, fn));

    verify(fn).get();
  }

  @Test
  void test_noRetry() {
    when(fn.get()).thenThrow(new IllegalStateException());

    assertThrows(
        IllegalStateException.class, () -> RetryUtils.withOptionalRetry(NoRetries.class, fn));

    verify(fn).get();
  }

  @Test
  void test_defaults() {
    when(fn.get()).thenThrow(new IllegalStateException());

    assertThrows(
        IllegalStateException.class,
        () -> RetryUtils.withOptionalRetry(RetryWithDefaults.class, fn));

    verify(fn, times(3)).get();
  }

  @Test
  void test_defaults_noRetry() {
    when(fn.get()).thenThrow(new CommandValidationException("broken", new Throwable()));
    when(fn2.get()).thenThrow(new QueryValidationException("broken", new Throwable()));

    assertThrows(
        CommandHandlingException.class,
        () -> RetryUtils.withOptionalRetry(RetryWithDefaults.class, fn));
    assertThrows(
        QueryHandlingException.class,
        () -> RetryUtils.withOptionalRetry(RetryWithDefaults.class, fn2));

    verify(fn).get();
    verify(fn2).get();
  }

  @Test
  void test_customConfig() {
    when(fn.get()).thenThrow(new IllegalStateException());
    when(fn2.get()).thenThrow(new IllegalArgumentException());

    assertThrows(
        IllegalStateException.class,
        () -> RetryUtils.withOptionalRetry(RetryWithCustomConfig.class, fn));
    assertThrows(
        IllegalArgumentException.class,
        () -> RetryUtils.withOptionalRetry(RetryWithCustomConfig.class, fn2));

    verify(fn).get();
    verify(fn2, times(2)).get();
  }

  @Test
  void test_backoff() {
    when(fn.get()).thenThrow(new IllegalStateException());

    assertThrows(
        IllegalStateException.class,
        () -> RetryUtils.withOptionalRetry(RetryWithBackoff.class, fn));

    verify(fn, times(5)).get();

    assertEquals(4, logCaptor.getDebugLogs().size());
    assertTrue(
        logCaptor
            .getDebugLogs()
            .contains("Sleeping for 20")); // first retry with default interval of 20
    assertTrue(logCaptor.getDebugLogs().contains("Sleeping for 24")); // interval*1.2
    assertEquals(
        2L,
        logCaptor.getDebugLogs().stream()
            .filter(x -> x.equals("Sleeping for 25"))
            .count()); // maxInterval
  }

  static class NoRetries {}

  @RetryConfiguration
  static class RetryWithDefaults {}

  @RetryConfiguration(
      maxAttempts = 2,
      notRetryOn = {IllegalStateException.class})
  static class RetryWithCustomConfig {}

  @RetryConfiguration(maxAttempts = 5, exponentialBackoffMaxInterval = 25)
  static class RetryWithBackoff {}
}
