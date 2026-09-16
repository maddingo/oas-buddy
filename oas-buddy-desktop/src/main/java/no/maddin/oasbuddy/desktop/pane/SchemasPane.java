package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.model.Schemas;
import atlantafx.base.theme.Styles;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public final class SchemasPane {

    private SchemasPane() {
    }

    public static Node build(Schemas schemas, Runnable onStructureChanged) {
        Label existing = new Label(schemas.names().isEmpty()
                ? "No schemas yet."
                : "Existing schemas: " + String.join(", ", schemas.names()));
        existing.getStyleClass().add(Styles.TEXT_MUTED);

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

        return FormFields.root(FormFields.heading("Schemas"), existing, new HBox(8, nameField, addButton));
    }
}
