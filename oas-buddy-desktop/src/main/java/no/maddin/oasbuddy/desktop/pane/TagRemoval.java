package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.TagUsages;

import java.util.List;

/**
 * Removing a root-level tag, shared by the tag list and the operation picker's own remove button.
 *
 * <p>The counterpart to {@link SchemaRemoval} / {@link SecuritySchemeRemoval}: a tag is named from
 * an operation's {@code tags} array rather than by {@code $ref} or a {@code security} requirement,
 * so the dialog lists the operations still using it. Those operations are left tagged with the now
 * undefined name for the validation panel to report, the same bargain schema {@code $ref}s and
 * security requirements get.
 */
public final class TagRemoval {

    private TagRemoval() {
    }

    public static void remove(OasDocument document, String tagName,
                              RemovalConfirmation confirmation, Runnable onRemoved) {
        List<String> usages = TagUsages.find(document, tagName);
        if (!confirmation.confirm("Remove tag \"" + tagName + "\"?", describeUsages(usages))) {
            return;
        }
        document.getTags().remove(tagName);
        onRemoved.run();
    }

    /** The dialog's body text: which operations the removal would leave using an undefined tag. */
    static String describeUsages(List<String> usages) {
        if (usages.isEmpty()) {
            return "No operation uses it.";
        }
        return "Still used by:\n  " + String.join("\n  ", usages)
                + "\n\nThose operations will be left using an undefined tag.";
    }
}
