package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.ComponentLinks;
import no.maddin.oasbuddy.core.model.ComponentReferences;

import java.util.List;

/**
 * Removing a component link, the same bargain as {@link ExampleRemoval}: the responses referring
 * to it are listed and their references are left dangling for the validation panel to report.
 */
public final class LinkRemoval {

    private LinkRemoval() {
    }

    public static void remove(OasDocument document, String linkName,
                              RemovalConfirmation confirmation, Runnable onRemoved) {
        List<String> usages = ComponentReferences.find(document, ComponentLinks.SECTION, linkName);
        if (!confirmation.confirm("Remove link \"" + linkName + "\"?", SchemaRemoval.describeUsages(usages))) {
            return;
        }
        document.getComponents().getLinks().removeLink(linkName);
        onRemoved.run();
    }
}
