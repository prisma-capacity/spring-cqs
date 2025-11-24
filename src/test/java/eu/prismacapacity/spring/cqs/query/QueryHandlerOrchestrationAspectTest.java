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
package eu.prismacapacity.spring.cqs.query;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import eu.prismacapacity.spring.cqs.metrics.QueryMetrics;
import eu.prismacapacity.spring.cqs.retry.RetryConfiguration;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import jakarta.validation.metadata.ConstraintDescriptor;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import lombok.NonNull;
import lombok.val;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class QueryHandlerOrchestrationAspectTest {

  @Mock private Validator validator;
  @Mock private QueryMetrics metrics;
  @InjectMocks private QueryHandlerOrchestrationAspect underTest;

  @Nested
  class WhenOrchestrating {

    class FooQuery implements Query {}

    ConstraintViolation<FooQuery> sampleViolation =
        new ConstraintViolation<FooQuery>() {
          @Override
          public String getMessage() {
            return null;
          }

          @Override
          public String getMessageTemplate() {
            return null;
          }

          @Override
          public FooQuery getRootBean() {
            return null;
          }

          @Override
          public Class getRootBeanClass() {
            return null;
          }

          @Override
          public Object getLeafBean() {
            return null;
          }

          @Override
          public Object[] getExecutableParameters() {
            return new Object[0];
          }

          @Override
          public Object getExecutableReturnValue() {
            return null;
          }

          @Override
          public Path getPropertyPath() {
            return null;
          }

          @Override
          public Object getInvalidValue() {
            return null;
          }

          @Override
          public ConstraintDescriptor<?> getConstraintDescriptor() {
            return null;
          }

          @Override
          public Object unwrap(Class type) {
            return null;
          }
        };

    @Mock(answer = Answers.RETURNS_DEEP_STUBS, lenient = true)
    private ProceedingJoinPoint joinPoint;

    private FooQuery query = new FooQuery() {};
    private QueryHandler<FooQuery, String> handler =
        spy(
            new QueryHandler<FooQuery, String>() {
              @Override
              public void verify(@NonNull FooQuery query) throws QueryVerificationException {}

              @Override
              public String handle(@NonNull FooQuery query)
                  throws QueryHandlingException, QueryTimeoutException {
                return "yep";
              }
            });

    @BeforeEach
    void setup() throws Throwable {
      when(joinPoint.getArgs()).thenReturn(new Object[] {query});
      when(joinPoint.getTarget()).thenReturn(handler);
    }

    @Test
    void processesInMetrics() throws Throwable {
      when(joinPoint.proceed()).thenAnswer(invocation -> handler.handle(query));

      underTest.orchestrate(joinPoint);

      ArgumentCaptor<Supplier<?>> cap = ArgumentCaptor.forClass(Supplier.class);
      verify(metrics).timedQuery(any(), anyInt(), cap.capture());

      Supplier<?> shouldRunTheProcessMethod = cap.getValue();
      verify(validator, never()).validate(any());
      verify(joinPoint, never()).proceed();

      shouldRunTheProcessMethod.get();
      verify(validator, times(1)).validate(any());
      verify(joinPoint, times(1)).proceed();
    }

    @Test
    void callsAllLifecycleMethodsExactlyOnce() throws Throwable {
      when(joinPoint.proceed()).thenAnswer(invocation -> handler.handle(query));

      underTest.process(joinPoint);

      verify(validator, times(1)).validate(query);
      verify(handler, times(1)).validate(query);
      verify(handler, times(1)).verify(query);
      verify(handler, times(1)).handle(query);
    }

    @Test
    void beanValidationFails() throws Throwable {

      Set<ConstraintViolation<FooQuery>> violations = new HashSet<>();
      violations.add(sampleViolation);

      when(validator.validate(query)).thenReturn(violations);

      Assertions.assertThrows(QueryValidationException.class, () -> underTest.process(joinPoint));
      verify(validator, times(1)).validate(query);
    }

    @Test
    void customValidationFails() throws Throwable {

      doThrow(RuntimeException.class).when(handler).validate(query);

      Assertions.assertThrows(QueryValidationException.class, () -> underTest.process(joinPoint));
      verify(validator, times(1)).validate(query);
      verify(handler, times(1)).validate(query);
      verify(handler, never()).verify(query);
      verify(handler, never()).handle(query);
    }

    @Test
    void verificationFails() throws Throwable {

      doThrow(RuntimeException.class).when(handler).verify(query);

      Assertions.assertThrows(QueryVerificationException.class, () -> underTest.process(joinPoint));
      verify(validator, times(1)).validate(query);
      verify(handler, times(1)).validate(query);
      verify(handler, times(1)).verify(query);
      verify(handler, never()).handle(query);
    }

    @Test
    void handlingFails() throws Throwable {

      when(joinPoint.proceed()).thenAnswer(invocation -> handler.handle(query));

      when(handler.handle(query)).thenThrow(RuntimeException.class);

      Assertions.assertThrows(QueryHandlingException.class, () -> underTest.process(joinPoint));
      verify(validator, times(1)).validate(query);
      verify(handler, times(1)).validate(query);
      verify(handler, times(1)).verify(query);
      verify(handler, times(1)).handle(query);
    }

    @Test
    void customValidationFailsNoMapping() throws Throwable {
      QueryValidationException e = new QueryValidationException(new RuntimeException());
      doThrow(e).when(handler).validate(query);
      val actual =
          Assertions.assertThrows(
              QueryValidationException.class, () -> underTest.process(joinPoint));
      Assertions.assertSame(e, actual);
    }

    @Test
    void verificationFailsNoMapping() throws Throwable {

      QueryVerificationException e = new QueryVerificationException("", new RuntimeException());
      doThrow(e).when(handler).verify(query);

      val actual =
          Assertions.assertThrows(
              QueryVerificationException.class, () -> underTest.process(joinPoint));
      Assertions.assertSame(e, actual);
    }

    @Test
    void handlingFailsNoMapping() throws Throwable {
      QueryHandlingException e = new QueryHandlingException("", new RuntimeException());
      when(joinPoint.proceed()).thenAnswer(invocation -> handler.handle(query));

      when(handler.handle(query)).thenThrow(e);

      val actual =
          Assertions.assertThrows(QueryHandlingException.class, () -> underTest.process(joinPoint));
      Assertions.assertSame(e, actual);
    }

    @Test
    void preventsNullReturn() throws Throwable {
      when(joinPoint.proceed()).thenAnswer(invocation -> handler.handle(query));
      when(handler.handle(query)).thenReturn(null);

      Assertions.assertThrows(QueryHandlingException.class, () -> underTest.process(joinPoint));
    }

    @Test
    void handlesQueryTimeoutException() throws Throwable {
      when(joinPoint.proceed()).thenAnswer(invocation -> handler.handle(query));
      when(handler.handle(query)).thenThrow(mock(QueryTimeoutException.class));

      Assertions.assertThrows(QueryTimeoutException.class, () -> underTest.process(joinPoint));

      verify(metrics).logTimeout();
    }

    @Test
    void handlesTimeoutException() throws Throwable {
      when(joinPoint.proceed()).thenAnswer(invocation -> handler.handle(query));
      when(handler.handle(query))
          .thenAnswer(
              invocation -> {
                throw new TimeoutException();
              });
      Assertions.assertThrows(QueryTimeoutException.class, () -> underTest.process(joinPoint));

      verify(metrics).logTimeout();
    }

    @Nested
    class RetryBehaviour {
      @Test
      void withRetry() throws Throwable {
        val uut = spy(underTest);
        val handler = new RetryQueryHandler();

        when(joinPoint.getTarget()).thenReturn(handler);
        when(metrics.timedQuery(any(), anyInt(), any()))
            .thenAnswer(
                invocationOnMock -> {
                  final Supplier<?> fn = invocationOnMock.getArgument(2);

                  return fn.get();
                });

        doThrow(new IllegalStateException("foo")).when(uut).process(joinPoint);

        Assertions.assertThrows(RuntimeException.class, () -> uut.orchestrate(joinPoint));

        verify(uut, times(4)).process(joinPoint);
        verify(metrics, times(4)).timedQuery(any(), anyInt(), any());
      }

      @RetryConfiguration
      class RetryQueryHandler implements QueryHandler<FooQuery, String> {
        @Override
        public void verify(@NonNull FooQuery query) throws QueryVerificationException {}

        @Override
        public @NonNull String handle(@NonNull FooQuery query)
            throws QueryHandlingException, QueryTimeoutException {
          return "foo";
        }
      }
    }
  }
}
