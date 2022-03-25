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

import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RetryableUtilsTest {
  @Mock Supplier<String> fn;
  @Mock Supplier<String> fn2;

  @Test
  void test_happyCase() {
    when(fn.get()).thenReturn("foo");

    assertEquals("foo", RetryableUtils.withOptionalRetry(RetryWithDefaults.class, fn));

    verify(fn).get();
  }

  @Test
  void test_noRetry() {
    when(fn.get()).thenThrow(new IllegalStateException());

    assertThrows(
        IllegalStateException.class, () -> RetryableUtils.withOptionalRetry(NoRetries.class, fn));

    verify(fn).get();
  }

  @Test
  void test() {
    when(fn.get()).thenThrow(new IllegalStateException());

    assertThrows(
        IllegalStateException.class,
        () -> RetryableUtils.withOptionalRetry(RetryWithDefaults.class, fn));

    verify(fn, times(3)).get();
  }

  @Test
  void testCustomConfig() {
    when(fn.get()).thenThrow(new IllegalStateException());
    when(fn2.get()).thenThrow(new IllegalArgumentException());

    assertThrows(
        IllegalStateException.class,
        () -> RetryableUtils.withOptionalRetry(RetryWithCustomConfig.class, fn));
    assertThrows(
        IllegalArgumentException.class,
        () -> RetryableUtils.withOptionalRetry(RetryWithCustomConfig.class, fn2));

    verify(fn).get();
    verify(fn2, times(2)).get();
  }

  static class NoRetries {}

  @Retryable
  static class RetryWithDefaults {}

  @Retryable(
      maxAttempts = 2,
      notRetryOn = {IllegalStateException.class})
  static class RetryWithCustomConfig {}
}
