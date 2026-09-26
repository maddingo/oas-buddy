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
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SchemaPane {

    static final List<String> TYPES = List.of("object", "array", "string", "integer", "number", "boolean");
    /** What an array's elements or an object's additional properties can be, besides a reference. */
    private static final List<String> ELEMENT_TYPES = List.of("string", "integer", "number", "boolean");
    private static final double TYPE_COLUMN_WIDTH = 140;
    private static final int COLUMNS = 6;

    /** The forms {@code additionalProperties} can take, as the picker offers them. */
    enum AdditionalProperties {
        UNSPECIFIED("not specified"),
        ANY("any value"),
        NONE("none"),
        TYPED("of type…");

        private final String label;

        AdditionalProperties(String label) {
            this.label = label;
        }

        static AdditionalProperties of(Schema schema) {
            if (schema.getAdditionalPropertiesSchema() != null) {
                return TYPED;
            }
            Boolean allowed = schema.getAdditionalPropertiesAllowed();
            return allowed == null ? UNSPECIFIED : allowed ? ANY : NONE;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private SchemaPane() {
    }

    /**
     * @param onRenameSchema asked to rename this schema to a new, not-yet-used name; returns
     *                       whether the rename went ahead (declined by its own confirmation, or a
     *                       collision, both leave the document untouched). The pane only reports
     *                       the request, it never renames anything itself.
     * @param onRemoveSchema asked to remove this schema; the pane only reports the request, it
     *                       never removes anything itself
     */
    public static Node build(OasDocument document, String schemaName,
                             BiFunction<String, String, Boolean> onRenameSchema,
                             Consumer<String> onRemoveSchema) {
        Schema schema = document.getComponents().getSchemas().getSchema(schemaName);
        List<String> schemaNames = document.getComponents().getSchemas().names();

        TextField nameField = FormFields.renameField(schemaName, candidate ->
                schemaNames.contains(candidate)
                        ? "A schema named \"" + candidate + "\" already exists."
                        : onRenameSchema.apply(schemaName, candidate) ? null : "");
        nameField.setId("schema-name");
        if (schema == null) {
            return FormFields.root(
                    FormFields.headerWithRenameAndDelete("Schema", nameField, "delete-schema", "Delete schema",
                            () -> onRemoveSchema.accept(schemaName)),
                    FormFields.notEditable(schemaName, "a schema"));
        }

        GridPane grid = FormFields.grid();
        int row = 0;
        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.setId("schema-type");
        typeBox.getItems().addAll(TYPES);
        typeBox.setValue(schema.getType());
        grid.addRow(row++, new Label("Type"), typeBox);
        FormFields.textRow(grid, row++, "Title", schema::getTitle, schema::setTitle);

        ComboBox<TypeChoice> itemsBox = elementTypeBox(schema::getItems, schema::createItems, schemaNames);
        itemsBox.setId("schema-items");
        Label itemsLabel = new Label("Items");
        grid.addRow(row++, itemsLabel, itemsBox);

        FormFields.textRow(grid, row++, "Format", schema::getFormat, schema::setFormat);
        FormFields.textAreaRow(grid, row++, "Description", schema::getDescription, schema::setDescription);
        FormFields.textRow(grid, row++,
                "Required properties (comma separated)",
                () -> String.join(", ", schema.getRequired()),
                value -> schema.setRequired(value == null || value.isBlank()
                        ? List.of()
                        : List.of(value.split("\\s*,\\s*"))));

        Label additionalLabel = new Label("Additional properties");
        Node additionalControls = additionalPropertiesControls(schema, schemaNames);
        grid.addRow(row, additionalLabel, additionalControls);

        SchemaConstraints constraints = new SchemaConstraints(schema);
        constraints.node().setId("schema-constraints");

        Runnable showRowsForType = () -> {
            boolean array = "array".equals(schema.getType());
            setShown(array, itemsLabel, itemsBox);
            // Without a type a schema may still be an object; only a type that rules it out hides the row.
            boolean object = schema.getType() == null || "object".equals(schema.getType());
            setShown(object, additionalLabel, additionalControls);
        };
        showRowsForType.run();
        typeBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            schema.changeTypeTo(newVal);
            if (!"array".equals(newVal)) {
                itemsBox.setValue(null);
            }
            showRowsForType.run();
            constraints.refresh();
        });

        Node values = SchemaDetails.build(schema, false);
        values.setId("schema-values");

        PropertyRows properties = new PropertyRows(schema, schemaNames, propertiesGrid(), new HashSet<>());
        properties.refresh();

        TextField propertyNameField = new TextField();
        propertyNameField.setPromptText("propertyName");
        Button addPropertyButton = new Button("Add property");
        addPropertyButton.getStyleClass().add(Styles.ACCENT);
        addPropertyButton.setOnAction(e -> {
            String name = propertyNameField.getText();
            if (name != null && !name.isBlank()) {
                schema.addProperty(name.strip());
                propertyNameField.clear();
                properties.refresh();
            }
        });

        return FormFields.root(
                FormFields.headerWithRenameAndDelete("Schema", nameField, "delete-schema", "Delete schema",
                        () -> onRemoveSchema.accept(schemaName)), grid,
                FormFields.heading("Values"), values,
                FormFields.heading("Constraints"), constraints.node(),
                FormFields.heading("Properties"), properties.grid(),
                new HBox(8, propertyNameField, addPropertyButton));
    }

    /**
     * The mode picker, plus a type picker revealed only for the schema form. Choosing that form
     * writes an empty schema straight away — which means "any value", same as {@code true} — so
     * the document never disagrees with what the picker shows while no type has been chosen yet.
     */
    private static Node additionalPropertiesControls(Schema schema, List<String> schemaNames) {
        ComboBox<AdditionalProperties> modeBox = new ComboBox<>();
        modeBox.setId("schema-additional-properties");
        modeBox.getItems().addAll(AdditionalProperties.values());
        modeBox.setValue(AdditionalProperties.of(schema));

        ComboBox<TypeChoice> typeBox = elementTypeBox(
                schema::getAdditionalPropertiesSchema, schema::createAdditionalPropertiesSchema, schemaNames);
        typeBox.setId("schema-additional-properties-type");
        setShown(modeBox.getValue() == AdditionalProperties.TYPED, typeBox);

        modeBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            switch (newVal) {
                case UNSPECIFIED -> schema.setAdditionalPropertiesAllowed(null);
                case ANY -> schema.setAdditionalPropertiesAllowed(true);
                case NONE -> schema.setAdditionalPropertiesAllowed(false);
                case TYPED -> schema.createAdditionalPropertiesSchema();
            }
            if (newVal != AdditionalProperties.TYPED) {
                typeBox.setValue(null);
            }
            setShown(newVal == AdditionalProperties.TYPED, typeBox);
        });
        return new HBox(8, modeBox, typeBox);
    }

    /**
     * A picker for a nested schema — array items, additional properties — offering the element
     * types and a reference to each declared schema. Nothing is written until a choice is made.
     */
    private static ComboBox<TypeChoice> elementTypeBox(Supplier<Schema> existing, Supplier<Schema> create,
                                                       List<String> schemaNames) {
        ComboBox<TypeChoice> box = new ComboBox<>();
        box.getItems().addAll(TypeChoice.choices(ELEMENT_TYPES, schemaNames));
        box.setPromptText("element type");
        box.setPrefWidth(TYPE_COLUMN_WIDTH);
        Schema current = existing.get();
        box.setValue(current == null ? null : TypeChoice.of(current));
        box.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                newVal.applyTo(create.get());
            }
        });
        return box;
    }

    /** Hides a row's nodes without deleting what they edit, and takes them out of the layout. */
    private static void setShown(boolean shown, Node... nodes) {
        for (Node node : nodes) {
            node.setVisible(shown);
            node.setManaged(shown);
        }
    }

    /**
     * One shared grid for all property rows, so the name/type/items/format columns line up
     * regardless of how long the individual property names are.
     */
    private static GridPane propertiesGrid() {
        GridPane grid = FormFields.grid();
        grid.setId("schema-properties");
        grid.getColumnConstraints().addAll(
                FormFields.column(HPos.LEFT, Priority.NEVER),
                FormFields.column(HPos.LEFT, Priority.NEVER),
                FormFields.column(HPos.LEFT, Priority.NEVER),
                FormFields.column(HPos.LEFT, Priority.ALWAYS),
                FormFields.column(HPos.RIGHT, Priority.NEVER),
                FormFields.column(HPos.RIGHT, Priority.NEVER));
        return grid;
    }

    /**
     * The property rows, rebuilt whenever a change decides which cells a row has. Which rows have
     * their details open is pane state, kept across rebuilds so a type change does not fold them.
     */
    private record PropertyRows(Schema schema, List<String> schemaNames, GridPane grid, Set<String> expanded) {

        void refresh() {
            grid.getChildren().clear();

            List<String> names = schema.propertyNames();
            if (names.isEmpty()) {
                Label empty = new Label("No properties yet.");
                empty.getStyleClass().add(Styles.TEXT_MUTED);
                grid.add(empty, 0, 0, COLUMNS, 1);
                return;
            }

            grid.addRow(0, FormFields.columnHeading("Property"), FormFields.columnHeading("Type"),
                    FormFields.columnHeading("Items"), FormFields.columnHeading("Format"));

            int row = 1;
            for (String name : names) {
                row = addRow(name, schema.getProperty(name), row);
            }
        }

        /** @return the next free row */
        private int addRow(String name, Schema property, int row) {
            TypeChoice choice = TypeChoice.of(property);

            ComboBox<TypeChoice> typeBox = new ComboBox<>();
            typeBox.getItems().addAll(TypeChoice.choices(TYPES, schemaNames));
            typeBox.setValue(choice);
            typeBox.setPrefWidth(TYPE_COLUMN_WIDTH);
            typeBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    newVal.applyTo(property);
                    refresh();
                }
            });
            TextField nameField = FormFields.renameField(name, candidate -> {
                if (schema.propertyNames().contains(candidate)) {
                    return "A property named \"" + candidate + "\" already exists.";
                }
                schema.renameProperty(name, candidate);
                if (expanded.remove(name)) {
                    expanded.add(candidate);
                }
                refresh();
                return null;
            });
            grid.add(nameField, 0, row);
            grid.add(typeBox, 1, row);

            if (choice != null && choice.isArray()) {
                grid.add(elementTypeBox(property::getItems, property::createItems, schemaNames), 2, row);
            }

            boolean reference = choice != null && choice.isReference();
            if (!reference) {
                TextField formatField = new TextField(property.getFormat() == null ? "" : property.getFormat());
                formatField.setPromptText("format");
                formatField.setMaxWidth(Double.MAX_VALUE);
                formatField.textProperty().addListener((obs, oldVal, newVal) -> property.setFormat(newVal));
                grid.add(formatField, 3, row);

                // OAS 3.0 ignores whatever sits beside a $ref, so a reference gets no details to edit.
                ToggleButton detailsButton = new ToggleButton("Details");
                detailsButton.setSelected(expanded.contains(name));
                detailsButton.setOnAction(e -> {
                    if (detailsButton.isSelected()) {
                        expanded.add(name);
                    } else {
                        expanded.remove(name);
                    }
                    refresh();
                });
                grid.add(detailsButton, 4, row);
            }

            Button removeButton = new Button("Remove");
            removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
            removeButton.setOnAction(e -> {
                schema.removeProperty(name);
                expanded.remove(name);
                refresh();
            });
            grid.add(removeButton, 5, row++);

            if (!reference && expanded.contains(name)) {
                // A type change rebuilds these rows, so the constraints follow the type without a refresh.
                VBox details = new VBox(10, SchemaDetails.build(property, true),
                        FormFields.columnHeading("Constraints"), new SchemaConstraints(property).node());
                details.getStyleClass().add(Styles.BORDERED);
                grid.add(details, 1, row++, COLUMNS - 1, 1);
            }
            return row;
        }
    }
}
