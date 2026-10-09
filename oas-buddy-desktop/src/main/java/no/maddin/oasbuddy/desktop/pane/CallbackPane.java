package no.maddin.oasbuddy.desktop.pane;

import javafx.scene.Node;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.Callback;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** One callback under {@code components.callbacks}: the same expressions an inline operation callback has. */
public final class CallbackPane {

    private CallbackPane() {
    }

    /**
     * @param confirmation     asked before anything the nested editors replace or remove is lost
     * @param onRemoveCallback asked to remove this callback; the pane only reports the request, it
     *                         never removes anything itself
     */
    public static Node build(OasDocument document, String callbackName, Supplier<List<String>> schemaNames,
                             RemovalConfirmation confirmation, Consumer<String> onRemoveCallback) {
        Callback callback = document.getComponents().getCallbacks().getCallback(callbackName);
        Node title = FormFields.headerWithDelete("Callback: " + callbackName, "delete-callback", "Delete callback",
                () -> onRemoveCallback.accept(callbackName));
        if (callback == null) {
            return FormFields.root(title, FormFields.notEditable(callbackName, "a callback"));
        }
        return FormFields.root(title, CallbackExpressions.build(callback, ComponentCatalog.of(document), confirmation,
                OperationPane.editor(schemaNames, TagCatalog.of(document), SecuritySchemeCatalog.of(document),
                        ComponentCatalog.of(document), confirmation)));
    }
}
