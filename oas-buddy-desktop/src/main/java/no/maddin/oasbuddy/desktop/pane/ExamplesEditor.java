package no.maddin.oasbuddy.desktop.pane;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import no.maddin.oasbuddy.core.model.Example;
import no.maddin.oasbuddy.core.model.MediaTypeExamples;

import java.util.function.Supplier;

/**
 * A named {@code examples} map: each entry inline or a reference to {@code components.examples}.
 * Shared by the media type editor and the parameter editor, so the two cannot drift apart.
 *
 * <p>Nothing here carries an id: example names are user data and several editors are on screen at
 * once. The style classes are the ones the media type tests already look for.
 */
final class ExamplesEditor {

    private ExamplesEditor() {
    }

    /**
     * @param type    the type a value is read as when typed (see {@code SchemaValues}), asked at the
     *                moment of entry; {@code null} where there is none to go by
     * @param refresh rebuilds whatever contains this editor, run after the list changes
     */
    static Node build(MediaTypeExamples examples, Supplier<String> type, ComponentCatalog catalog,
                      RemovalConfirmation confirmation, Runnable refresh) {
        VBox box = new VBox(6);
        for (String name : examples.names()) {
            box.getChildren().add(exampleRow(examples, type, name, catalog, confirmation, refresh));
        }

        TextField nameField = new TextField();
        nameField.getStyleClass().add("new-example-name");
        nameField.setPromptText("example name");
        Button addButton = new Button("Add example");
        addButton.getStyleClass().addAll(Styles.SMALL, "add-example");
        addButton.setOnAction(e -> {
            String name = nameField.getText();
            if (name != null && !name.isBlank() && !examples.names().contains(name.strip())) {
                examples.add(name.strip());
                refresh.run();
            }
        });
        HBox addRow = new HBox(8, nameField, addButton);
        addRow.setAlignment(Pos.CENTER_LEFT);

        if (!catalog.exampleNames().isEmpty()) {
            ComboBox<String> components = new ComboBox<>();
            components.getStyleClass().add("reference-example");
            components.setPromptText("Reusable example");
            components.getItems().addAll(catalog.exampleNames());
            Button referButton = new Button("Add reference");
            referButton.getStyleClass().addAll(Styles.SMALL, "add-example-reference");
            // the entry is named by the name field if one was typed, otherwise after the component
            referButton.setOnAction(e -> {
                String component = components.getValue();
                if (component == null) {
                    return;
                }
                String typed = nameField.getText();
                String name = typed == null || typed.isBlank() ? component : typed.strip();
                if (!examples.names().contains(name)) {
                    examples.referTo(name, component);
                    refresh.run();
                }
            });
            addRow.getChildren().addAll(components, referButton);
        }
        box.getChildren().add(addRow);
        return box;
    }

    private static Node exampleRow(MediaTypeExamples examples, Supplier<String> type, String name,
                                   ComponentCatalog catalog, RemovalConfirmation confirmation, Runnable refresh) {
        Example example = examples.get(name);

        HBox row = new HBox(8, new Label(name));
        row.getStyleClass().add("example-row");
        row.setAlignment(Pos.CENTER_LEFT);

        if (example == null || example.isReference() && example.getReferencedExampleName() == null) {
            // not an object, or a reference this editor cannot follow: shown, not edited
            Label raw = new Label(example == null ? "(not an example object)" : "$ref: " + example.getRef());
            raw.getStyleClass().add(Styles.TEXT_MUTED);
            row.getChildren().add(raw);
        } else {
            row.getChildren().add(sourcePicker(examples, name, example, catalog, confirmation, refresh));
            if (example.isReference()) {
                Example component = catalog.example(example.getReferencedExampleName());
                Label resolved = new Label(component == null ? "(not declared)"
                        : component.getSummary() == null ? "" : component.getSummary());
                resolved.getStyleClass().add(Styles.TEXT_MUTED);
                row.getChildren().add(resolved);
            } else {
                TextField summary = ExampleForm.summaryField(example);
                var value = ExampleForm.valueArea(example, type);
                value.setPrefRowCount(1);
                HBox.setHgrow(value, Priority.ALWAYS);
                row.getChildren().addAll(summary, value);
            }
        }

        Button removeButton = new Button("Remove");
        removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED, Styles.SMALL);
        removeButton.setOnAction(e -> {
            examples.remove(name);
            refresh.run();
        });
        row.getChildren().add(removeButton);
        return row;
    }

    /** The same inline/reference switch responses and parameters have; asks only when there is something to lose. */
    private static ComboBox<SourceChoice> sourcePicker(MediaTypeExamples examples, String name, Example example,
                                                       ComponentCatalog catalog, RemovalConfirmation confirmation,
                                                       Runnable refresh) {
        ComboBox<SourceChoice> picker = new ComboBox<>();
        picker.getStyleClass().add("example-source");
        SourceChoice current = example.isReference()
                ? new SourceChoice(example.getReferencedExampleName())
                : SourceChoice.INLINE;
        picker.getItems().addAll(SourceChoice.choices(catalog.exampleNames(), current));
        picker.setValue(current);

        boolean[] reverting = {false};
        picker.valueProperty().addListener((obs, was, now) -> {
            if (reverting[0] || now == null || now.equals(was)) {
                return;
            }
            if (now.isInline()) {
                examples.defineInline(name, catalog.example(was.name()));
            } else if (!example.isReference() && !example.isEmpty() && !confirmation.confirm(
                    "Replace the inline example \"" + name + "\" with a reference to \"" + now.name() + "\"?",
                    "Its summary and value are defined only here, and will be discarded.")) {
                reverting[0] = true;
                picker.setValue(was);
                reverting[0] = false;
                return;
            } else {
                examples.referTo(name, now.name());
            }
            refresh.run();
        });
        return picker;
    }
}
