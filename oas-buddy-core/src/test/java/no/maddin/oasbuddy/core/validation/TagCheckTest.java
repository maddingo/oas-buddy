package no.maddin.oasbuddy.core.validation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Undefined tags, which swagger-parser does not flag on its own. A loaded document using an
 * undefined tag is not rewritten (see {@code core.model.Tags}), so this is where it is reported
 * instead — as a warning, not an error: OAS does not require a tag an operation uses to be declared
 * at the root, unlike a duplicate root name, which really is a spec violation.
 */
class TagCheckTest {

    private final OasValidator validator = new OasValidator();

    @Test
    void anOperationTaggedWithSomethingUndefinedIsReported() {
        List<String> messages = messages("""
                tags:
                  - name: pets
                paths:
                  /pets:
                    get:
                      tags: [pets, ghost]
                      responses:
                        '200':
                          description: ok
                """);

        assertEquals(List.of("GET /pets uses undefined tag \"ghost\"."), messages);
    }

    @Test
    void aDefinedTagIsNotReported() {
        List<String> messages = messages("""
                tags:
                  - name: pets
                paths:
                  /pets:
                    get:
                      tags: [pets]
                      responses:
                        '200':
                          description: ok
                """);

        assertEquals(List.of(), messages);
    }

    @Test
    void anOperationWithNoTagsIsNotReported() {
        List<String> messages = messages("""
                paths:
                  /pets:
                    get:
                      responses:
                        '200':
                          description: ok
                """);

        assertEquals(List.of(), messages);
    }

    /**
     * Not this class's job: swagger-parser already reports {@code attribute tags.pets is repeated}
     * for a duplicate root tag name (verified for both YAML and JSON), so checking it again here
     * would just show the same problem under a second message. A duplicate name is a real spec
     * violation, so it stays an error.
     */
    @Test
    void aDuplicateTagNameInTheRootListIsAlreadyCaughtBySwaggerParserAsAnError() {
        var result = validator.validate(document("""
                tags:
                  - name: pets
                  - name: pets
                paths: {}
                """));

        assertTrue(result.messages().stream()
                .anyMatch(m -> m.severity() == ValidationSeverity.ERROR
                        && m.message().contains("tags.pets is repeated")));
    }

    /**
     * A warning, not an error: it must reach {@code MainApp}'s validation panel (which shows both),
     * but it must not make an otherwise-valid document read as invalid.
     */
    @Test
    void undefinedTagsAreReportedAsWarningsRatherThanErrors() {
        var result = validator.validate(document("""
                paths:
                  /pets:
                    get:
                      tags: [ghost]
                      responses:
                        '200':
                          description: ok
                """));

        assertTrue(result.messages().stream()
                .anyMatch(m -> m.severity() == ValidationSeverity.WARNING
                        && m.message().contains("undefined tag")));
        assertTrue(result.isValid(), "an undefined tag is a warning, not an error");
    }

    private List<String> messages(String body) {
        return validator.validate(document(body)).messages().stream()
                .filter(m -> m.message().contains("tag"))
                .map(ValidationMessage::message)
                .toList();
    }

    private static String document(String body) {
        return """
                openapi: 3.0.3
                info:
                  title: t
                  version: '1'
                """ + (body.contains("paths:") ? "" : "paths: {}\n") + body;
    }
}
