package no.maddin.oasbuddy.core.validation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkCheckTest {

    private final OasValidator validator = new OasValidator();

    @Test
    void aLinkToAnExistingOperationIdIsFine() {
        assertEquals(List.of(), messages("""
                paths:
                  /pets:
                    post:
                      operationId: addPet
                      responses:
                        '201':
                          description: created
                          links:
                            GetPet:
                              operationId: getPet
                  /pets/{id}:
                    get:
                      operationId: getPet
                      parameters:
                        - {name: id, in: path, required: true, schema: {type: string}}
                      responses:
                        '200':
                          description: ok
                """));
    }

    @Test
    void aLinkToAMissingOperationIdIsReportedWhereverTheLinkLives() {
        List<String> messages = messages("""
                paths:
                  /pets:
                    post:
                      responses:
                        '201':
                          description: created
                          links:
                            GetPet:
                              operationId: getPet
                components:
                  links:
                    Shared:
                      operationId: nowhere
                """);

        assertEquals(List.of(
                "Link targets operationId \"getPet\", which no operation declares "
                        + "(at paths → /pets → post → responses → 201 → links → GetPet)",
                "Link targets operationId \"nowhere\", which no operation declares "
                        + "(at components → links → Shared)"), messages);
    }

    @Test
    void anOperationInsideACallbackCanBeTargeted() {
        assertEquals(List.of(), messages("""
                paths:
                  /subscribe:
                    post:
                      responses:
                        '200':
                          description: ok
                          links:
                            Ack:
                              operationId: onEvent
                      callbacks:
                        event:
                          '{$request.body#/url}':
                            post:
                              operationId: onEvent
                              responses:
                                '200':
                                  description: ok
                """));
    }

    @Test
    void bothTargetsAtOnceIsReported() {
        List<String> messages = messages("""
                paths:
                  /a:
                    get:
                      operationId: a
                      responses:
                        '200':
                          description: ok
                          links:
                            L:
                              operationId: a
                              operationRef: '#/paths/~1a/get'
                """);

        assertEquals(1, messages.size(), () -> messages.toString());
        assertTrue(messages.getFirst().startsWith("Link sets both operationId and operationRef"));
    }

    @Test
    void aSchemaPropertyCalledLinksIsNotALink() {
        assertEquals(List.of(), messages("""
                paths: {}
                components:
                  schemas:
                    Page:
                      type: object
                      properties:
                        links:
                          type: object
                          properties:
                            next:
                              type: string
                """));
    }

    private List<String> messages(String body) {
        String document = "openapi: 3.0.3\ninfo: {title: t, version: '1'}\n" + body;
        return validator.validate(document).messages().stream().map(ValidationMessage::message).toList();
    }
}
