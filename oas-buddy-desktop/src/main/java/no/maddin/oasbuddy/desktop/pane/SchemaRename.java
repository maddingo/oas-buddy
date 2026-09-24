package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.SchemaReferences;

import java.util.List;

/**
 * Renaming a component schema. Unlike a path or a property, a schema can be {@code $ref}erenced
 * from anywhere else in the document, so this composes two core primitives that stay separate on
 * purpose — {@code Schemas.renameSchema} (the position-preserving key rename) and
 * {@code SchemaReferences.rewrite} (fixing up every {@code $ref} that pointed at the old name) —
 * because deciding *whether* to go ahead is a UI concern (the confirmation below) that a
 * non-interactive caller of core would not want forced on it.
 */
public final class SchemaRename {

    private SchemaRename() {
    }

    /**
     * Asks first only when something is actually at stake: a schema nothing refers to is renamed
     * immediately, one with references is confirmed first, and the confirmation says how many
     * {@code $ref}s will be rewritten to match — the same "don't ask about nothing" rule as an
     * empty oauth2 flow (see {@code OAuthFlow}) or an unused security scheme.
     *
     * @return whether the rename went ahead
     */
    public static boolean rename(OasDocument document, String from, String to,
                                 RemovalConfirmation confirmation, Runnable onRenamed) {
        if (from.equals(to)) {
            return false;
        }
        List<String> usages = SchemaReferences.find(document, from);
        if (!usages.isEmpty() && !confirmation.confirm(question(from, to), describe(usages))) {
            return false;
        }
        if (!document.getComponents().getSchemas().renameSchema(from, to)) {
            return false;
        }
        if (!usages.isEmpty()) {
            SchemaReferences.rewrite(document, from, to);
        }
        onRenamed.run();
        return true;
    }

    static String question(String from, String to) {
        return "Rename schema \"" + from + "\" to \"" + to + "\"?";
    }

    /** The dialog's body text: how many references will be updated, and where they are. */
    static String describe(List<String> usages) {
        String noun = usages.size() == 1 ? "reference" : "references";
        return usages.size() + " " + noun + " will be updated:\n  " + String.join("\n  ", usages);
    }
}
