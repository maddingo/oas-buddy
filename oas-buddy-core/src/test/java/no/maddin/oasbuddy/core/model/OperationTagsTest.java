package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.JsonNode;
import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.OasDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** An operation's own {@code tags} array, as distinct from the root tags {@link Tags} defines. */
class OperationTagsTest {

    @Test
    void readingAnUntaggedOperationNeverAddsTheKey() {
        OasDocument document = bareWithOperation();

        assertTrue(operationOf(document).getTags().isEmpty());
        assertFalse(getOperationNode(document).has("tags"), "reading added a tags key");
    }

    @Test
    void settingTagsWritesThemInOrder() {
        OasDocument document = bareWithOperation();

        operationOf(document).setTags(List.of("pets", "store"));

        assertEquals(List.of("pets", "store"), operationOf(document).getTags());
    }

    /** {@code tags: []} is noise a clean diff should never carry; the key is dropped instead. */
    @Test
    void settingAnEmptyListRemovesTheKeyRatherThanLeavingAnEmptyArray() {
        OasDocument document = bareWithOperation();
        operationOf(document).setTags(List.of("pets"));

        operationOf(document).setTags(List.of());

        assertTrue(operationOf(document).getTags().isEmpty());
        assertFalse(getOperationNode(document).has("tags"), "an empty tag list should remove the key");
    }

    private static OasDocument bareWithOperation() {
        OasDocument document = DocumentReader.read("""
                openapi: 3.0.3
                info:
                  title: Bare
                  version: 1.0.0
                paths: {}
                """, DocumentFormat.YAML);
        document.getPaths().addPath("/pets").addOperation(HttpMethod.GET).setOperationId("listPets");
        return document;
    }

    private static Operation operationOf(OasDocument document) {
        return document.getPaths().getPathItem("/pets").getOperation(HttpMethod.GET);
    }

    private static JsonNode getOperationNode(OasDocument document) {
        return document.getRoot().get("paths").get("/pets").get("get");
    }
}
