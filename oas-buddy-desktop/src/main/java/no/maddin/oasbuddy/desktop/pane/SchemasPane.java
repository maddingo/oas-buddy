package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.Schemas;
import atlantafx.base.theme.Styles;
import javafx.geometry.HPos;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.List;
import java.util.function.Consumer;

public final class SchemasPane {

    private SchemasPane() {
    }

    /**
     * @param onStructureChanged run after a schema is added, so the outline picks it up
     * @param onRemoveSchema     asked to remove the named schema; the pane only reports the
     *                           request, it never removes anything itself
     */
    public static Node build(OasDocument document, Runnable onStructureChanged, Consumer<String> onRemoveSchema) {
        Schemas schemas = document.getComponents().getSchemas();

        GridPane list = FormFields.grid();
        list.setId("schemas-list");
        // the name column keeps a minimum width so Remove stays next to the names
        // instead of being pushed to the far edge of a wide window
        ColumnConstraints nameColumn = FormFields.column(HPos.LEFT, Priority.NEVER);
        nameColumn.setMinWidth(220);
        list.getColumnConstraints().addAll(nameColumn, FormFields.column(HPos.LEFT, Priority.NEVER));
        fillList(schemas, list, onRemoveSchema);

        TextField nameField = new TextField();
        nameField.setPromptText("SchemaName");
        Button addButton = new Button("Add schema");
        addButton.getStyleClass().add(Styles.ACCENT);
        addButton.setOnAction(e -> {
            String name = nameField.getText();
            if (name != null && !name.isBlank()) {
                schemas.addSchema(name.strip()).setType("object");
                nameField.clear();
                onStructureChanged.run();
            }
        });

        return FormFields.root(FormFields.heading("Schemas"), list, new HBox(8, nameField, addButton));
    }

    private static void fillList(Schemas schemas, GridPane list, Consumer<String> onRemoveSchema) {
        List<String> names = schemas.names();
        if (names.isEmpty()) {
            Label empty = new Label("No schemas yet.");
            empty.getStyleClass().add(Styles.TEXT_MUTED);
            list.add(empty, 0, 0, 2, 1);
            return;
        }

        list.addRow(0, FormFields.columnHeading("Schema"));

        int row = 1;
        for (String name : names) {
            Button removeButton = new Button("Remove");
            removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
            removeButton.setOnAction(e -> onRemoveSchema.accept(name));
            list.addRow(row++, new Label(name), removeButton);
        }
    }
}
