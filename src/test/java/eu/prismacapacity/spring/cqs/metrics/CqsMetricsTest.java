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
package eu.prismacapacity.spring.cqs.metrics;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.micrometer.core.instrument.*;
import java.util.function.Supplier;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CqsMetricsTest {
  @Mock(lenient = true)
  MeterRegistry registry;

  @Test
  public void registerCounterAndTimers() {
    new CqsMetrics(registry, "foo", "bar", "baz");

    verify(registry).counter("bar");
    verify(registry).timer("foo");
    verify(registry).timer("baz");
  }

  @Test
  public void logTimeout() {
    val counter = mock(Counter.class);
    when(registry.counter("bar")).thenReturn(counter);

    val uut = new CqsMetrics(registry, "foo", "bar", "baz");

    uut.logTimeout();

    verify(registry, times(2)).counter("bar");
    verify(counter).increment();
  }

  @Test
  public void timedCommand() {
    val timer = mock(Timer.class);
    when(registry.timer(any(), any(Tags.class))).thenReturn(timer);
    when(timer.record(ArgumentMatchers.<Supplier<Boolean>>any())).thenReturn(true);

    val uut = new CqsMetrics(registry, "foo", "bar", "baz");

    val result = uut.timedCommand("clazz", 1, () -> true);

    assertTrue(result);

    verify(registry).timer("baz", Tags.of(Tag.of("class", "clazz"), Tag.of("retryCount", "1")));
    verify(timer).record(ArgumentMatchers.<Supplier<Boolean>>any());
  }

  @Test
  public void timedQuery() {
    val timer = mock(Timer.class);
    when(registry.timer(any(), any(Tags.class))).thenReturn(timer);
    when(timer.record(ArgumentMatchers.<Supplier<Boolean>>any())).thenReturn(true);

    val uut = new CqsMetrics(registry, "foo", "bar", "baz");

    val result = uut.timedQuery("clazz", 2, () -> true);

    assertTrue(result);

    verify(registry).timer("foo", Tags.of(Tag.of("class", "clazz"), Tag.of("retryCount", "2")));
    verify(timer).record(ArgumentMatchers.<Supplier<Boolean>>any());
  }
}
