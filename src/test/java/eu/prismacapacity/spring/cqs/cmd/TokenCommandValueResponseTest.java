package eu.prismacapacity.spring.cqs.cmd;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

import lombok.val;

class TokenCommandValueResponseTest {

    @Test
    void empty() {
        val r = CommandValueResponse.empty();
        assertNull(r.getToken());
        assertNull(r.getValue());
    }
}
