package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.Parameter;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.util.function.Consumer;

/** One parameter under {@code components.parameters}: the same fields an inline parameter has. */
public final class ParameterPane {

    private ParameterPane() {
    }

    /**
     * @param onRemoveParameter asked to remove this parameter; the pane only reports the request, it
     *                          never removes anything itself
     */
    public static Node build(OasDocument document, String key, Consumer<String> onRemoveParameter) {
        Parameter parameter = document.getComponents().getParameters().getParameter(key);
        Node header = FormFields.headerWithDelete("Parameter: " + key, "delete-parameter", "Delete parameter",
                () -> onRemoveParameter.accept(key));
        if (parameter == null) {
            return FormFields.root(header, FormFields.notEditable(key, "a parameter"));
        }

        GridPane grid = FormFields.grid();
        int row = 0;
        grid.addRow(row++, new Label("Name"), ParameterForm.nameField(parameter));
        grid.addRow(row++, new Label("In"), ParameterForm.inBox(parameter));
        grid.addRow(row++, new Label(""), ParameterForm.requiredBox(parameter));
        grid.addRow(row++, new Label("Type"), ParameterForm.typeField(parameter));
        FormFields.textAreaRow(grid, row, "Description", parameter::getDescription, parameter::setDescription);

        return FormFields.root(header, grid);
    }
}
