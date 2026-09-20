package no.maddin.oasbuddy.core.model;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.OasDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A security scheme is referenced by name from a {@code security} requirement, not by {@code $ref},
 * so removing one leaves a dangling requirement rather than a dangling reference. Same problem as
 * {@link SchemaReferences} solves for schemas, different spelling.
 */
class SecuritySchemeUsagesTest {

    private static final String DOCUMENT = """
            openapi: 3.0.3
            info:
              title: Secured
              version: 1.0.0
            security:
              - ApiKeyAuth: []
            paths:
              /pets:
                get:
                  operationId: listPets
                  security:
                    - ApiKeyAuth: []
                    - BearerAuth: []
                  responses:
                    '200':
                      description: ok
                post:
                  operationId: addPet
                  responses:
                    '201':
                      description: created
            components:
              securitySchemes:
                ApiKeyAuth:
                  type: apiKey
                  name: X-API-Key
                  in: header
                BearerAuth:
                  type: http
                  scheme: bearer
                Unused:
                  type: http
                  scheme: basic
            """;

    @Test
    void findsBothTheDocumentWideAndTheOperationRequirement() {
        assertEquals(
                List.of("security → [0]", "paths → /pets → get → security → [0]"),
                SecuritySchemeUsages.find(load(), "ApiKeyAuth"));
    }

    @Test
    void findsARequirementThatIsNotTheFirstInItsList() {
        assertEquals(
                List.of("paths → /pets → get → security → [1]"),
                SecuritySchemeUsages.find(load(), "BearerAuth"));
    }

    @Test
    void aSchemeNothingRequiresHasNoUsages() {
        assertEquals(List.of(), SecuritySchemeUsages.find(load(), "Unused"));
    }

    /**
     * Only {@code security} requirements count. A schema property, a path or a response header
     * that happens to share the scheme's name is not a usage, and reporting it would teach the
     * user to ignore the dialog.
     */
    @Test
    void aFieldElsewhereThatSharesTheNameIsNotAUsage() {
        OasDocument document = DocumentReader.read("""
                openapi: 3.0.3
                info:
                  title: Confusing
                  version: 1.0.0
                paths: {}
                components:
                  schemas:
                    Credentials:
                      type: object
                      properties:
                        ApiKeyAuth:
                          type: string
                  securitySchemes:
                    ApiKeyAuth:
                      type: apiKey
                      name: X-API-Key
                      in: header
                """, DocumentFormat.YAML);

        assertEquals(List.of(), SecuritySchemeUsages.find(document, "ApiKeyAuth"));
    }

    private static OasDocument load() {
        return DocumentReader.read(DOCUMENT, DocumentFormat.YAML);
    }
}
