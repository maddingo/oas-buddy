package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.model.Schemas;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class SchemasPane {

    private SchemasPane() {
    }

    public static Node build(Schemas schemas, Runnable onStructureChanged) {
        VBox root = new VBox(8);
        root.setPadding(new Insets(12));

        TextField nameField = new TextField();
        nameField.setPromptText("SchemaName");
        Button addButton = new Button("Add schema");
        addButton.setOnAction(e -> {
            String name = nameField.getText();
            if (name != null && !name.isBlank()) {
                schemas.addSchema(name.strip()).setType("object");
                nameField.clear();
                onStructureChanged.run();
            }
        });

        root.getChildren().addAll(
                new Label("Existing schemas: " + String.join(", ", schemas.names())),
                new HBox(6, nameField, addButton));
        return root;
    }
}
