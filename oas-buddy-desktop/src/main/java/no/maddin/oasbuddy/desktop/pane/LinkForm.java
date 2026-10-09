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
import no.maddin.oasbuddy.core.model.Link;
import no.maddin.oasbuddy.core.model.LinkParameters;
import no.maddin.oasbuddy.core.model.SchemaValues;

/**
 * The fields of an inline link, shared by a response's link rows and the component link editor,
 * the way {@link HeaderForm} is for headers. None carries an id: several are on screen at once and
 * every name in it is user data.
 *
 * <p>The operation picker offers the operation ids the document declares but accepts any text — a
 * link to a missing operation is for validation to report, not for the form to refuse.
 */
final class LinkForm {

    static final String OPERATION_ID_CLASS = "link-operation-id";
    static final String OPERATION_REF_CLASS = "link-operation-ref";
    static final String DESCRIPTION_CLASS = "link-description";
    static final String REQUEST_BODY_CLASS = "link-request-body";
    static final String PARAMETER_NAME_CLASS = "link-parameter-name";
    static final String PARAMETER_VALUE_CLASS = "link-parameter-value";
    static final String NEW_PARAMETER_CLASS = "new-link-parameter";
    static final String ADD_PARAMETER_CLASS = "add-link-parameter";

    private LinkForm() {
    }

    static VBox build(Link link, ComponentCatalog catalog) {
        VBox box = new VBox(6);
        refresh(link, catalog, box);
        return box;
    }

    private static void refresh(Link link, ComponentCatalog catalog, VBox box) {
        box.getChildren().clear();
        box.getChildren().add(labelled("Operation", operationIdBox(link, catalog)));
        box.getChildren().add(labelled("Operation ref", textField(link.getOperationRef(), OPERATION_REF_CLASS,
                "#/paths/~1pets/get", link::setOperationRef)));
        box.getChildren().add(labelled("Description", textField(link.getDescription(), DESCRIPTION_CLASS,
                "description", link::setDescription)));
        box.getChildren().add(labelled("Request body", requestBodyField(link)));

        Label parametersTitle = new Label("Parameters");
        parametersTitle.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        box.getChildren().add(parametersTitle);
        LinkParameters parameters = link.getParameters();
        for (String name : parameters.names()) {
            box.getChildren().add(parameterRow(parameters, name, () -> refresh(link, catalog, box)));
        }
        box.getChildren().add(newParameterRow(parameters, () -> refresh(link, catalog, box)));
    }

    /**
     * An editable combo of the document's operation ids. Typed text only reaches {@code getValue()} on
     * commit, so the editor's text is listened to directly, as {@link ContentEditor}'s media type box is.
     */
    private static ComboBox<String> operationIdBox(Link link, ComponentCatalog catalog) {
        ComboBox<String> box = new ComboBox<>();
        box.getStyleClass().add(OPERATION_ID_CLASS);
        box.setEditable(true);
        box.setPromptText("operationId");
        box.getItems().addAll(catalog.operationIds());
        box.getEditor().setText(nullToEmpty(link.getOperationId()));
        box.getEditor().textProperty().addListener((obs, was, now) ->
                link.setOperationId(now == null || now.isBlank() ? null : now.strip()));
        return box;
    }

    /** Read as JSON when it is, else as a literal string, so {@code $request.body#/id} stays a string. */
    private static TextField requestBodyField(Link link) {
        TextField field = new TextField(SchemaValues.display(link.getRequestBody()));
        field.getStyleClass().add(REQUEST_BODY_CLASS);
        field.setPromptText("$request.body or a value");
        field.textProperty().addListener((obs, was, now) -> link.setRequestBody(SchemaValues.parse(now, null)));
        return field;
    }

    private static HBox parameterRow(LinkParameters parameters, String name, Runnable refresh) {
        TextField nameField = FormFields.renameField(name, candidate ->
                parameters.names().contains(candidate)
                        ? "A parameter named \"" + candidate + "\" already exists."
                        : parameters.rename(name, candidate) ? null : "");
        nameField.getStyleClass().add(PARAMETER_NAME_CLASS);
        nameField.setPrefColumnCount(10);
        // the row is rebuilt after a rename so its field acts on the new key
        nameField.focusedProperty().addListener((obs, was, is) -> {
            if (!is && !parameters.names().contains(name)) {
                refresh.run();
            }
        });

        TextField valueField = new TextField(SchemaValues.display(parameters.get(name)));
        valueField.getStyleClass().add(PARAMETER_VALUE_CLASS);
        valueField.setPromptText("$response.body#/id");
        HBox.setHgrow(valueField, Priority.ALWAYS);
        valueField.textProperty().addListener((obs, was, now) -> {
            var value = SchemaValues.parse(now, null);
            parameters.put(name, value == null ? com.fasterxml.jackson.databind.node.TextNode.valueOf("") : value);
        });

        Button remove = new Button("Remove");
        remove.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED, Styles.SMALL);
        remove.setOnAction(e -> {
            parameters.remove(name);
            refresh.run();
        });
        HBox row = new HBox(8, nameField, valueField, remove);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static HBox newParameterRow(LinkParameters parameters, Runnable refresh) {
        TextField nameField = new TextField();
        nameField.getStyleClass().add(NEW_PARAMETER_CLASS);
        nameField.setPromptText("parameter name");
        Button add = new Button("Add parameter");
        add.getStyleClass().addAll(Styles.SMALL, ADD_PARAMETER_CLASS);
        Runnable addParameter = () -> {
            String name = nameField.getText();
            if (name != null && !name.isBlank() && !parameters.names().contains(name.strip())) {
                parameters.put(name.strip(), com.fasterxml.jackson.databind.node.TextNode.valueOf(""));
                refresh.run();
            }
        };
        add.setOnAction(e -> addParameter.run());
        nameField.setOnAction(e -> addParameter.run());
        HBox row = new HBox(8, nameField, add);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static TextField textField(String initial, String styleClass, String prompt,
                                       java.util.function.Consumer<String> setter) {
        TextField field = new TextField(nullToEmpty(initial));
        field.getStyleClass().add(styleClass);
        field.setPromptText(prompt);
        field.textProperty().addListener((obs, was, now) -> setter.accept(now == null || now.isBlank() ? null : now));
        return field;
    }

    private static HBox labelled(String label, javafx.scene.Node control) {
        Label text = new Label(label);
        text.setMinWidth(100);
        HBox.setHgrow(control, Priority.ALWAYS);
        HBox row = new HBox(8, text, control);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
