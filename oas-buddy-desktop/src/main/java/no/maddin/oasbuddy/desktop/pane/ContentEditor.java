package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.model.Content;
import no.maddin.oasbuddy.core.model.Example;
import no.maddin.oasbuddy.core.model.MediaType;
import no.maddin.oasbuddy.core.model.MediaTypeExamples;
import no.maddin.oasbuddy.core.model.Schema;
import no.maddin.oasbuddy.core.model.SchemaValues;
import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Supplier;

/**
 * A request body's or response's {@code content}: one section per media type, each with its
 * schema, a single {@code example} and named {@code examples}, and a way to add further media
 * types. Replaces the fixed "Schema (application/json)" row the editor had before.
 *
 * <p>Nothing here carries an id: a media type and an example name are user data, and several of
 * these editors can be on screen at once (one per response). Controls carry style classes instead,
 * and tests find a section by its media-type field's text.
 */
final class ContentEditor {

    /** What the add picker suggests; any other media type can be typed. */
    static final List<String> COMMON_MEDIA_TYPES = List.of(
            "application/json", "application/xml", "application/x-www-form-urlencoded",
            "multipart/form-data", "text/plain", "application/octet-stream", "*/*");

    private ContentEditor() {
    }

    /**
     * @param confirmation asked before an inline named example is replaced by a reference
     */
    static VBox build(Content content, Supplier<List<String>> schemaNames, ComponentCatalog catalog,
                      RemovalConfirmation confirmation) {
        VBox box = new VBox(8);
        box.getStyleClass().add("content-editor");
        fill(content, schemaNames, catalog, confirmation, box);
        return box;
    }

    private static void fill(Content content, Supplier<List<String>> schemaNames, ComponentCatalog catalog,
                             RemovalConfirmation confirmation, VBox box) {
        box.getChildren().clear();
        Runnable refresh = () -> fill(content, schemaNames, catalog, confirmation, box);

        for (String mediaType : content.mediaTypes()) {
            box.getChildren().add(section(content, mediaType, schemaNames, catalog, confirmation, refresh));
        }
        if (content.mediaTypes().isEmpty()) {
            Label empty = new Label("No content.");
            empty.getStyleClass().add(Styles.TEXT_MUTED);
            box.getChildren().add(empty);
        }

        ComboBox<String> newMediaType = new ComboBox<>();
        newMediaType.getStyleClass().add("new-media-type");
        newMediaType.setEditable(true);
        newMediaType.setPromptText("media type");
        newMediaType.getItems().addAll(COMMON_MEDIA_TYPES.stream()
                .filter(type -> !content.mediaTypes().contains(type)).toList());
        Button addButton = new Button("Add media type");
        addButton.getStyleClass().addAll(Styles.SMALL, "add-media-type");
        addButton.setOnAction(e -> {
            // an editable combo's typed text reaches its value only on commit; read the editor directly
            String typed = newMediaType.getEditor().getText();
            if (typed != null && !typed.isBlank() && !content.mediaTypes().contains(typed.strip())) {
                content.add(typed.strip());
                refresh.run();
            }
        });
        HBox addRow = new HBox(8, newMediaType, addButton);
        addRow.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(addRow);
    }

    private static Node section(Content content, String mediaType, Supplier<List<String>> schemaNames,
                                ComponentCatalog catalog, RemovalConfirmation confirmation, Runnable refresh) {
        MediaType media = content.get(mediaType);

        TextField nameField = FormFields.renameField(mediaType, candidate -> {
            if (content.mediaTypes().contains(candidate)) {
                return "\"" + candidate + "\" is already listed.";
            }
            content.rename(mediaType, candidate);
            refresh.run();
            return null;
        });
        nameField.getStyleClass().add("media-type-name");

        Button removeButton = new Button("Remove");
        removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED, Styles.SMALL);
        removeButton.setOnAction(e -> {
            content.remove(mediaType);
            refresh.run();
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(8, nameField, spacer, removeButton);
        header.setAlignment(Pos.CENTER_LEFT);

        GridPane grid = FormFields.grid();
        grid.addRow(0, new Label("Schema"), schemaPicker(media, schemaNames));
        grid.addRow(1, new Label("Example"), exampleField(media));
        grid.addRow(2, new Label("Examples"), examples(media, catalog, confirmation, refresh));

        VBox section = new VBox(8, header, grid);
        section.getStyleClass().addAll("media-type", Styles.BORDERED);
        section.setStyle("-fx-padding: 8;");
        return section;
    }

    /**
     * The same one-choice picker a schema property has: a JSON type or {@code → Name}. Reads without
     * creating, so a media type without a schema shows an empty picker rather than growing one.
     */
    private static ComboBox<TypeChoice> schemaPicker(MediaType media, Supplier<List<String>> schemaNames) {
        ComboBox<TypeChoice> picker = new ComboBox<>();
        picker.getStyleClass().add("media-type-schema");
        picker.getItems().addAll(TypeChoice.choices(SchemaPane.TYPES, schemaNames.get()));
        Schema existing = media.findSchema();
        picker.setValue(existing == null ? null : TypeChoice.of(existing));
        picker.valueProperty().addListener((obs, was, now) -> {
            if (now == null) {
                media.removeSchema();
            } else {
                now.applyTo(media.getSchema());
            }
        });
        return picker;
    }

    private static TextField exampleField(MediaType media) {
        TextField field = new TextField(SchemaValues.display(media.getExample()));
        field.getStyleClass().add("media-type-example");
        field.setPromptText("typed as the schema's type");
        field.textProperty().addListener((obs, oldVal, newVal) ->
                media.setExample(SchemaValues.parse(newVal, typeOf(media))));
        return field;
    }

    private static String typeOf(MediaType media) {
        Schema schema = media.findSchema();
        return schema == null ? null : schema.getType();
    }

    private static Node examples(MediaType media, ComponentCatalog catalog, RemovalConfirmation confirmation,
                                 Runnable refresh) {
        MediaTypeExamples examples = media.getExamples();
        VBox box = new VBox(6);
        for (String name : examples.names()) {
            box.getChildren().add(exampleRow(media, name, catalog, confirmation, refresh));
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

    private static Node exampleRow(MediaType media, String name, ComponentCatalog catalog,
                                   RemovalConfirmation confirmation, Runnable refresh) {
        MediaTypeExamples examples = media.getExamples();
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
                var value = ExampleForm.valueArea(example, () -> typeOf(media));
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
