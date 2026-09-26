package no.maddin.oasbuddy.core.model;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.OasDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Which operations use a tag, the removal dialog's counterpart to {@link SchemaReferences}. */
class TagUsagesTest {

    private static final String DOCUMENT = """
            openapi: 3.0.3
            info:
              title: Tagged
              version: 1.0.0
            tags:
              - name: pets
              - name: store
            paths:
              /pets:
                get:
                  operationId: listPets
                  tags: [pets]
                  responses:
                    '200':
                      description: ok
                post:
                  operationId: addPet
                  tags: [pets, store]
                  responses:
                    '201':
                      description: created
              /store:
                get:
                  operationId: listOrders
                  responses:
                    '200':
                      description: ok
            """;

    @Test
    void findsEveryOperationUsingTheTag() {
        assertEquals(List.of("GET /pets", "POST /pets"), TagUsages.find(load(), "pets"));
    }

    @Test
    void findsATagUsedByOnlyOneOperation() {
        assertEquals(List.of("POST /pets"), TagUsages.find(load(), "store"));
    }

    /** A hand-edited {@code /odd: ~} has no operations, so it cannot use a tag — and must not throw. */
    @Test
    void aPathThatIsNotAnObjectIsSkipped() {
        OasDocument document = load();
        ((com.fasterxml.jackson.databind.node.ObjectNode) document.getRoot().get("paths")).putNull("/odd");

        assertEquals(List.of("GET /pets", "POST /pets"), TagUsages.find(document, "pets"));
    }

    @Test
    void aTagNothingUsesHasNoUsages() {
        assertEquals(List.of(), TagUsages.find(load(), "unused"));
    }

    /**
     * A schema property that happens to share a tag's name is not a usage — the same guarantee
     * {@link SecuritySchemeUsagesTest} makes for schemes.
     */
    @Test
    void aPropertyNamedLikeATagIsNotAUsage() {
        OasDocument document = DocumentReader.read("""
                openapi: 3.0.3
                info:
                  title: Confusing
                  version: 1.0.0
                paths: {}
                components:
                  schemas:
                    Thing:
                      type: object
                      properties:
                        pets:
                          type: string
                """, DocumentFormat.YAML);

        assertEquals(List.of(), TagUsages.find(document, "pets"));
    }

    private static OasDocument load() {
        return DocumentReader.read(DOCUMENT, DocumentFormat.YAML);
    }
}
