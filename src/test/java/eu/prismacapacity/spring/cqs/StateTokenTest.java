package eu.prismacapacity.spring.cqs;

import java.util.UUID;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StateTokenTest {

    private static final UUID TOKEN = UUID.randomUUID();
    @InjectMocks
    private StateToken underTest;

    @Nested
    class WhenRandoming {
        @BeforeEach
        void setup() {
        }

        @Test
        void randomReturnsNonNull() {
            Assertions.assertNotNull(StateToken.random());
        }
    }
}
