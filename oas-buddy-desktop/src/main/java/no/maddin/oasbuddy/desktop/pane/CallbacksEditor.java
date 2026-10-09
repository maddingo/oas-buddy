package no.maddin.oasbuddy.desktop.pane;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import no.maddin.oasbuddy.core.model.Callback;
import no.maddin.oasbuddy.core.model.Callbacks;

/**
 * The callbacks of a response, used by an operation's response cards and by the component response
 * editor. Each callback is inline or a reference to {@code components.callbacks}, switched with the
 * same picker and the same "ask only when there is something to lose" rule as responses.
 *
 * <p>Nothing carries an id: callback names are user data and several responses are on screen at once.
 */
final class CallbacksEditor {

    static final String CARD_CLASS = "callback-card";
    static final String ROW_CLASS = "callback-row";
    static final String NAME_CLASS = "callback-name";
    static final String SOURCE_CLASS = "callback-source";
    static final String NEW_NAME_CLASS = "new-callback-name";
    static final String ADD_CLASS = "add-callback";
    static final String REFERENCE_CLASS = "reference-callback";
    static final String ADD_REFERENCE_CLASS = "add-callback-reference";

    private CallbacksEditor() {
    }

    static VBox build(Callbacks callbacks, ComponentCatalog catalog, RemovalConfirmation confirmation,
                      OperationEditor operationEditor) {
        VBox box = new VBox(6);
        refresh(callbacks, catalog, confirmation, operationEditor, box);
        return box;
    }

    private static void refresh(Callbacks callbacks, ComponentCatalog catalog, RemovalConfirmation confirmation,
                                OperationEditor operationEditor, VBox box) {
        box.getChildren().clear();
        Runnable refresh = () -> refresh(callbacks, catalog, confirmation, operationEditor, box);

        for (String name : callbacks.names()) {
            box.getChildren().add(card(callbacks, name, catalog, confirmation, operationEditor, refresh));
        }

        TextField nameField = new TextField();
        nameField.getStyleClass().add(NEW_NAME_CLASS);
        nameField.setPromptText("callback name");
        Button addButton = new Button("Add callback");
        addButton.getStyleClass().addAll(Styles.SMALL, ADD_CLASS);
        Runnable add = () -> {
            String name = nameField.getText();
            if (name != null && !name.isBlank() && !callbacks.names().contains(name.strip())) {
                callbacks.add(name.strip());
                refresh.run();
            }
        };
        addButton.setOnAction(e -> add.run());
        nameField.setOnAction(e -> add.run());
        HBox addRow = new HBox(8, nameField, addButton);
        addRow.setAlignment(Pos.CENTER_LEFT);

        if (!catalog.callbackNames().isEmpty()) {
            ComboBox<String> components = new ComboBox<>();
            components.getStyleClass().add(REFERENCE_CLASS);
            components.setPromptText("Reusable callback");
            components.getItems().addAll(catalog.callbackNames());
            Button referButton = new Button("Add reference");
            referButton.getStyleClass().addAll(Styles.SMALL, ADD_REFERENCE_CLASS);
            referButton.setOnAction(e -> {
                String component = components.getValue();
                // named after the component, since that is what a callback is usually called
                if (component != null && !callbacks.names().contains(component)) {
                    callbacks.referTo(component, component);
                    refresh.run();
                }
            });
            addRow.getChildren().addAll(components, referButton);
        }
        box.getChildren().add(addRow);
    }

    private static VBox card(Callbacks callbacks, String name, ComponentCatalog catalog,
                             RemovalConfirmation confirmation, OperationEditor operationEditor, Runnable refresh) {
        Callback callback = callbacks.get(name);

        TextField nameField = FormFields.renameField(name, candidate ->
                callbacks.names().contains(candidate)
                        ? "A callback named \"" + candidate + "\" already exists."
                        : callbacks.rename(name, candidate) ? null : "");
        nameField.getStyleClass().add(NAME_CLASS);
        nameField.setPrefColumnCount(12);
        // the row is rebuilt after a rename so its buttons act on the new key
        nameField.focusedProperty().addListener((obs, was, is) -> {
            if (!is && !callbacks.names().contains(name)) {
                refresh.run();
            }
        });

        Node expressions = null;
        HBox row = new HBox(8, nameField);
        row.getStyleClass().add(ROW_CLASS);
        row.setAlignment(Pos.CENTER_LEFT);

        if (callback == null) {
            Label note = FormFields.notAnObject();
            row.getChildren().add(note);
        } else if (callback.isReference() && callback.getReferencedCallbackName() == null) {
            // a reference this editor cannot follow: shown, not edited
            Label ref = new Label("$ref: " + callback.getRef());
            ref.getStyleClass().add(Styles.TEXT_MUTED);
            row.getChildren().add(ref);
        } else {
            row.getChildren().add(sourcePicker(callbacks, name, callback, catalog, confirmation, refresh));
            if (!callback.isReference()) {
                expressions = CallbackExpressions.build(callback, catalog, confirmation, operationEditor);
            }
        }

        Button removeButton = new Button("Remove");
        removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED, Styles.SMALL);
        removeButton.setOnAction(e -> {
            // an inline callback may hold whole operations; a reference or an empty one loses nothing
            if (callback != null && !callback.isReference() && !callback.isEmpty()
                    && !confirmation.confirm("Remove callback \"" + name + "\"?",
                            "Its expressions and the operations under them are defined only here.")) {
                return;
            }
            callbacks.remove(name);
            refresh.run();
        });
        row.getChildren().add(removeButton);
        VBox card = new VBox(8, row);
        card.getStyleClass().add(CARD_CLASS);
        if (expressions != null) {
            card.getChildren().add(expressions);
        }
        return card;
    }

    private static ComboBox<SourceChoice> sourcePicker(Callbacks callbacks, String name, Callback callback,
                                                       ComponentCatalog catalog, RemovalConfirmation confirmation,
                                                       Runnable refresh) {
        ComboBox<SourceChoice> picker = new ComboBox<>();
        picker.getStyleClass().add(SOURCE_CLASS);
        SourceChoice current = callback.isReference()
                ? new SourceChoice(callback.getReferencedCallbackName())
                : SourceChoice.INLINE;
        picker.getItems().addAll(SourceChoice.choices(catalog.callbackNames(), current));
        picker.setValue(current);

        // Reverting a declined switch would otherwise re-enter this listener.
        boolean[] reverting = {false};
        picker.valueProperty().addListener((obs, was, now) -> {
            if (reverting[0] || now == null || now.equals(was)) {
                return;
            }
            if (now.isInline()) {
                callbacks.defineInline(name, catalog.callback(was.name()));
            } else if (!callback.isReference() && !callback.isEmpty() && !confirmation.confirm(
                    "Replace the inline \"" + name + "\" callback with a reference to \"" + now.name() + "\"?",
                    "Its expressions and the operations under them are defined only here, and will be discarded.")) {
                reverting[0] = true;
                picker.setValue(was);
                reverting[0] = false;
                return;
            } else {
                callbacks.referTo(name, now.name());
            }
            refresh.run();
        });
        return picker;
    }
}
