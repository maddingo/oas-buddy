package no.maddin.oasbuddy.core.validation;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Constraint problems swagger-parser lets through, reported by the validation panel instead. */
class ConstraintCheckTest {

    private final OasValidator validator = new OasValidator();

    @Test
    void aLowerBoundAboveItsUpperBoundIsReported() {
        List<String> messages = messages("""
                components:
                  schemas:
                    Name:
                      type: string
                      minLength: 10
                      maxLength: 2
                """);

        assertEquals(List.of("components.schemas.Name: minLength (10) is greater than maxLength (2),"
                + " so no value can satisfy it"), messages);
    }

    @Test
    void valuesOfTheWrongKindAreReported() {
        List<String> messages = messages("""
                components:
                  schemas:
                    Age:
                      type: integer
                      minimum: "0"
                      exclusiveMinimum: 5
                    Tags:
                      type: array
                      items:
                        type: string
                      minItems: -1
                """);

        assertEquals(List.of(
                "components.schemas.Age.minimum must be a number, but is \"0\"",
                "components.schemas.Age.exclusiveMinimum must be a boolean, but is 5",
                "components.schemas.Tags.minItems must be a non-negative integer, but is -1"), messages);
    }

    @Test
    void nestedSchemasAreCheckedWhereverTheyAre() {
        List<String> messages = messages("""
                paths:
                  /pets:
                    get:
                      parameters:
                        - name: limit
                          in: query
                          schema:
                            type: integer
                            minimum: 10
                            maximum: 1
                      responses:
                        '200':
                          description: ok
                          content:
                            application/json:
                              schema:
                                type: array
                                items:
                                  type: object
                                  properties:
                                    name:
                                      type: string
                                      maxLength: -5
                components:
                  schemas:
                    Pet:
                      type: object
                      additionalProperties:
                        type: array
                        items:
                          type: string
                        minItems: 3
                        maxItems: 1
                """);

        assertAll(
                () -> assertEquals(3, messages.size(), () -> "messages: " + messages),
                () -> assertTrue(messages.get(0).startsWith("components.schemas.Pet.additionalProperties: minItems")),
                () -> assertTrue(messages.get(1).startsWith("paths./pets.get.parameters[0].schema: minimum")),
                () -> assertTrue(messages.get(2).startsWith(
                        "paths./pets.get.responses.200.content.application/json.schema.items.properties.name.maxLength")));
    }

    @Test
    void aPropertyNamedLikeAConstraintIsNotOne() {
        List<String> messages = messages("""
                components:
                  schemas:
                    Rule:
                      type: object
                      properties:
                        minLength:
                          type: integer
                      example:
                        minLength: nonsense
                """);

        assertEquals(List.of(), messages);
    }

    private List<String> messages(String body) {
        String document = """
                openapi: 3.0.3
                info:
                  title: t
                  version: '1'
                """ + (body.startsWith("paths:") ? "" : "paths: {}\n") + body;
        return validator.validate(document).messages().stream().map(ValidationMessage::message).toList();
    }
}
