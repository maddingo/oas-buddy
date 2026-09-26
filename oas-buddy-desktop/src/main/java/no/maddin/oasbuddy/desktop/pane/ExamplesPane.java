package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.ComponentExamples;
import no.maddin.oasbuddy.core.model.Example;
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

/** The list of examples under {@code components.examples}, the counterpart to {@link SchemasPane}. */
public final class ExamplesPane {

    private ExamplesPane() {
    }

    /**
     * @param onStructureChanged run after an example is added, so the outline picks it up
     * @param onRemoveExample    asked to remove the named example; the pane only reports the
     *                           request, it never removes anything itself
     */
    public static Node build(OasDocument document, Runnable onStructureChanged, Consumer<String> onRemoveExample) {
        ComponentExamples examples = document.getComponents().getExamples();

        GridPane list = FormFields.grid();
        list.setId("examples-list");
        // the name column keeps a minimum width so Summary and Remove stay next to the names
        // instead of being pushed to the far edge of a wide window
        ColumnConstraints nameColumn = FormFields.column(HPos.LEFT, Priority.NEVER);
        nameColumn.setMinWidth(220);
        list.getColumnConstraints().addAll(nameColumn,
                FormFields.column(HPos.LEFT, Priority.NEVER),
                FormFields.column(HPos.LEFT, Priority.NEVER));
        fillList(examples, list, onRemoveExample);

        TextField nameField = new TextField();
        nameField.setId("new-example-name");
        nameField.setPromptText("Cat");
        Button addButton = new Button("Add example");
        addButton.setId("add-component-example");
        addButton.getStyleClass().add(Styles.ACCENT);
        addButton.setOnAction(e -> {
            String name = nameField.getText();
            if (name != null && !name.isBlank()) {
                examples.addExample(name.strip());
                nameField.clear();
                onStructureChanged.run();
            }
        });

        return FormFields.root(FormFields.heading("Examples"), list, new HBox(8, nameField, addButton));
    }

    private static void fillList(ComponentExamples examples, GridPane list, Consumer<String> onRemoveExample) {
        List<String> names = examples.names();
        if (names.isEmpty()) {
            Label empty = new Label("No reusable examples yet.");
            empty.getStyleClass().add(Styles.TEXT_MUTED);
            list.add(empty, 0, 0, 3, 1);
            return;
        }

        list.addRow(0, FormFields.columnHeading("Example"), FormFields.columnHeading("Summary"));

        int row = 1;
        for (String name : names) {
            Example example = examples.getExample(name);
            Label summaryLabel;
            if (example == null) {
                summaryLabel = FormFields.notAnObject();
            } else {
                summaryLabel = new Label(example.getSummary() == null ? "" : example.getSummary());
                summaryLabel.getStyleClass().add(Styles.TEXT_MUTED);
            }

            Button removeButton = new Button("Remove");
            removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
            removeButton.setOnAction(e -> onRemoveExample.accept(name));

            list.addRow(row++, new Label(name), summaryLabel, removeButton);
        }
    }
}
