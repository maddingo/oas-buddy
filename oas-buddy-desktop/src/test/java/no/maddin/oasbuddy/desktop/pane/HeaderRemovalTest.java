package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.HttpMethod;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The removal flow with the confirmation stubbed out, as in {@link ExampleRemovalTest}. */
class HeaderRemovalTest {

    @Test
    void listsTheResponsesReferringToTheHeader() {
        OasDocument document = documentWithRate();
        List<String> asked = new ArrayList<>();

        HeaderRemoval.remove(document, "Rate", (question, details) -> {
            asked.add(question + " | " + details);
            return false;
        }, () -> { });

        assertEquals(List.of("Remove header \"Rate\"? | Still referenced by:\n"
                + "  paths → /pets → get → responses → 200 → headers → X-Rate\n"
                + "\n"
                + "Those references will be left dangling."), asked);
    }

    @Test
    void removesTheHeaderWhenConfirmed() {
        OasDocument document = documentWithRate();
        AtomicBoolean notified = new AtomicBoolean();

        HeaderRemoval.remove(document, "Rate", (question, details) -> true, () -> notified.set(true));

        assertFalse(document.getComponents().getHeaders().names().contains("Rate"));
        assertTrue(notified.get());
    }

    @Test
    void keepsTheHeaderWhenDeclined() {
        OasDocument document = documentWithRate();

        HeaderRemoval.remove(document, "Rate", (question, details) -> false, () -> { });

        assertTrue(document.getComponents().getHeaders().names().contains("Rate"));
    }

    private static OasDocument documentWithRate() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getComponents().getHeaders().addHeader("Rate").setDescription("Calls left");
        var ok = document.getPaths().addPath("/pets").addOperation(HttpMethod.GET).getResponses().addResponse("200");
        ok.setDescription("ok");
        ok.getHeaders().referTo("X-Rate", "Rate");
        return document;
    }
}
