package eu.prismacapacity.spring.cqs.cmd;

import static org.mockito.Mockito.*;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.Validator;
import javax.validation.metadata.ConstraintDescriptor;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.prismacapacity.spring.cqs.metrics.CommandMetrics;
import lombok.NonNull;
import lombok.val;

@ExtendWith(MockitoExtension.class)
class CommandHandlerOrchestrationAspectTest {

    @Mock
    private Validator validator;
    @Mock
    private CommandMetrics metrics;
    @InjectMocks
    private CommandHandlerOrchestrationAspect underTest;


    @Nested
    class WhenOrchestrating {

        class FooCommand implements Command {
        }

        ConstraintViolation<FooCommand> sampleViolation = new ConstraintViolation<FooCommand>() {
            @Override
            public String getMessage() {
                return null;
            }

            @Override
            public String getMessageTemplate() {
                return null;
            }

            @Override
            public FooCommand getRootBean() {
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

        @Mock(answer = Answers.RETURNS_DEEP_STUBS)
        private ProceedingJoinPoint joinPoint;
        private FooCommand cmd = new FooCommand() {
        };
        private CommandHandler<FooCommand> handler = spy(new CommandHandler<FooCommand>() {
            @Override
            public void verify(@NonNull FooCommand cmd) throws CommandVerificationException {
            }

            @Override
            public @NonNull CommandTokenResponse handle(@NonNull FooCommand cmd) throws CommandHandlingException {
                return CommandTokenResponse.empty();
            }
        });

        @BeforeEach
        void setup() throws Throwable {
            when(joinPoint.getArgs()).thenReturn(new Command[]{cmd});
            when(joinPoint.getTarget()).thenReturn(handler);

        }

        @Test
        void processesInMetrics() throws Throwable {
            when(joinPoint.proceed()).thenAnswer(invocation -> handler.handle(cmd));

            underTest.orchestrate(joinPoint);

            ArgumentCaptor<Supplier<?>> cap = ArgumentCaptor.forClass(Supplier.class);
            verify(metrics).timedCommand(any(), cap.capture());

            Supplier<?> shouldRunTheProcessMethod = cap.getValue();
            verify(validator, never()).validate(any());
            verify(joinPoint, never()).proceed();

            shouldRunTheProcessMethod.get();
            verify(validator, times(1)).validate(any());
            verify(joinPoint, times(1)).proceed();

        }

        @Test
        void callsAllLifecycleMethodsExactlyOnce() throws Throwable {
            when(joinPoint.proceed()).thenAnswer(invocation -> handler.handle(cmd));

            underTest.process(joinPoint);

            verify(validator, times(1)).validate(cmd);
            verify(handler, times(1)).validate(cmd);
            verify(handler, times(1)).verify(cmd);
            verify(handler, times(1)).handle(cmd);


        }

        @Test
        void beanValidationFails() throws Throwable {

            Set<ConstraintViolation<FooCommand>> violations = new HashSet<>();
            violations.add(sampleViolation);

            when(validator.validate(cmd)).thenReturn(violations);

            Assertions.assertThrows(CommandValidationException.class, () -> underTest.process(joinPoint));
            verify(validator, times(1)).validate(cmd);
        }

        @Test
        void customValidationFails() throws Throwable {
            doThrow(RuntimeException.class).when(handler).validate(cmd);

            Assertions.assertThrows(CommandValidationException.class, () -> underTest.process(joinPoint));
            verify(validator, times(1)).validate(cmd);
            verify(handler, times(1)).validate(cmd);
            verify(handler, never()).verify(cmd);
            verify(handler, never()).handle(cmd);

        }

        @Test
        void verificationFails() throws Throwable {
            doThrow(RuntimeException.class).when(handler).verify(cmd);

            Assertions.assertThrows(CommandVerificationException.class, () -> underTest.process(joinPoint));
            verify(validator, times(1)).validate(cmd);
            verify(handler, times(1)).validate(cmd);
            verify(handler, times(1)).verify(cmd);
            verify(handler, never()).handle(cmd);

        }

        @Test
        void handlingFails() throws Throwable {

            when(joinPoint.proceed()).thenAnswer(invocation -> handler.handle(cmd));

            when(handler.handle(cmd)).thenThrow(RuntimeException.class);

            Assertions.assertThrows(CommandHandlingException.class, () -> underTest.process(joinPoint));
            verify(validator, times(1)).validate(cmd);
            verify(handler, times(1)).validate(cmd);
            verify(handler, times(1)).verify(cmd);
            verify(handler, times(1)).handle(cmd);

        }


        @Test
        void customValidationFailsNoMapping() throws Throwable {
            CommandValidationException e=new CommandValidationException(new RuntimeException());
            doThrow(e).when(handler).validate(cmd);
            val actual = Assertions.assertThrows(CommandValidationException.class, () -> underTest.process(joinPoint));
            Assertions.assertSame(e, actual);
        }

        @Test
        void verificationFailsNoMapping() throws Throwable {

            CommandVerificationException e=new CommandVerificationException("",new RuntimeException());
            doThrow(e).when(handler).verify(cmd);

            val actual = Assertions.assertThrows(CommandVerificationException.class, () -> underTest.process(joinPoint));
            Assertions.assertSame(e, actual);

        }

        @Test
        void handlingFailsNoMapping() throws Throwable {
            CommandHandlingException e=new CommandHandlingException("",new RuntimeException());
            when(joinPoint.proceed()).thenAnswer(invocation -> handler.handle(cmd));

            when(handler.handle(cmd)).thenThrow(e);

            val actual = Assertions.assertThrows(CommandHandlingException.class, () -> underTest.process(joinPoint));
            Assertions.assertSame(e, actual);
        }
    }
}
