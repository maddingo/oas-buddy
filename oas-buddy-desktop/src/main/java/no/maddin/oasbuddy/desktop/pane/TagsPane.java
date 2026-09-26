package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.Tag;
import no.maddin.oasbuddy.core.model.Tags;
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

import java.util.List;
import java.util.function.Consumer;

/**
 * The root {@code tags} list: name plus description, editable, with add/remove. Renaming is
 * deliberately not offered — see {@link Tag}.
 */
public final class TagsPane {

    private TagsPane() {
    }

    /**
     * @param onStructureChanged run after a tag is added, so the operation pickers pick it up
     * @param onRemoveTag        asked to remove the named tag; the pane only reports the request,
     *                           it never removes anything itself
     */
    public static Node build(OasDocument document, Runnable onStructureChanged, Consumer<String> onRemoveTag) {
        Tags tags = document.getTags();

        GridPane list = FormFields.grid();
        list.setId("tags-list");
        // the name column keeps a minimum width so Description stays readable and Remove stays
        // next to it instead of being pushed to the far edge of a wide window
        ColumnConstraints nameColumn = FormFields.column(HPos.LEFT, Priority.NEVER);
        nameColumn.setMinWidth(160);
        list.getColumnConstraints().addAll(nameColumn,
                FormFields.column(HPos.LEFT, Priority.ALWAYS),
                FormFields.column(HPos.LEFT, Priority.NEVER));
        fillList(tags, list, onRemoveTag);

        TextField nameField = new TextField();
        nameField.setId("new-tag-name");
        nameField.setPromptText("pets");
        Button addButton = new Button("Add tag");
        addButton.setId("add-tag");
        addButton.getStyleClass().add(Styles.ACCENT);
        addButton.setOnAction(e -> {
            String name = nameField.getText();
            if (name != null && !name.isBlank()) {
                tags.add(name.strip());
                nameField.clear();
                onStructureChanged.run();
            }
        });

        return FormFields.root(FormFields.heading("Tags"), list, new HBox(8, nameField, addButton));
    }

    private static void fillList(Tags tags, GridPane list, Consumer<String> onRemoveTag) {
        List<Tag> all = tags.all();
        if (all.isEmpty()) {
            Label empty = new Label("No tags yet.");
            empty.getStyleClass().add(Styles.TEXT_MUTED);
            list.add(empty, 0, 0, 3, 1);
            return;
        }

        list.addRow(0, FormFields.columnHeading("Tag"), FormFields.columnHeading("Description"));

        int row = 1;
        for (Tag tag : all) {
            String name = tag.getName();

            TextField descriptionField = new TextField(nullToEmpty(tag.getDescription()));
            descriptionField.setPromptText("Description");
            descriptionField.textProperty().addListener((obs, oldVal, newVal) -> tag.setDescription(newVal));

            Button removeButton = new Button("Remove");
            removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
            removeButton.setOnAction(e -> onRemoveTag.accept(name));

            list.addRow(row++, new Label(name), descriptionField, removeButton);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
