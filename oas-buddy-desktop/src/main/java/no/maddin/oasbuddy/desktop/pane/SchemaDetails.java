package no.maddin.oasbuddy.desktop.pane;

import atlantafx.base.theme.Styles;
import com.fasterxml.jackson.databind.JsonNode;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import no.maddin.oasbuddy.core.model.Schema;
import no.maddin.oasbuddy.core.model.SchemaValues;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The value-level and descriptive fields of a schema — enum, default, example and the four
 * boolean flags — shared by the schema pane and each property's details row.
 *
 * <p>The controls are marked with style classes rather than ids: a pane can show several of these
 * editors at once, one per expanded property, and a property name must never become part of a node
 * id (see the scope-name gotcha in CLAUDE.md).
 */
final class SchemaDetails {

    static final String DEFAULT = "schema-default";
    static final String EXAMPLE = "schema-example";
    static final String ENUM = "schema-enum";
    static final String ENUM_INPUT = "schema-enum-input";

    private static final double ENUM_LIST_HEIGHT = 120;

    private SchemaDetails() {
    }

    /**
     * @param withTitleAndDescription whether to include those two — the schema pane already shows
     *                                them in its main form, a property's details row does not
     */
    static Node build(Schema schema, boolean withTitleAndDescription) {
        GridPane grid = FormFields.grid();
        int row = 0;
        if (withTitleAndDescription) {
            FormFields.textRow(grid, row++, "Title", schema::getTitle, schema::setTitle);
            FormFields.textAreaRow(grid, row++, "Description", schema::getDescription, schema::setDescription);
        }
        valueRow(grid, row++, "Default", DEFAULT, schema, schema::getDefault, schema::setDefault);
        valueRow(grid, row++, "Example", EXAMPLE, schema, schema::getExample, schema::setExample);
        grid.addRow(row++, new Label("Enum"), enumEditor(schema));
        grid.addRow(row, new Label("Flags"), flags(schema));
        return grid;
    }

    /** A value typed by the schema's type at the moment it is entered; see {@link SchemaValues}. */
    private static void valueRow(GridPane grid, int row, String label, String styleClass, Schema schema,
                                 Supplier<JsonNode> getter, Consumer<JsonNode> setter) {
        TextField field = new TextField(SchemaValues.display(getter.get()));
        field.getStyleClass().add(styleClass);
        field.setPromptText("typed as the schema's type");
        field.textProperty().addListener((obs, oldVal, newVal) ->
                setter.accept(SchemaValues.parse(newVal, schema.getType())));
        grid.addRow(row, new Label(label), field);
    }

    private static Node enumEditor(Schema schema) {
        ListView<JsonNode> values = new ListView<>();
        values.getStyleClass().add(ENUM);
        values.setPrefHeight(ENUM_LIST_HEIGHT);
        values.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(JsonNode item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : SchemaValues.display(item));
            }
        });
        values.getItems().setAll(schema.getEnum());
        Runnable write = () -> schema.setEnum(new ArrayList<>(values.getItems()));

        TextField input = new TextField();
        input.getStyleClass().add(ENUM_INPUT);
        input.setPromptText("value");
        Runnable add = () -> {
            JsonNode value = SchemaValues.parse(input.getText(), schema.getType());
            if (value != null) {
                values.getItems().add(value);
                input.clear();
                write.run();
            }
        };
        input.setOnAction(e -> add.run());

        Button addButton = button("Add", add);
        Button upButton = button("Up", () -> move(values, -1, write));
        Button downButton = button("Down", () -> move(values, 1, write));
        Button removeButton = button("Remove", () -> {
            int index = values.getSelectionModel().getSelectedIndex();
            if (index >= 0) {
                values.getItems().remove(index);
                write.run();
            }
        });
        Button clearButton = button("Clear", () -> {
            values.getItems().clear();
            write.run();
        });
        removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
        clearButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);

        return new VBox(8, values,
                new HBox(8, input, addButton, upButton, downButton, removeButton, clearButton));
    }

    private static void move(ListView<JsonNode> values, int offset, Runnable write) {
        int index = values.getSelectionModel().getSelectedIndex();
        int target = index + offset;
        if (index < 0 || target < 0 || target >= values.getItems().size()) {
            return;
        }
        List<JsonNode> items = values.getItems();
        items.add(target, items.remove(index));
        values.getSelectionModel().select(target);
        write.run();
    }

    private static Node flags(Schema schema) {
        return new HBox(16,
                flag("Nullable", schema::isNullable, schema::setNullable),
                flag("Read only", schema::isReadOnly, schema::setReadOnly),
                flag("Write only", schema::isWriteOnly, schema::setWriteOnly),
                flag("Deprecated", schema::isDeprecated, schema::setDeprecated));
    }

    private static CheckBox flag(String label, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        CheckBox box = new CheckBox(label);
        box.setSelected(getter.get());
        box.selectedProperty().addListener((obs, oldVal, newVal) -> setter.accept(newVal));
        return box;
    }

    private static Button button(String text, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add("schema-enum-" + text.toLowerCase());
        button.setOnAction(e -> action.run());
        return button;
    }
}
