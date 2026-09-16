package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.model.Schema;
import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public final class SchemaPane {

    private static final List<String> TYPES = List.of("object", "array", "string", "integer", "number", "boolean");

    private SchemaPane() {
    }

    public static Node build(Schema schema) {
        GridPane grid = FormFields.grid();
        int row = 0;
        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll(TYPES);
        typeBox.setValue(schema.getType());
        typeBox.valueProperty().addListener((obs, oldVal, newVal) -> schema.setType(newVal));
        grid.addRow(row++, new Label("Type"), typeBox);

        FormFields.textRow(grid, row++, "Format", schema::getFormat, schema::setFormat);
        FormFields.textAreaRow(grid, row++, "Description", schema::getDescription, schema::setDescription);
        FormFields.textRow(grid, row,
                "Required properties (comma separated)",
                () -> String.join(", ", schema.getRequired()),
                value -> schema.setRequired(value == null || value.isBlank()
                        ? List.of()
                        : List.of(value.split("\\s*,\\s*"))));

        VBox propertiesBox = new VBox(8);
        refreshProperties(schema, propertiesBox);

        TextField propertyNameField = new TextField();
        propertyNameField.setPromptText("propertyName");
        Button addPropertyButton = new Button("Add property");
        addPropertyButton.getStyleClass().add(Styles.ACCENT);
        addPropertyButton.setOnAction(e -> {
            String name = propertyNameField.getText();
            if (name != null && !name.isBlank()) {
                schema.addProperty(name.strip());
                propertyNameField.clear();
                refreshProperties(schema, propertiesBox);
            }
        });

        return FormFields.root(
                FormFields.heading("Schema"), grid,
                FormFields.heading("Properties"), propertiesBox, new HBox(8, propertyNameField, addPropertyButton));
    }

    private static void refreshProperties(Schema schema, VBox box) {
        box.getChildren().clear();
        for (String name : schema.propertyNames()) {
            Schema property = schema.getProperty(name);

            ComboBox<String> typeBox = new ComboBox<>();
            typeBox.getItems().addAll(TYPES);
            typeBox.setValue(property.getType());
            typeBox.valueProperty().addListener((obs, oldVal, newVal) -> property.setType(newVal));

            TextField formatField = new TextField(property.getFormat() == null ? "" : property.getFormat());
            formatField.setPromptText("format");
            formatField.textProperty().addListener((obs, oldVal, newVal) -> property.setFormat(newVal));

            Button removeButton = new Button("Remove");
            removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
            removeButton.setOnAction(e -> {
                schema.removeProperty(name);
                refreshProperties(schema, box);
            });

            HBox propRow = new HBox(8,
                    new Label(name), new Label("Type"), typeBox, new Label("Format"), formatField, removeButton);
            propRow.setAlignment(Pos.CENTER_LEFT);
            propRow.getStyleClass().add(Styles.BORDERED);
            box.getChildren().add(propRow);
        }
    }
}
