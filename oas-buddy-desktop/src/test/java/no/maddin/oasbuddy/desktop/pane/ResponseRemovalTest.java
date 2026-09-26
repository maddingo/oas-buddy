package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.HttpMethod;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The removal flow with the confirmation stubbed out, as in {@link SchemaRemovalTest}. */
class ResponseRemovalTest {

    @Test
    void listsTheOperationsReferringToTheResponse() {
        OasDocument document = documentWithNotFound();
        List<String> asked = new ArrayList<>();

        ResponseRemoval.remove(document, "NotFound", (question, details) -> {
            asked.add(question + " | " + details);
            return false;
        }, () -> { });

        assertEquals(List.of("Remove response \"NotFound\"? | Still referenced by:\n"
                + "  paths → /pets → get → responses → 404\n"
                + "\n"
                + "Those references will be left dangling."), asked);
    }

    @Test
    void removesTheResponseButLeavesTheReferenceWhenConfirmed() {
        OasDocument document = documentWithNotFound();
        AtomicBoolean notified = new AtomicBoolean();

        ResponseRemoval.remove(document, "NotFound", (question, details) -> true, () -> notified.set(true));

        assertAll(
                () -> assertFalse(document.getComponents().getResponses().names().contains("NotFound")),
                () -> assertEquals("NotFound", document.getPaths().getPathItem("/pets").getOperation(HttpMethod.GET)
                        .getResponses().getResponse("404").getReferencedResponseName(),
                        "the reference is left for validation to report"),
                () -> assertTrue(notified.get()));
    }

    @Test
    void keepsTheResponseAndStaysQuietWhenDeclined() {
        OasDocument document = documentWithNotFound();
        AtomicBoolean notified = new AtomicBoolean();

        ResponseRemoval.remove(document, "NotFound", (question, details) -> false, () -> notified.set(true));

        assertTrue(document.getComponents().getResponses().names().contains("NotFound"));
        assertFalse(notified.get());
    }

    private static OasDocument documentWithNotFound() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getComponents().getResponses().addResponse("NotFound").setDescription("Not here");
        document.getPaths().addPath("/pets").addOperation(HttpMethod.GET).getResponses().referTo("404", "NotFound");
        return document;
    }
}
