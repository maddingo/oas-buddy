package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.SecuritySchemeUsages;

import java.util.List;

/**
 * Removing a security scheme, shared by the scheme list and the individual scheme editor.
 *
 * <p>The counterpart to {@link SchemaRemoval}: a scheme is named by {@code security} requirements
 * rather than by {@code $ref}, so the dialog lists those instead of references. As with schemas,
 * the requirements are left dangling for the validation panel to report rather than rewritten.
 */
public final class SecuritySchemeRemoval {

    private SecuritySchemeRemoval() {
    }

    public static void remove(OasDocument document, String schemeName,
                              RemovalConfirmation confirmation, Runnable onRemoved) {
        List<String> usages = SecuritySchemeUsages.find(document, schemeName);
        if (!confirmation.confirm("Remove security scheme \"" + schemeName + "\"?", describeUsages(usages))) {
            return;
        }
        document.getComponents().getSecuritySchemes().removeScheme(schemeName);
        onRemoved.run();
    }

    /** The dialog's body text: which requirements the removal would leave dangling. */
    static String describeUsages(List<String> usages) {
        if (usages.isEmpty()) {
            return "Nothing requires it.";
        }
        return "Still required by:\n  " + String.join("\n  ", usages)
                + "\n\nThose requirements will be left dangling.";
    }
}
