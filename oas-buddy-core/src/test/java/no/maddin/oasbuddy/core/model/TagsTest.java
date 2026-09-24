package no.maddin.oasbuddy.core.model;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.OasDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The root {@code tags} array. Absent and empty mean the same thing here, the same rule
 * {@link SecurityRequirements} follows at document level.
 */
class TagsTest {

    @Test
    void aDocumentWithNoTagsDeclaresNone() {
        OasDocument document = bare();

        assertTrue(document.getTags().names().isEmpty());
        assertNull(document.getTags().get("pets"));
        assertFalse(document.getRoot().has("tags"), "reading added a tags key");
    }

    @Test
    void anAddedTagLandsInTheRootArray() {
        OasDocument document = bare();

        document.getTags().add("pets").setDescription("Everything about pets");

        assertEquals("Everything about pets", document.getRoot().get("tags").get(0).get("description").asText());
        assertEquals("pets", document.getRoot().get("tags").get(0).get("name").asText());
    }

    @Test
    void namesComeBackInDocumentOrder() {
        OasDocument document = bare();

        document.getTags().add("pets");
        document.getTags().add("store");

        assertEquals(List.of("pets", "store"), document.getTags().names());
    }

    @Test
    void addingAnExistingNameReturnsTheSameTagRatherThanDuplicatingIt() {
        OasDocument document = bare();
        document.getTags().add("pets").setDescription("Everything about pets");

        Tag again = document.getTags().add("pets");

        assertEquals(1, document.getTags().names().size());
        assertEquals("Everything about pets", again.getDescription());
    }

    @Test
    void removingTheLastTagTakesTheKeyOutOfTheDocument() {
        OasDocument document = bare();
        document.getTags().add("pets");

        document.getTags().remove("pets");

        assertFalse(document.getRoot().has("tags"), "the last tag going should remove the key");
    }

    @Test
    void removingOneTagLeavesTheOthersInOrder() {
        OasDocument document = bare();
        document.getTags().add("pets");
        document.getTags().add("store");
        document.getTags().add("users");

        document.getTags().remove("store");

        assertEquals(List.of("pets", "users"), document.getTags().names());
    }

    @Test
    void tagsAreReadFromALoadedDocument() {
        OasDocument document = DocumentReader.read("""
                openapi: 3.0.3
                info:
                  title: Tagged
                  version: 1.0.0
                paths: {}
                tags:
                  - name: pets
                    description: Everything about pets
                """, DocumentFormat.YAML);

        Tag pets = document.getTags().get("pets");

        assertEquals("pets", pets.getName());
        assertEquals("Everything about pets", pets.getDescription());
    }

    private static OasDocument bare() {
        return DocumentReader.read("""
                openapi: 3.0.3
                info:
                  title: Bare
                  version: 1.0.0
                paths: {}
                """, DocumentFormat.YAML);
    }
}
