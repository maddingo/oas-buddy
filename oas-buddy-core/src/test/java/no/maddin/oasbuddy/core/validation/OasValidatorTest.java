package no.maddin.oasbuddy.core.validation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OasValidatorTest {

    private final OasValidator validator = new OasValidator();

    @Test
    void validFixtureHasNoErrors() throws IOException {
        ValidationResult result = validator.validate(readFixture("petstore.yaml"));
        assertTrue(result.isValid(), () -> "Unexpected messages: " + result.messages());
    }

    @Test
    void invalidFixtureHasErrors() throws IOException {
        ValidationResult result = validator.validate(readFixture("invalid.yaml"));
        assertFalse(result.isValid());
        assertFalse(result.messages().isEmpty());
    }

    private static String readFixture(String name) throws IOException {
        try (InputStream is = OasValidatorTest.class.getResourceAsStream("/fixtures/" + name)) {
            if (is == null) {
                throw new IOException("Fixture not found: " + name);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
