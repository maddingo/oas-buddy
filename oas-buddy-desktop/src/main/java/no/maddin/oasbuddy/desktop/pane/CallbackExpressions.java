package no.maddin.oasbuddy.desktop.pane;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import no.maddin.oasbuddy.core.model.Callback;
import no.maddin.oasbuddy.core.model.PathItem;

/**
 * The runtime-expression keys of one inline callback, each carrying a full path item. The path item
 * is edited by {@link PathItemPane#embedded} — the same fields as a top-level path, with its
 * operations edited in place by the same operation editor.
 *
 * <p>Nothing carries an id: an expression is user data ({@code {$request.body#/url}} would not even
 * survive a CSS selector) and several callbacks are on screen at once.
 */
final class CallbackExpressions {

    static final String CARD_CLASS = "callback-expression";
    static final String NAME_CLASS = "expression-name";
    static final String REMOVE_CLASS = "remove-expression";
    static final String NEW_NAME_CLASS = "new-expression-name";
    static final String ADD_CLASS = "add-expression";

    private CallbackExpressions() {
    }

    static VBox build(Callback callback, ComponentCatalog catalog, RemovalConfirmation confirmation,
                      OperationEditor operationEditor) {
        VBox box = new VBox(8);
        refresh(callback, catalog, confirmation, operationEditor, box);
        return box;
    }

    private static void refresh(Callback callback, ComponentCatalog catalog, RemovalConfirmation confirmation,
                                OperationEditor operationEditor, VBox box) {
        box.getChildren().clear();
        Runnable refresh = () -> refresh(callback, catalog, confirmation, operationEditor, box);

        for (String expression : callback.expressions()) {
            box.getChildren().add(card(callback, expression, catalog, confirmation, operationEditor, refresh));
        }

        TextField nameField = new TextField();
        nameField.getStyleClass().add(NEW_NAME_CLASS);
        nameField.setPromptText("{$request.body#/callbackUrl}");
        nameField.setPrefColumnCount(24);
        Button addButton = new Button("Add expression");
        addButton.getStyleClass().addAll(Styles.SMALL, ADD_CLASS);
        Runnable add = () -> {
            String name = nameField.getText();
            if (name != null && !name.isBlank() && !callback.expressions().contains(name.strip())
                    && callback.get(name.strip()) == null) {
                callback.add(name.strip());
                refresh.run();
            }
        };
        addButton.setOnAction(e -> add.run());
        nameField.setOnAction(e -> add.run());
        HBox addRow = new HBox(8, nameField, addButton);
        addRow.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(addRow);
    }

    private static VBox card(Callback callback, String expression, ComponentCatalog catalog,
                             RemovalConfirmation confirmation, OperationEditor operationEditor, Runnable refresh) {
        PathItem pathItem = callback.get(expression);

        TextField nameField = FormFields.renameField(expression, candidate ->
                callback.expressions().contains(candidate)
                        ? "An expression \"" + candidate + "\" already exists."
                        : callback.rename(expression, candidate) ? null : "");
        nameField.getStyleClass().add(NAME_CLASS);
        HBox.setHgrow(nameField, Priority.ALWAYS);
        // the card is rebuilt after a rename so its controls act on the new key
        nameField.focusedProperty().addListener((obs, was, is) -> {
            if (!is && !callback.expressions().contains(expression)) {
                refresh.run();
            }
        });

        Button removeButton = new Button("Remove");
        removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED, Styles.SMALL, REMOVE_CLASS);
        removeButton.setOnAction(e -> {
            // a path item with no operations has nothing to lose; one that is not an object likewise
            if (pathItem == null || pathItem.getOperations().isEmpty()
                    || confirmation.confirm("Remove expression \"" + expression + "\"?",
                            PathRemoval.describeOperations(pathItem))) {
                callback.remove(expression);
                refresh.run();
            }
        });
        HBox header = new HBox(8, nameField, removeButton);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(8, header);
        card.getStyleClass().addAll(Styles.BORDERED, CARD_CLASS);
        card.setStyle("-fx-padding: 8;");
        card.getChildren().add(pathItem == null
                ? FormFields.notAnObject()
                : PathItemPane.embedded(pathItem, catalog, confirmation, operationEditor, refresh));
        return card;
    }
}
