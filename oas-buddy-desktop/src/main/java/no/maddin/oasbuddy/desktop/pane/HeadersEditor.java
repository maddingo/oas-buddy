package no.maddin.oasbuddy.desktop.pane;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import no.maddin.oasbuddy.core.model.Header;
import no.maddin.oasbuddy.core.model.ResponseHeaders;

/**
 * The headers of a response, used by an operation's response cards and by the component response
 * editor. Each header is inline or a reference to {@code components.headers}, switched with the
 * same picker and the same "ask only when there is something to lose" rule as responses.
 *
 * <p>Nothing carries an id: header names are user data and several responses are on screen at once.
 */
final class HeadersEditor {

    static final String ROW_CLASS = "header-row";
    static final String NAME_CLASS = "header-name";
    static final String SOURCE_CLASS = "header-source";
    static final String NEW_NAME_CLASS = "new-header-name";
    static final String ADD_CLASS = "add-header";
    static final String REFERENCE_CLASS = "reference-header";
    static final String ADD_REFERENCE_CLASS = "add-header-reference";

    private HeadersEditor() {
    }

    static VBox build(ResponseHeaders headers, ComponentCatalog catalog, RemovalConfirmation confirmation) {
        VBox box = new VBox(6);
        refresh(headers, catalog, confirmation, box);
        return box;
    }

    private static void refresh(ResponseHeaders headers, ComponentCatalog catalog, RemovalConfirmation confirmation,
                                VBox box) {
        box.getChildren().clear();
        Runnable refresh = () -> refresh(headers, catalog, confirmation, box);

        for (String name : headers.names()) {
            box.getChildren().add(row(headers, name, catalog, confirmation, refresh));
        }

        TextField nameField = new TextField();
        nameField.getStyleClass().add(NEW_NAME_CLASS);
        nameField.setPromptText("header name");
        Button addButton = new Button("Add header");
        addButton.getStyleClass().addAll(Styles.SMALL, ADD_CLASS);
        Runnable add = () -> {
            String name = nameField.getText();
            if (name != null && !name.isBlank() && !headers.names().contains(name.strip())) {
                headers.add(name.strip());
                refresh.run();
            }
        };
        addButton.setOnAction(e -> add.run());
        nameField.setOnAction(e -> add.run());
        HBox addRow = new HBox(8, nameField, addButton);
        addRow.setAlignment(Pos.CENTER_LEFT);

        if (!catalog.headerNames().isEmpty()) {
            ComboBox<String> components = new ComboBox<>();
            components.getStyleClass().add(REFERENCE_CLASS);
            components.setPromptText("Reusable header");
            components.getItems().addAll(catalog.headerNames());
            Button referButton = new Button("Add reference");
            referButton.getStyleClass().addAll(Styles.SMALL, ADD_REFERENCE_CLASS);
            referButton.setOnAction(e -> {
                String component = components.getValue();
                // named after the component, since that is what a header is usually called
                if (component != null && !headers.names().contains(component)) {
                    headers.referTo(component, component);
                    refresh.run();
                }
            });
            addRow.getChildren().addAll(components, referButton);
        }
        box.getChildren().add(addRow);
    }

    private static HBox row(ResponseHeaders headers, String name, ComponentCatalog catalog,
                            RemovalConfirmation confirmation, Runnable refresh) {
        Header header = headers.get(name);

        TextField nameField = FormFields.renameField(name, candidate ->
                headers.names().contains(candidate)
                        ? "A header named \"" + candidate + "\" already exists."
                        : headers.rename(name, candidate) ? null : "");
        nameField.getStyleClass().add(NAME_CLASS);
        nameField.setPrefColumnCount(12);
        // the row is rebuilt after a rename so its buttons act on the new key
        nameField.focusedProperty().addListener((obs, was, is) -> {
            if (!is && !headers.names().contains(name)) {
                refresh.run();
            }
        });

        HBox row = new HBox(8, nameField);
        row.getStyleClass().add(ROW_CLASS);
        row.setAlignment(Pos.CENTER_LEFT);

        if (header == null) {
            Label note = FormFields.notAnObject();
            row.getChildren().add(note);
        } else if (header.isReference() && header.getReferencedHeaderName() == null) {
            // a reference this editor cannot follow: shown, not edited
            Label ref = new Label("$ref: " + header.getRef());
            ref.getStyleClass().add(Styles.TEXT_MUTED);
            row.getChildren().add(ref);
        } else {
            row.getChildren().add(sourcePicker(headers, name, header, catalog, confirmation, refresh));
            if (!header.isReference()) {
                TextField description = HeaderForm.descriptionField(header);
                HBox.setHgrow(description, Priority.ALWAYS);
                TextField type = HeaderForm.typeField(header);
                type.setPrefColumnCount(8);
                row.getChildren().addAll(description, type, HeaderForm.requiredBox(header));
            }
        }

        Button removeButton = new Button("Remove");
        removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED, Styles.SMALL);
        removeButton.setOnAction(e -> {
            headers.remove(name);
            refresh.run();
        });
        row.getChildren().add(removeButton);
        return row;
    }

    private static ComboBox<SourceChoice> sourcePicker(ResponseHeaders headers, String name, Header header,
                                                       ComponentCatalog catalog, RemovalConfirmation confirmation,
                                                       Runnable refresh) {
        ComboBox<SourceChoice> picker = new ComboBox<>();
        picker.getStyleClass().add(SOURCE_CLASS);
        SourceChoice current = header.isReference()
                ? new SourceChoice(header.getReferencedHeaderName())
                : SourceChoice.INLINE;
        picker.getItems().addAll(SourceChoice.choices(catalog.headerNames(), current));
        picker.setValue(current);

        // Reverting a declined switch would otherwise re-enter this listener.
        boolean[] reverting = {false};
        picker.valueProperty().addListener((obs, was, now) -> {
            if (reverting[0] || now == null || now.equals(was)) {
                return;
            }
            if (now.isInline()) {
                headers.defineInline(name, catalog.header(was.name()));
            } else if (!header.isReference() && !header.isEmpty() && !confirmation.confirm(
                    "Replace the inline \"" + name + "\" header with a reference to \"" + now.name() + "\"?",
                    "Its description and schema are defined only here, and will be discarded.")) {
                reverting[0] = true;
                picker.setValue(was);
                reverting[0] = false;
                return;
            } else {
                headers.referTo(name, now.name());
            }
            refresh.run();
        });
        return picker;
    }
}
