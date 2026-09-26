package no.maddin.oasbuddy.core.validation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Local references that point at nothing, which swagger-parser does not report with resolving
 * switched off. Asserts on every message the validator produces, not just this check's, so a
 * duplicate from swagger-parser would show up here too.
 */
class ReferenceCheckTest {

    private final OasValidator validator = new OasValidator();

    /** swagger-parser reports this one itself ("… is missing"); saying it twice would be noise. */
    @Test
    void aDanglingSchemaReferenceIsReportedOnlyOnce() {
        List<String> messages = messages("""
                paths:
                  /pets:
                    get:
                      responses:
                        '200':
                          description: ok
                          content:
                            application/json:
                              schema:
                                $ref: '#/components/schemas/Pet'
                """);

        assertEquals(1, messages.size(), () -> "expected exactly one message, got " + messages);
    }

    @Test
    void aDanglingResponseReferenceIsReported() {
        List<String> messages = messages("""
                paths:
                  /pets:
                    get:
                      responses:
                        '404':
                          $ref: '#/components/responses/NotFound'
                """);

        assertEquals(List.of("Reference #/components/responses/NotFound does not resolve "
                + "(at paths → /pets → get → responses → 404)"), messages);
    }

    @Test
    void aDanglingParameterReferenceIsReportedOnce() {
        List<String> messages = messages("""
                paths:
                  /pets:
                    get:
                      parameters:
                        - $ref: '#/components/parameters/Limit'
                      responses:
                        '200':
                          description: ok
                """);

        assertEquals(1, messages.size(), () -> "expected exactly one message, got " + messages);
    }

    @Test
    void aDanglingExampleReferenceIsReportedOnce() {
        List<String> messages = messages("""
                paths:
                  /pets:
                    get:
                      responses:
                        '200':
                          description: ok
                          content:
                            application/json:
                              examples:
                                cat:
                                  $ref: '#/components/examples/Cat'
                """);

        assertEquals(1, messages.size(), () -> "expected exactly one message, got " + messages);
    }

    @Test
    void aReferenceThatResolvesIsNotReported() {
        assertTrue(messages("""
                paths:
                  /pets:
                    get:
                      responses:
                        '404':
                          $ref: '#/components/responses/NotFound'
                components:
                  responses:
                    NotFound:
                      description: gone
                """).isEmpty());
    }

    @Test
    void aRefKeyInsideExampleDataIsNotAReference() {
        assertTrue(messages("""
                paths: {}
                components:
                  schemas:
                    Link:
                      type: object
                      example:
                        $ref: '#/nowhere'
                      default:
                        $ref: '#/nowhere'
                """).isEmpty());
    }

    @Test
    void anExternalReferenceIsLeftAlone() {
        assertTrue(messages("""
                paths:
                  /pets:
                    get:
                      responses:
                        '404':
                          $ref: 'common.yaml#/components/responses/NotFound'
                """).isEmpty());
    }

    private List<String> messages(String body) {
        return validator.validate(document(body)).messages().stream()
                .map(ValidationMessage::message)
                .toList();
    }

    private static String document(String body) {
        return """
                openapi: 3.0.3
                info:
                  title: t
                  version: '1'
                """ + body;
    }
}
