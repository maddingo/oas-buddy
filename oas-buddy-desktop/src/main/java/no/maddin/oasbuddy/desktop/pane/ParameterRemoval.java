package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.ComponentParameters;
import no.maddin.oasbuddy.core.model.ComponentReferences;

import java.util.List;

/**
 * Removing a component parameter, shared by the parameter list and the individual parameter
 * editor. The same bargain as {@link SchemaRemoval}: the operations and paths referring to it are
 * listed, and their references are left dangling for the validation panel to report.
 */
public final class ParameterRemoval {

    private ParameterRemoval() {
    }

    public static void remove(OasDocument document, String key,
                              RemovalConfirmation confirmation, Runnable onRemoved) {
        List<String> usages = ComponentReferences.find(document, ComponentParameters.SECTION, key);
        if (!confirmation.confirm("Remove parameter \"" + key + "\"?", SchemaRemoval.describeUsages(usages))) {
            return;
        }
        document.getComponents().getParameters().removeParameter(key);
        onRemoved.run();
    }
}
