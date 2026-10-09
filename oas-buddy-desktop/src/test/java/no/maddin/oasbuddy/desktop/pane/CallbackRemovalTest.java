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
class CallbackRemovalTest {

    @Test
    void listsTheResponsesReferringToTheCallback() {
        OasDocument document = documentWithEvent();
        List<String> asked = new ArrayList<>();

        CallbackRemoval.remove(document, "Event", (question, details) -> {
            asked.add(question + " | " + details);
            return false;
        }, () -> { });

        assertEquals(List.of("Remove callback \"Event\"? | Still referenced by:\n"
                + "  paths → /pets → post → callbacks → onEvent\n"
                + "\n"
                + "Those references will be left dangling."), asked);
    }

    @Test
    void removesTheCallbackWhenConfirmed() {
        OasDocument document = documentWithEvent();
        AtomicBoolean notified = new AtomicBoolean();

        CallbackRemoval.remove(document, "Event", (question, details) -> true, () -> notified.set(true));

        assertFalse(document.getComponents().getCallbacks().names().contains("Event"));
        assertTrue(notified.get());
    }

    @Test
    void keepsTheCallbackWhenDeclined() {
        OasDocument document = documentWithEvent();

        CallbackRemoval.remove(document, "Event", (question, details) -> false, () -> { });

        assertTrue(document.getComponents().getCallbacks().names().contains("Event"));
    }

    private static OasDocument documentWithEvent() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getComponents().getCallbacks().addCallback("Event").add("{$request.body#/url}");
        document.getPaths().addPath("/pets").addOperation(HttpMethod.POST)
                .getCallbacks().referTo("onEvent", "Event");
        return document;
    }
}
