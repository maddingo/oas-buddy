package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.OasDocument;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The removal flow, with the confirmation stubbed out. Mirrors {@link SecuritySchemeRemovalTest}. */
class TagRemovalTest {

    @Test
    void removesTheTagAndNotifiesWhenConfirmed() {
        OasDocument document = tagged();
        AtomicBoolean notified = new AtomicBoolean();

        TagRemoval.remove(document, "pets", (question, details) -> true, () -> notified.set(true));

        assertFalse(document.getTags().names().contains("pets"));
        assertTrue(notified.get());
    }

    @Test
    void keepsTheTagAndStaysQuietWhenDeclined() {
        OasDocument document = tagged();
        AtomicBoolean notified = new AtomicBoolean();

        TagRemoval.remove(document, "pets", (question, details) -> false, () -> notified.set(true));

        assertTrue(document.getTags().names().contains("pets"));
        assertFalse(notified.get());
    }

    @Test
    void tellsTheUserWhichOperationsTheRemovalWouldLeaveDangling() {
        List<String> asked = new ArrayList<>();

        TagRemoval.remove(tagged(), "pets", (question, details) -> {
            asked.add(question + " | " + details);
            return false;
        }, () -> { });

        assertEquals(List.of("Remove tag \"pets\"? | Still used by:"
                + "\n  GET /pets"
                + "\n\nThose operations will be left using an undefined tag."), asked);
    }

    @Test
    void asksForConfirmationEvenWhenNothingUsesTheTag() {
        List<String> asked = new ArrayList<>();

        TagRemoval.remove(tagged(), "store", (question, details) -> {
            asked.add(question + " | " + details);
            return false;
        }, () -> { });

        assertEquals(List.of("Remove tag \"store\"? | No operation uses it."), asked);
    }

    private static OasDocument tagged() {
        return DocumentReader.read("""
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
                """, DocumentFormat.YAML);
    }
}
