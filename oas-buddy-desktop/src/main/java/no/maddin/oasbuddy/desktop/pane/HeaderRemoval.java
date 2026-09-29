package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.ComponentHeaders;
import no.maddin.oasbuddy.core.model.ComponentReferences;

import java.util.List;

/**
 * Removing a component header, the same bargain as {@link ExampleRemoval}: the responses referring
 * to it are listed and their references are left dangling for the validation panel to report.
 */
public final class HeaderRemoval {

    private HeaderRemoval() {
    }

    public static void remove(OasDocument document, String headerName,
                              RemovalConfirmation confirmation, Runnable onRemoved) {
        List<String> usages = ComponentReferences.find(document, ComponentHeaders.SECTION, headerName);
        if (!confirmation.confirm("Remove header \"" + headerName + "\"?", SchemaRemoval.describeUsages(usages))) {
            return;
        }
        document.getComponents().getHeaders().removeHeader(headerName);
        onRemoved.run();
    }
}
