package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import javafx.scene.Node;

public final class ServersPane {

    private ServersPane() {
    }

    public static Node build(OasDocument document) {
        return FormFields.root(FormFields.heading("Servers"), ServersEditor.build(document.getServers(), true));
    }
}
