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

/** The removal flow with the confirmation stubbed out, as in {@link SchemaRemovalTest}. */
class ExampleRemovalTest {

    @Test
    void listsTheMediaTypesReferringToTheExample() {
        OasDocument document = documentWithCat();
        List<String> asked = new ArrayList<>();

        ExampleRemoval.remove(document, "Cat", (question, details) -> {
            asked.add(question + " | " + details);
            return false;
        }, () -> { });

        assertEquals(List.of("Remove example \"Cat\"? | Still referenced by:\n"
                + "  paths → /pets → get → responses → 200 → content → application/json → examples → cat\n"
                + "\n"
                + "Those references will be left dangling."), asked);
    }

    @Test
    void removesTheExampleWhenConfirmed() {
        OasDocument document = documentWithCat();
        AtomicBoolean notified = new AtomicBoolean();

        ExampleRemoval.remove(document, "Cat", (question, details) -> true, () -> notified.set(true));

        assertFalse(document.getComponents().getExamples().names().contains("Cat"));
        assertTrue(notified.get());
    }

    @Test
    void keepsTheExampleWhenDeclined() {
        OasDocument document = documentWithCat();

        ExampleRemoval.remove(document, "Cat", (question, details) -> false, () -> { });

        assertTrue(document.getComponents().getExamples().names().contains("Cat"));
    }

    private static OasDocument documentWithCat() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getComponents().getExamples().addExample("Cat").setSummary("A cat");
        var ok = document.getPaths().addPath("/pets").addOperation(HttpMethod.GET).getResponses().addResponse("200");
        ok.setDescription("ok");
        ok.getContent().add("application/json").getExamples().referTo("cat", "Cat");
        return document;
    }
}
