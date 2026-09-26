package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.HttpMethod;
import no.maddin.oasbuddy.core.model.PathItem;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The removal flow with the confirmation stubbed out, as in {@link SchemaRemovalTest}. */
class ParameterRemovalTest {

    @Test
    void listsThePathsAndOperationsReferringToTheParameter() {
        OasDocument document = documentWithLimit();
        List<String> asked = new ArrayList<>();

        ParameterRemoval.remove(document, "Limit", (question, details) -> {
            asked.add(question + " | " + details);
            return false;
        }, () -> { });

        assertEquals(List.of("Remove parameter \"Limit\"? | Still referenced by:\n"
                + "  paths → /pets → parameters → [0]\n"
                + "  paths → /pets → get → parameters → [0]\n"
                + "\n"
                + "Those references will be left dangling."), asked);
    }

    @Test
    void removesTheParameterButLeavesTheReferencesWhenConfirmed() {
        OasDocument document = documentWithLimit();
        AtomicBoolean notified = new AtomicBoolean();

        ParameterRemoval.remove(document, "Limit", (question, details) -> true, () -> notified.set(true));

        assertAll(
                () -> assertFalse(document.getComponents().getParameters().names().contains("Limit")),
                () -> assertEquals("Limit", document.getPaths().getPathItem("/pets").getParameters().all()
                        .getFirst().getReferencedParameterName(), "left for validation to report"),
                () -> assertTrue(notified.get()));
    }

    @Test
    void keepsTheParameterAndStaysQuietWhenDeclined() {
        OasDocument document = documentWithLimit();
        AtomicBoolean notified = new AtomicBoolean();

        ParameterRemoval.remove(document, "Limit", (question, details) -> false, () -> notified.set(true));

        assertTrue(document.getComponents().getParameters().names().contains("Limit"));
        assertFalse(notified.get());
    }

    private static OasDocument documentWithLimit() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getComponents().getParameters().addParameter("Limit", "limit", "query");
        PathItem pets = document.getPaths().addPath("/pets");
        pets.getParameters().addReference("Limit");
        pets.addOperation(HttpMethod.GET).getParameters().addReference("Limit");
        return document;
    }
}
