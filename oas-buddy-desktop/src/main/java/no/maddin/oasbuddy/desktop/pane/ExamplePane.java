package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.Example;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.function.Consumer;

/**
 * One example under {@code components.examples}. Its value has no schema to be typed against —
 * it may be used under several — so it is read as JSON when it is valid JSON, and as text otherwise.
 */
public final class ExamplePane {

    private ExamplePane() {
    }

    /**
     * @param onRemoveExample asked to remove this example; the pane only reports the request, it
     *                        never removes anything itself
     */
    public static Node build(OasDocument document, String exampleName, Consumer<String> onRemoveExample) {
        Example example = document.getComponents().getExamples().getExample(exampleName);
        Node header = FormFields.headerWithDelete("Example: " + exampleName, "delete-example", "Delete example",
                () -> onRemoveExample.accept(exampleName));
        if (example == null) {
            return FormFields.root(header, FormFields.notEditable(exampleName, "an example"));
        }

        GridPane grid = FormFields.grid();
        int row = 0;
        TextField summary = ExampleForm.summaryField(example);
        summary.setId("example-summary");
        grid.addRow(row++, new Label("Summary"), summary);
        FormFields.textAreaRow(grid, row++, "Description", example::getDescription, example::setDescription);
        TextArea value = ExampleForm.valueArea(example, () -> null);
        value.setId("example-value");
        value.setPrefRowCount(6);
        grid.addRow(row++, new Label("Value"), value);
        TextField external = ExampleForm.externalValueField(example);
        external.setId("example-external-value");
        grid.addRow(row, new Label("External value"), external);

        return FormFields.root(header, grid);
    }
}
