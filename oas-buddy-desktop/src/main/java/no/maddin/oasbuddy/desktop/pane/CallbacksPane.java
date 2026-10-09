package no.maddin.oasbuddy.desktop.pane;

import atlantafx.base.theme.Styles;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.ComponentCallbacks;
import no.maddin.oasbuddy.core.model.Callback;

import java.util.List;
import java.util.function.Consumer;

/** The list of callbacks under {@code components.callbacks}, the counterpart to {@link ExamplesPane}. */
public final class CallbacksPane {

    private CallbacksPane() {
    }

    /**
     * @param onStructureChanged run after a callback is added, so the outline picks it up
     * @param onRemoveCallback     asked to remove the named callback; the pane only reports the
     *                           request, it never removes anything itself
     */
    public static Node build(OasDocument document, Runnable onStructureChanged, Consumer<String> onRemoveCallback) {
        ComponentCallbacks callbacks = document.getComponents().getCallbacks();

        GridPane list = FormFields.grid();
        list.setId("callbacks-list");
        ColumnConstraints nameColumn = FormFields.column(HPos.LEFT, Priority.NEVER);
        nameColumn.setMinWidth(220);
        list.getColumnConstraints().addAll(nameColumn,
                FormFields.column(HPos.LEFT, Priority.NEVER),
                FormFields.column(HPos.LEFT, Priority.NEVER));
        fillList(callbacks, list, onRemoveCallback);

        TextField nameField = new TextField();
        nameField.setId("new-callback-name");
        nameField.setPromptText("onEvent");
        Button addButton = new Button("Add callback");
        addButton.setId("add-component-callback");
        addButton.getStyleClass().add(Styles.ACCENT);
        addButton.setOnAction(e -> {
            String name = nameField.getText();
            if (name != null && !name.isBlank()) {
                callbacks.addCallback(name.strip());
                nameField.clear();
                onStructureChanged.run();
            }
        });

        return FormFields.root(FormFields.heading("Callbacks"), list, new HBox(8, nameField, addButton));
    }

    private static void fillList(ComponentCallbacks callbacks, GridPane list, Consumer<String> onRemoveCallback) {
        List<String> names = callbacks.names();
        if (names.isEmpty()) {
            Label empty = new Label("No reusable callbacks yet.");
            empty.getStyleClass().add(Styles.TEXT_MUTED);
            list.add(empty, 0, 0, 3, 1);
            return;
        }

        list.addRow(0, FormFields.columnHeading("Callback"), FormFields.columnHeading("Expressions"));

        int row = 1;
        for (String name : names) {
            Callback callback = callbacks.getCallback(name);
            Label description;
            if (callback == null) {
                description = FormFields.notAnObject();
            } else {
                description = new Label(callback.isReference() ? "$ref: " + callback.getRef()
                        : String.join(", ", callback.expressions()));
                description.getStyleClass().add(Styles.TEXT_MUTED);
            }

            Button removeButton = new Button("Remove");
            removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
            removeButton.setOnAction(e -> onRemoveCallback.accept(name));

            list.addRow(row++, new Label(name), description, removeButton);
        }
    }
}
