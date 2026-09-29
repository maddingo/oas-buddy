package no.maddin.oasbuddy.desktop.pane;

import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import no.maddin.oasbuddy.core.model.ExternalDocs;

/**
 * The two-field {@code externalDocs} section, deliberately the same component at the document
 * root, on a tag, an operation and a schema.
 *
 * <p>It is collapsed unless there is already something to show. Its fields carry style classes
 * rather than ids because a tag list puts one on screen per tag. Clearing both fields removes the
 * key — that is {@link ExternalDocs}' job, not the pane's.
 */
final class ExternalDocsSection {

    static final String URL_CLASS = "external-docs-url";
    static final String DESCRIPTION_CLASS = "external-docs-description";

    private ExternalDocsSection() {
    }

    static Node build(ExternalDocs docs) {
        if (!docs.isEditable()) {
            TitledPane note = new TitledPane("External docs",
                    FormFields.notEditable("externalDocs", "an"));
            note.setExpanded(true);
            return note;
        }

        GridPane grid = FormFields.grid();
        TextField url = FormFields.textRow(grid, 0, "Url", docs::getUrl, docs::setUrl);
        url.getStyleClass().add(URL_CLASS);
        url.setPromptText("https://example.com/docs");
        TextField description = FormFields.textRow(grid, 1, "Description", docs::getDescription, docs::setDescription);
        description.getStyleClass().add(DESCRIPTION_CLASS);

        TitledPane section = new TitledPane("External docs", grid);
        section.setExpanded(docs.getUrl() != null || docs.getDescription() != null);
        return section;
    }
}
