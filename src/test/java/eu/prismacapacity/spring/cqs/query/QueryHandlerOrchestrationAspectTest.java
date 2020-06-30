package eu.prismacapacity.spring.cqs.query;

import javax.validation.Validator;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import eu.prismacapacity.spring.cqs.metrics.QueryMetrics;

@ExtendWith(MockitoExtension.class)
class QueryHandlerOrchestrationAspectTest {

    @Mock
    private Validator validator;
    @Mock
    private QueryMetrics metrics;
    @InjectMocks
    private QueryHandlerOrchestrationAspect underTest;

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
