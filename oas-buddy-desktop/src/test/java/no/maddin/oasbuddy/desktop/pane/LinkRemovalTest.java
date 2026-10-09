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
class LinkRemovalTest {

    @Test
    void listsTheResponsesReferringToTheLink() {
        OasDocument document = documentWithGetPet();
        List<String> asked = new ArrayList<>();

        LinkRemoval.remove(document, "GetPet", (question, details) -> {
            asked.add(question + " | " + details);
            return false;
        }, () -> { });

        assertEquals(List.of("Remove link \"GetPet\"? | Still referenced by:\n"
                + "  paths → /pets → get → responses → 200 → links → pet\n"
                + "\n"
                + "Those references will be left dangling."), asked);
    }

    @Test
    void removesTheLinkWhenConfirmed() {
        OasDocument document = documentWithGetPet();
        AtomicBoolean notified = new AtomicBoolean();

        LinkRemoval.remove(document, "GetPet", (question, details) -> true, () -> notified.set(true));

        assertFalse(document.getComponents().getLinks().names().contains("GetPet"));
        assertTrue(notified.get());
    }

    @Test
    void keepsTheLinkWhenDeclined() {
        OasDocument document = documentWithGetPet();

        LinkRemoval.remove(document, "GetPet", (question, details) -> false, () -> { });

        assertTrue(document.getComponents().getLinks().names().contains("GetPet"));
    }

    private static OasDocument documentWithGetPet() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getComponents().getLinks().addLink("GetPet").setOperationId("getPet");
        var ok = document.getPaths().addPath("/pets").addOperation(HttpMethod.GET).getResponses().addResponse("200");
        ok.setDescription("ok");
        ok.getLinks().referTo("pet", "GetPet");
        return document;
    }
}
