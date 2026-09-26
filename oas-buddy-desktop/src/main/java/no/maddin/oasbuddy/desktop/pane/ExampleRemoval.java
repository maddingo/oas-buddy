package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.ComponentExamples;
import no.maddin.oasbuddy.core.model.ComponentReferences;

import java.util.List;

/**
 * Removing a component example, shared by the example list and the individual example editor. The
 * same bargain as {@link SchemaRemoval}: the media types referring to it are listed, and their
 * references are left dangling for the validation panel to report.
 */
public final class ExampleRemoval {

    private ExampleRemoval() {
    }

    public static void remove(OasDocument document, String exampleName,
                              RemovalConfirmation confirmation, Runnable onRemoved) {
        List<String> usages = ComponentReferences.find(document, ComponentExamples.SECTION, exampleName);
        if (!confirmation.confirm("Remove example \"" + exampleName + "\"?", SchemaRemoval.describeUsages(usages))) {
            return;
        }
        document.getComponents().getExamples().removeExample(exampleName);
        onRemoved.run();
    }
}
