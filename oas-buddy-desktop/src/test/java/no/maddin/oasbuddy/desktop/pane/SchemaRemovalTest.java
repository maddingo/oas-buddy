package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The removal flow itself, with the confirmation stubbed out; the dialog it normally shows is
 * covered only by the fact that it implements the same interface.
 */
class SchemaRemovalTest {

    @Test
    void removesTheSchemaAndNotifiesWhenConfirmed() {
        OasDocument document = documentWithPet();
        AtomicBoolean notified = new AtomicBoolean();

        SchemaRemoval.remove(document, "Pet", (question, details) -> true, () -> notified.set(true));

        assertFalse(document.getComponents().getSchemas().names().contains("Pet"));
        assertTrue(notified.get());
    }

    @Test
    void keepsTheSchemaAndStaysQuietWhenDeclined() {
        OasDocument document = documentWithPet();
        AtomicBoolean notified = new AtomicBoolean();

        SchemaRemoval.remove(document, "Pet", (question, details) -> false, () -> notified.set(true));

        assertTrue(document.getComponents().getSchemas().names().contains("Pet"));
        assertFalse(notified.get());
    }

    @Test
    void asksForConfirmationEvenWhenNothingReferencesTheSchema() {
        OasDocument document = documentWithPet();
        List<String> asked = new ArrayList<>();

        SchemaRemoval.remove(document, "Pet", (question, details) -> {
            asked.add(question + " | " + details);
            return false;
        }, () -> { });

        assertEquals(List.of("Remove schema \"Pet\"? | Nothing references it."), asked);
    }

    @Test
    void passesTheReferencingLocationsToTheConfirmation() {
        OasDocument document = documentWithPet();
        document.getPaths().addPath("/pets").addOperation(no.maddin.oasbuddy.core.model.HttpMethod.GET)
                .getResponses().addResponse("200").getSchema("application/json")
                .setRef("#/components/schemas/Pet");
        List<String> asked = new ArrayList<>();

        SchemaRemoval.remove(document, "Pet", (question, details) -> {
            asked.add(details);
            return false;
        }, () -> { });

        assertEquals(List.of("Still referenced by:\n"
                        + "  paths → /pets → get → responses → 200 → content → application/json → schema\n"
                        + "\n"
                        + "Those references will be left dangling."),
                asked);
    }

    @Test
    void spellsOutEveryReferenceThatWouldBeLeftDangling() {
        String described = SchemaRemoval.describeUsages(
                List.of("paths → /pets → get", "paths → /pets → post"));

        assertEquals("Still referenced by:\n"
                + "  paths → /pets → get\n"
                + "  paths → /pets → post\n"
                + "\n"
                + "Those references will be left dangling.", described);
    }

    @Test
    void saysSoWhenNothingReferencesTheSchema() {
        assertEquals("Nothing references it.", SchemaRemoval.describeUsages(List.of()));
    }

    private static OasDocument documentWithPet() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getComponents().getSchemas().addSchema("Pet").setType("object");
        return document;
    }
}
