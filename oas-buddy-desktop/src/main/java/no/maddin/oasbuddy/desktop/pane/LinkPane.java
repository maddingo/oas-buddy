package no.maddin.oasbuddy.desktop.pane;

import javafx.scene.Node;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.Link;

import java.util.function.Consumer;

/** One link under {@code components.links}: the same fields an inline response link has. */
public final class LinkPane {

    private LinkPane() {
    }

    /**
     * @param onRemoveLink asked to remove this link; the pane only reports the request, it
     *                       never removes anything itself
     */
    public static Node build(OasDocument document, String linkName, Consumer<String> onRemoveLink) {
        Link link = document.getComponents().getLinks().getLink(linkName);
        Node title = FormFields.headerWithDelete("Link: " + linkName, "delete-link", "Delete link",
                () -> onRemoveLink.accept(linkName));
        if (link == null) {
            return FormFields.root(title, FormFields.notEditable(linkName, "a link"));
        }

        return FormFields.root(title, LinkForm.build(link, ComponentCatalog.of(document)));
    }
}
