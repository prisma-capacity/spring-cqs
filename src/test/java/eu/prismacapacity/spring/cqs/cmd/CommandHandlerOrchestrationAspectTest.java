package eu.prismacapacity.spring.cqs.cmd;

import javax.validation.Validator;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.prismacapacity.spring.cqs.metrics.CommandMetrics;

@ExtendWith(MockitoExtension.class)
class CommandHandlerOrchestrationAspectTest {

    @Mock
    private Validator validator;
    @Mock
    private CommandMetrics metrics;
    @InjectMocks
    private CommandHandlerOrchestrationAspect underTest;

    @Nested
    class WhenCommandingHandlerPointCut {
        @BeforeEach
        void setup() {
        }
    }

    @Nested
    class WhenReturningingCommandHandlerPointCut {
        @BeforeEach
        void setup() {
        }
    }

    @Nested
    class WhenOrchestrating {
        @Mock
        private ProceedingJoinPoint joinPoint;

        @BeforeEach
        void setup() {
        }
    }

    @Nested
    class WhenProcessing {
        @Mock
        private ProceedingJoinPoint joinPoint;

        @BeforeEach
        void setup() {
        }
    }
}
