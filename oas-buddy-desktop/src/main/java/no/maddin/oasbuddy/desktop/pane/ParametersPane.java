package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.ComponentParameters;
import no.maddin.oasbuddy.core.model.Parameter;
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

import java.util.List;
import java.util.function.Consumer;

/** The list of parameters under {@code components.parameters}, the counterpart to {@link SchemasPane}. */
public final class ParametersPane {

    private ParametersPane() {
    }

    /**
     * @param onStructureChanged run after a parameter is added, so the outline picks it up
     * @param onRemoveParameter  asked to remove the parameter under that key; the pane only reports
     *                           the request, it never removes anything itself
     */
    public static Node build(OasDocument document, Runnable onStructureChanged, Consumer<String> onRemoveParameter) {
        ComponentParameters parameters = document.getComponents().getParameters();

        GridPane list = FormFields.grid();
        list.setId("parameters-list");
        // the key column keeps a minimum width so the rest stays next to the keys
        // instead of being pushed to the far edge of a wide window
        ColumnConstraints keyColumn = FormFields.column(HPos.LEFT, Priority.NEVER);
        keyColumn.setMinWidth(220);
        list.getColumnConstraints().addAll(keyColumn,
                FormFields.column(HPos.LEFT, Priority.NEVER),
                FormFields.column(HPos.LEFT, Priority.NEVER));
        fillList(parameters, list, onRemoveParameter);

        TextField keyField = new TextField();
        keyField.setId("new-parameter-name");
        keyField.setPromptText("Limit");
        Button addButton = new Button("Add parameter");
        addButton.setId("add-component-parameter");
        addButton.getStyleClass().add(Styles.ACCENT);
        addButton.setOnAction(e -> {
            String key = keyField.getText();
            if (key != null && !key.isBlank()) {
                // the key names it for references; its wire name starts out the same and can then diverge
                String trimmed = key.strip();
                parameters.addParameter(trimmed, trimmed, "query");
                keyField.clear();
                onStructureChanged.run();
            }
        });

        return FormFields.root(FormFields.heading("Parameters"), list, new HBox(8, keyField, addButton));
    }

    private static void fillList(ComponentParameters parameters, GridPane list, Consumer<String> onRemoveParameter) {
        List<String> keys = parameters.names();
        if (keys.isEmpty()) {
            Label empty = new Label("No reusable parameters yet.");
            empty.getStyleClass().add(Styles.TEXT_MUTED);
            list.add(empty, 0, 0, 3, 1);
            return;
        }

        list.addRow(0, FormFields.columnHeading("Parameter"), FormFields.columnHeading("Name and location"));

        int row = 1;
        for (String key : keys) {
            Parameter parameter = parameters.getParameter(key);
            Label nameAndLocation;
            if (parameter == null) {
                nameAndLocation = FormFields.notAnObject();
            } else {
                nameAndLocation = new Label(parameter.getName() + " in " + parameter.getIn());
                nameAndLocation.getStyleClass().add(Styles.TEXT_MUTED);
            }

            Button removeButton = new Button("Remove");
            removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
            removeButton.setOnAction(e -> onRemoveParameter.accept(key));

            list.addRow(row++, new Label(key), nameAndLocation, removeButton);
        }
    }
}
