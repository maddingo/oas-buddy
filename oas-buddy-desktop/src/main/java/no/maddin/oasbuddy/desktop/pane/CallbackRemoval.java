package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.ComponentCallbacks;
import no.maddin.oasbuddy.core.model.ComponentReferences;

import java.util.List;

/**
 * Removing a component callback, the same bargain as {@link ExampleRemoval}: the operations referring
 * to it are listed and their references are left dangling for the validation panel to report.
 */
public final class CallbackRemoval {

    private CallbackRemoval() {
    }

    public static void remove(OasDocument document, String callbackName,
                              RemovalConfirmation confirmation, Runnable onRemoved) {
        List<String> usages = ComponentReferences.find(document, ComponentCallbacks.SECTION, callbackName);
        if (!confirmation.confirm("Remove callback \"" + callbackName + "\"?", SchemaRemoval.describeUsages(usages))) {
            return;
        }
        document.getComponents().getCallbacks().removeCallback(callbackName);
        onRemoved.run();
    }
}
