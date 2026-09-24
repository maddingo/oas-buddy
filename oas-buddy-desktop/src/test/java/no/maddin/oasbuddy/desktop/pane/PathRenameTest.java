package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.HttpMethod;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Renaming a path. Nothing can $ref a path, so there is no confirmation seam to stub out. */
class PathRenameTest {

    @Test
    void renamesThePathAndNotifies() {
        OasDocument document = documentWithPets();
        AtomicBoolean notified = new AtomicBoolean();

        boolean renamed = PathRename.rename(document, "/pets", "/animals", () -> notified.set(true));

        assertTrue(renamed);
        assertEquals(List.of("/animals"), document.getPaths().pathNames());
        assertEquals("listPets",
                document.getPaths().getPathItem("/animals").getOperation(HttpMethod.GET).getOperationId());
        assertTrue(notified.get());
    }

    @Test
    void renamingOntoAnExistingPathIsRefused() {
        OasDocument document = documentWithPets();
        document.getPaths().addPath("/owners");
        AtomicBoolean notified = new AtomicBoolean();

        boolean renamed = PathRename.rename(document, "/pets", "/owners", () -> notified.set(true));

        assertFalse(renamed);
        assertEquals(List.of("/pets", "/owners"), document.getPaths().pathNames());
        assertFalse(notified.get());
    }

    private static OasDocument documentWithPets() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getPaths().addPath("/pets").addOperation(HttpMethod.GET).setOperationId("listPets");
        return document;
    }
}
