package no.maddin.oasbuddy.desktop.pane;

import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import no.maddin.oasbuddy.core.model.Server;
import no.maddin.oasbuddy.core.model.ServerVariable;
import no.maddin.oasbuddy.core.model.ServerVariables;
import no.maddin.oasbuddy.core.model.Servers;

import java.util.Arrays;
import java.util.List;

/**
 * The one editor for a {@code servers} list, used for the document, for a path item and for an
 * operation. Each server gets its URL, description and a variables section; a URL that templates a
 * name no variable defines is flagged, with a button that declares the missing ones.
 *
 * <p>Nothing here carries an id (several servers are on screen at once and variable names are user
 * data): controls are found by style class or by their own text.
 */
final class ServersEditor {

    static final String URL_CLASS = "server-url";
    static final String UNDEFINED_NOTE_CLASS = "server-undefined-variables";
    static final String VARIABLE_NAME_CLASS = "server-variable-name";
    static final String VARIABLE_DEFAULT_CLASS = "server-variable-default";
    static final String VARIABLE_ENUM_CLASS = "server-variable-enum";

    private ServersEditor() {
    }

    /**
     * @param undeclareWhenEmpty at document level absent and empty mean the same, so removing the
     *                           last server also removes the key; an override keeps its empty array
     */
    static VBox build(Servers servers, boolean undeclareWhenEmpty) {
        VBox rows = new VBox(14);
        refresh(servers, rows, undeclareWhenEmpty);

        Button addButton = new Button("Add server");
        addButton.getStyleClass().add(Styles.ACCENT);
        addButton.setOnAction(e -> {
            servers.add("https://");
            refresh(servers, rows, undeclareWhenEmpty);
        });
        return new VBox(8, rows, addButton);
    }

    private static void refresh(Servers servers, VBox rows, boolean undeclareWhenEmpty) {
        rows.getChildren().clear();
        for (Server server : servers.all()) {
            rows.getChildren().add(serverBlock(server, () -> {
                servers.remove(server);
                if (undeclareWhenEmpty && servers.all().isEmpty()) {
                    servers.undeclare();
                }
                refresh(servers, rows, undeclareWhenEmpty);
            }));
        }
    }

    private static Node serverBlock(Server server, Runnable onRemove) {
        TextField urlField = new TextField(nullToEmpty(server.getUrl()));
        urlField.getStyleClass().add(URL_CLASS);
        urlField.setPromptText("URL");
        HBox.setHgrow(urlField, Priority.SOMETIMES);

        TextField descriptionField = new TextField(nullToEmpty(server.getDescription()));
        descriptionField.setPromptText("Description");
        descriptionField.textProperty().addListener((obs, oldVal, newVal) -> server.setDescription(newVal));
        HBox.setHgrow(descriptionField, Priority.ALWAYS);

        Button removeButton = new Button("Remove");
        removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
        removeButton.setOnAction(e -> onRemove.run());

        HBox row = new HBox(8, new Label("URL"), urlField, new Label("Description"), descriptionField, removeButton);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox variablesBox = new VBox(6);
        Label undefined = new Label();
        undefined.getStyleClass().addAll(UNDEFINED_NOTE_CLASS, Styles.WARNING);
        Button declareMissing = new Button("Declare them");
        declareMissing.getStyleClass().addAll(Styles.SMALL, Styles.BUTTON_OUTLINED);
        HBox undefinedRow = new HBox(8, undefined, declareMissing);
        undefinedRow.setAlignment(Pos.CENTER_LEFT);

        Runnable refreshUndefined = () -> {
            List<String> missing = server.undefinedVariables();
            undefined.setText("The URL uses {" + String.join("}, {", missing) + "} but no variable defines "
                    + (missing.size() == 1 ? "it." : "them."));
            undefinedRow.setManaged(!missing.isEmpty());
            undefinedRow.setVisible(!missing.isEmpty());
        };
        Runnable[] rebuildVariables = new Runnable[1];
        rebuildVariables[0] = () -> {
            fillVariables(server, variablesBox, () -> {
                rebuildVariables[0].run();
                refreshUndefined.run();
            });
            refreshUndefined.run();
        };
        declareMissing.setOnAction(e -> {
            server.undefinedVariables().forEach(name -> server.getVariables().add(name));
            rebuildVariables[0].run();
        });
        urlField.textProperty().addListener((obs, oldVal, newVal) -> {
            server.setUrl(newVal);
            refreshUndefined.run();
        });
        rebuildVariables[0].run();

        return new VBox(6, row, undefinedRow, variablesBox);
    }

    private static void fillVariables(Server server, VBox box, Runnable onChanged) {
        box.getChildren().clear();
        ServerVariables variables = server.getVariables();
        Label title = new Label("Variables");
        title.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        box.getChildren().add(title);

        for (String name : variables.names()) {
            ServerVariable variable = variables.get(name);
            if (variable == null) {
                Label note = FormFields.notAnObject();
                box.getChildren().add(new HBox(8, new Label(name), note, removeVariable(variables, name, onChanged)));
                continue;
            }
            TextField nameField = FormFields.renameField(name, candidate ->
                    variables.names().contains(candidate)
                            ? "A variable named \"" + candidate + "\" already exists."
                            : variables.rename(name, candidate) ? null : "");
            nameField.getStyleClass().add(VARIABLE_NAME_CLASS);
            nameField.setPrefColumnCount(10);
            nameField.focusedProperty().addListener((obs, was, is) -> {
                if (!is) {
                    onChanged.run();
                }
            });

            TextField defaultField = new TextField(nullToEmpty(variable.getDefault()));
            defaultField.getStyleClass().add(VARIABLE_DEFAULT_CLASS);
            defaultField.setPromptText("default");
            defaultField.setPrefColumnCount(10);
            defaultField.textProperty().addListener((obs, oldVal, newVal) -> variable.setDefault(newVal));

            TextField enumField = new TextField(String.join(", ", variable.getEnum()));
            enumField.getStyleClass().add(VARIABLE_ENUM_CLASS);
            enumField.setPromptText("allowed values, comma separated");
            enumField.textProperty().addListener((obs, oldVal, newVal) -> variable.setEnum(
                    newVal == null || newVal.isBlank() ? List.of()
                            : Arrays.stream(newVal.split("\\s*,\\s*")).filter(v -> !v.isEmpty()).toList()));
            HBox.setHgrow(enumField, Priority.ALWAYS);

            TextField descriptionField = new TextField(nullToEmpty(variable.getDescription()));
            descriptionField.setPromptText("description");
            descriptionField.textProperty().addListener((obs, oldVal, newVal) ->
                    variable.setDescription(newVal == null || newVal.isBlank() ? null : newVal));
            HBox.setHgrow(descriptionField, Priority.ALWAYS);

            HBox row = new HBox(8, nameField, defaultField, enumField, descriptionField,
                    removeVariable(variables, name, onChanged));
            row.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().add(row);
        }

        TextField newName = new TextField();
        newName.setPromptText("variable name");
        Button add = new Button("Add variable");
        add.getStyleClass().addAll(Styles.SMALL, Styles.BUTTON_OUTLINED);
        Runnable addVariable = () -> {
            String name = newName.getText();
            if (name != null && !name.isBlank()) {
                variables.add(name.strip());
                onChanged.run();
            }
        };
        add.setOnAction(e -> addVariable.run());
        newName.setOnAction(e -> addVariable.run());
        HBox addRow = new HBox(8, newName, add);
        addRow.setAlignment(Pos.CENTER_LEFT);
        box.getChildren().add(addRow);
    }

    private static Button removeVariable(ServerVariables variables, String name, Runnable onChanged) {
        Button remove = new Button("Remove");
        remove.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED, Styles.SMALL);
        remove.setOnAction(e -> {
            variables.remove(name);
            onChanged.run();
        });
        return remove;
    }

    /**
     * A path or operation's override: a checkbox that declares or drops the {@code servers} key,
     * and the editor beneath it while it is declared. Unchecking asks first, but only when there
     * are servers to lose — an empty override goes silently, like an empty oauth2 flow. Declining
     * puts the box back as it was.
     */
    static VBox override(Servers servers, RemovalConfirmation confirmation, String subject) {
        CheckBox overrideBox = new CheckBox("Override the document's servers for this " + subject);
        overrideBox.getStyleClass().add("servers-override");
        overrideBox.setSelected(servers.isDeclared());

        VBox editorSlot = new VBox();
        Runnable showEditor = () -> {
            editorSlot.getChildren().clear();
            if (servers.isDeclared()) {
                editorSlot.getChildren().add(build(servers, false));
            }
        };
        showEditor.run();

        boolean[] reverting = {false};
        overrideBox.selectedProperty().addListener((obs, was, now) -> {
            if (reverting[0]) {
                return;
            }
            if (now) {
                servers.declare();
            } else {
                int count = servers.all().size();
                if (count > 0 && !confirmation.confirm(
                        "Stop overriding the document's servers?",
                        "The " + count + " server" + (count == 1 ? "" : "s") + " declared here will be discarded.")) {
                    reverting[0] = true;
                    overrideBox.setSelected(true);
                    reverting[0] = false;
                    return;
                }
                servers.undeclare();
            }
            showEditor.run();
        });
        return new VBox(8, overrideBox, editorSlot);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
