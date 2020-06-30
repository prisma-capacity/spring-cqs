package eu.prismacapacity.spring.cqs.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

        val result = uut.timedCommand("clazz", () -> true);

        assertTrue(result);

        verify(registry).timer("baz", Tags.of(Tags.of("class", "clazz")));
        verify(timer).record(ArgumentMatchers.<Supplier<Boolean>>any());
    }

    @Test
    public void timedQuery() {
        val timer = mock(Timer.class);
        when(registry.timer(any(), any(Tags.class))).thenReturn(timer);
        when(timer.record(ArgumentMatchers.<Supplier<Boolean>>any())).thenReturn(true);

        val uut = new CqsMetrics(registry, "foo", "bar", "baz");

        val result = uut.timedQuery("clazz", () -> true);

        assertTrue(result);

        verify(registry).timer("foo", Tags.of(Tags.of("class", "clazz")));
        verify(timer).record(ArgumentMatchers.<Supplier<Boolean>>any());
    }
}