package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.SchemaReferences;

import java.util.List;

/**
 * Removing a component schema, shared by the schema list and the individual schema editor.
 */
public final class SchemaRemoval {

    private SchemaRemoval() {
    }

    public static void remove(OasDocument document, String schemaName,
                              RemovalConfirmation confirmation, Runnable onRemoved) {
        List<String> usages = SchemaReferences.find(document, schemaName);
        if (!confirmation.confirm("Remove schema \"" + schemaName + "\"?", describeUsages(usages))) {
            return;
        }
        document.getComponents().getSchemas().removeSchema(schemaName);
        onRemoved.run();
    }

    /** The dialog's body text: which references the removal would leave dangling. */
    static String describeUsages(List<String> usages) {
        if (usages.isEmpty()) {
            return "Nothing references it.";
        }
        return "Still referenced by:\n  " + String.join("\n  ", usages)
                + "\n\nThose references will be left dangling.";
    }
}
