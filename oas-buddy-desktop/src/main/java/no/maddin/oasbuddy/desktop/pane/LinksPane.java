package no.maddin.oasbuddy.desktop.pane;

import atlantafx.base.theme.Styles;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.ComponentLinks;
import no.maddin.oasbuddy.core.model.Link;

import java.util.List;
import java.util.function.Consumer;

/** The list of links under {@code components.links}, the counterpart to {@link ExamplesPane}. */
public final class LinksPane {

    private LinksPane() {
    }

    /**
     * @param onStructureChanged run after a link is added, so the outline picks it up
     * @param onRemoveLink     asked to remove the named link; the pane only reports the
     *                           request, it never removes anything itself
     */
    public static Node build(OasDocument document, Runnable onStructureChanged, Consumer<String> onRemoveLink) {
        ComponentLinks links = document.getComponents().getLinks();

        GridPane list = FormFields.grid();
        list.setId("links-list");
        ColumnConstraints nameColumn = FormFields.column(HPos.LEFT, Priority.NEVER);
        nameColumn.setMinWidth(220);
        list.getColumnConstraints().addAll(nameColumn,
                FormFields.column(HPos.LEFT, Priority.NEVER),
                FormFields.column(HPos.LEFT, Priority.NEVER));
        fillList(links, list, onRemoveLink);

        TextField nameField = new TextField();
        nameField.setId("new-link-name");
        nameField.setPromptText("GetPet");
        Button addButton = new Button("Add link");
        addButton.setId("add-component-link");
        addButton.getStyleClass().add(Styles.ACCENT);
        addButton.setOnAction(e -> {
            String name = nameField.getText();
            if (name != null && !name.isBlank()) {
                links.addLink(name.strip());
                nameField.clear();
                onStructureChanged.run();
            }
        });

        return FormFields.root(FormFields.heading("Links"), list, new HBox(8, nameField, addButton));
    }

    private static void fillList(ComponentLinks links, GridPane list, Consumer<String> onRemoveLink) {
        List<String> names = links.names();
        if (names.isEmpty()) {
            Label empty = new Label("No reusable links yet.");
            empty.getStyleClass().add(Styles.TEXT_MUTED);
            list.add(empty, 0, 0, 3, 1);
            return;
        }

        list.addRow(0, FormFields.columnHeading("Link"), FormFields.columnHeading("Description"));

        int row = 1;
        for (String name : names) {
            Link link = links.getLink(name);
            Label description;
            if (link == null) {
                description = FormFields.notAnObject();
            } else {
                description = new Label(link.getDescription() == null ? "" : link.getDescription());
                description.getStyleClass().add(Styles.TEXT_MUTED);
            }

            Button removeButton = new Button("Remove");
            removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
            removeButton.setOnAction(e -> onRemoveLink.accept(name));

            list.addRow(row++, new Label(name), description, removeButton);
        }
    }
}
