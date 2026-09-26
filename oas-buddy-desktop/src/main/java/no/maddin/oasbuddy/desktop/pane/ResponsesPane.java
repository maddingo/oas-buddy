package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.ComponentResponses;
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

/** The list of responses under {@code components.responses}, the counterpart to {@link SchemasPane}. */
public final class ResponsesPane {

    private ResponsesPane() {
    }

    /**
     * @param onStructureChanged run after a response is added, so the outline picks it up
     * @param onRemoveResponse   asked to remove the named response; the pane only reports the
     *                           request, it never removes anything itself
     */
    public static Node build(OasDocument document, Runnable onStructureChanged, Consumer<String> onRemoveResponse) {
        ComponentResponses responses = document.getComponents().getResponses();

        GridPane list = FormFields.grid();
        list.setId("responses-list");
        // the name column keeps a minimum width so Description and Remove stay next to the names
        // instead of being pushed to the far edge of a wide window
        ColumnConstraints nameColumn = FormFields.column(HPos.LEFT, Priority.NEVER);
        nameColumn.setMinWidth(220);
        list.getColumnConstraints().addAll(nameColumn,
                FormFields.column(HPos.LEFT, Priority.NEVER),
                FormFields.column(HPos.LEFT, Priority.NEVER));
        fillList(responses, list, onRemoveResponse);

        TextField nameField = new TextField();
        nameField.setId("new-response-name");
        nameField.setPromptText("NotFound");
        Button addButton = new Button("Add response");
        addButton.setId("add-response");
        addButton.getStyleClass().add(Styles.ACCENT);
        addButton.setOnAction(e -> {
            String name = nameField.getText();
            if (name != null && !name.isBlank()) {
                // description is the one field a response must have
                responses.addResponse(name.strip()).setDescription("");
                nameField.clear();
                onStructureChanged.run();
            }
        });

        return FormFields.root(FormFields.heading("Responses"), list, new HBox(8, nameField, addButton));
    }

    private static void fillList(ComponentResponses responses, GridPane list, Consumer<String> onRemoveResponse) {
        List<String> names = responses.names();
        if (names.isEmpty()) {
            Label empty = new Label("No reusable responses yet.");
            empty.getStyleClass().add(Styles.TEXT_MUTED);
            list.add(empty, 0, 0, 3, 1);
            return;
        }

        list.addRow(0, FormFields.columnHeading("Response"), FormFields.columnHeading("Description"));

        int row = 1;
        for (String name : names) {
            String description = responses.getResponse(name).getDescription();
            Label descriptionLabel = new Label(description == null ? "" : description);
            descriptionLabel.getStyleClass().add(Styles.TEXT_MUTED);

            Button removeButton = new Button("Remove");
            removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
            removeButton.setOnAction(e -> onRemoveResponse.accept(name));

            list.addRow(row++, new Label(name), descriptionLabel, removeButton);
        }
    }
}
