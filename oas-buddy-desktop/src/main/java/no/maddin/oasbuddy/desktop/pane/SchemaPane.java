package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.Schema;
import atlantafx.base.theme.Styles;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.List;
import java.util.function.Consumer;

public final class SchemaPane {

    private static final List<String> TYPES = List.of("object", "array", "string", "integer", "number", "boolean");
    private static final double TYPE_COLUMN_WIDTH = 140;

    private SchemaPane() {
    }

    /**
     * @param onRemoveSchema asked to remove this schema; the pane only reports the request, it
     *                       never removes anything itself
     */
    public static Node build(OasDocument document, String schemaName, Consumer<String> onRemoveSchema) {
        Schema schema = document.getComponents().getSchemas().getSchema(schemaName);

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

        GridPane propertiesGrid = propertiesGrid();
        refreshProperties(schema, propertiesGrid);

        TextField propertyNameField = new TextField();
        propertyNameField.setPromptText("propertyName");
        Button addPropertyButton = new Button("Add property");
        addPropertyButton.getStyleClass().add(Styles.ACCENT);
        addPropertyButton.setOnAction(e -> {
            String name = propertyNameField.getText();
            if (name != null && !name.isBlank()) {
                schema.addProperty(name.strip());
                propertyNameField.clear();
                refreshProperties(schema, propertiesGrid);
            }
        });

        return FormFields.root(
                FormFields.headerWithDelete("Schema", "delete-schema", "Delete schema",
                        () -> onRemoveSchema.accept(schemaName)), grid,
                FormFields.heading("Properties"), propertiesGrid,
                new HBox(8, propertyNameField, addPropertyButton));
    }

    /**
     * One shared grid for all property rows, so the name/type/format/remove columns line up
     * regardless of how long the individual property names are.
     */
    private static GridPane propertiesGrid() {
        GridPane grid = FormFields.grid();
        grid.setId("schema-properties");
        grid.getColumnConstraints().addAll(
                FormFields.column(HPos.LEFT, Priority.NEVER),
                FormFields.column(HPos.LEFT, Priority.NEVER),
                FormFields.column(HPos.LEFT, Priority.ALWAYS),
                FormFields.column(HPos.RIGHT, Priority.NEVER));
        return grid;
    }

    private static void refreshProperties(Schema schema, GridPane grid) {
        grid.getChildren().clear();

        List<String> names = schema.propertyNames();
        if (names.isEmpty()) {
            Label empty = new Label("No properties yet.");
            empty.getStyleClass().add(Styles.TEXT_MUTED);
            grid.add(empty, 0, 0, 4, 1);
            return;
        }

        grid.addRow(0, FormFields.columnHeading("Property"), FormFields.columnHeading("Type"),
                FormFields.columnHeading("Format"));

        int row = 1;
        for (String name : names) {
            Schema property = schema.getProperty(name);

            ComboBox<String> typeBox = new ComboBox<>();
            typeBox.getItems().addAll(TYPES);
            typeBox.setValue(property.getType());
            typeBox.setPrefWidth(TYPE_COLUMN_WIDTH);
            typeBox.valueProperty().addListener((obs, oldVal, newVal) -> property.setType(newVal));

            TextField formatField = new TextField(property.getFormat() == null ? "" : property.getFormat());
            formatField.setPromptText("format");
            formatField.setMaxWidth(Double.MAX_VALUE);
            formatField.textProperty().addListener((obs, oldVal, newVal) -> property.setFormat(newVal));

            Button removeButton = new Button("Remove");
            removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
            removeButton.setOnAction(e -> {
                schema.removeProperty(name);
                refreshProperties(schema, grid);
            });

            grid.addRow(row++, new Label(name), typeBox, formatField, removeButton);
        }
    }

}
