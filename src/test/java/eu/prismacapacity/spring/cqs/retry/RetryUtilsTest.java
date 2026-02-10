/*
 * Copyright © 2022-2026 PRISMA European Capacity Platform GmbH 
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
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RetryUtilsTest {
  @Mock private Function<Integer, String> fn;
  @Mock private Function<Integer, String> fn2;

  @Test
  void test_happyCase() {
    when(fn.apply(any())).thenReturn("foo");

    assertEquals("foo", RetryUtils.withOptionalRetry(RetryWithDefaults.class, fn));

    verify(fn).apply(0);
  }

  @Test
  void test_noRetry() {
    when(fn.apply(any())).thenThrow(new IllegalStateException());

    assertThrows(
        IllegalStateException.class, () -> RetryUtils.withOptionalRetry(NoRetries.class, fn));

    verify(fn).apply(0);
  }

  @Test
  void test_defaults() {
    when(fn.apply(any())).thenThrow(new IllegalStateException());

    assertThrows(
        IllegalStateException.class,
        () -> RetryUtils.withOptionalRetry(RetryWithDefaults.class, fn));

    verify(fn).apply(0);
    verify(fn).apply(1);
    verify(fn).apply(2);
  }

  @Test
  void test_defaults_noRetry() {
    when(fn.apply(any())).thenThrow(new CommandValidationException("broken", new Throwable()));
    when(fn2.apply(any())).thenThrow(new QueryValidationException("broken", new Throwable()));

    assertThrows(
        CommandHandlingException.class,
        () -> RetryUtils.withOptionalRetry(RetryWithDefaults.class, fn));
    assertThrows(
        QueryHandlingException.class,
        () -> RetryUtils.withOptionalRetry(RetryWithDefaults.class, fn2));

    verify(fn).apply(0);
    verify(fn2).apply(0);
  }

  @Test
  void test_customConfig() {
    when(fn.apply(any())).thenThrow(new IllegalStateException());
    when(fn2.apply(any())).thenThrow(new IllegalArgumentException());

    assertThrows(
        IllegalStateException.class,
        () -> RetryUtils.withOptionalRetry(RetryWithCustomConfig.class, fn));
    assertThrows(
        IllegalArgumentException.class,
        () -> RetryUtils.withOptionalRetry(RetryWithCustomConfig.class, fn2));

    verify(fn).apply(0);
    verify(fn2, times(3)).apply(any());
  }

  @Test
  void test_backoff() {
    when(fn.apply(any())).thenThrow(new IllegalStateException());

    assertThrows(
        IllegalStateException.class,
        () -> RetryUtils.withOptionalRetry(RetryWithBackoff.class, fn));

    verify(fn, times(6)).apply(any());
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
