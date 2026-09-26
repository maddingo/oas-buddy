package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.ComponentReferences;
import no.maddin.oasbuddy.core.model.ComponentResponses;

import java.util.List;

/**
 * Removing a component response, shared by the response list and the individual response editor.
 * The same bargain as {@link SchemaRemoval}: the operations referring to it are listed, and their
 * references are left dangling for the validation panel to report rather than rewritten.
 */
public final class ResponseRemoval {

    private ResponseRemoval() {
    }

    public static void remove(OasDocument document, String responseName,
                              RemovalConfirmation confirmation, Runnable onRemoved) {
        List<String> usages = ComponentReferences.find(document, ComponentResponses.SECTION, responseName);
        if (!confirmation.confirm("Remove response \"" + responseName + "\"?",
                SchemaRemoval.describeUsages(usages))) {
            return;
        }
        document.getComponents().getResponses().removeResponse(responseName);
        onRemoved.run();
    }
}
