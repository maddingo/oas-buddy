package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.model.HttpMethod;
import no.maddin.oasbuddy.core.model.PathItem;
import atlantafx.base.theme.Styles;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;

public final class PathItemPane {

    private PathItemPane() {
    }

    public static Node build(PathItem pathItem, Runnable onStructureChanged) {
        GridPane grid = FormFields.grid();
        int row = 0;
        FormFields.textRow(grid, row++, "Summary", pathItem::getSummary, pathItem::setSummary);
        FormFields.textAreaRow(grid, row, "Description", pathItem::getDescription, pathItem::setDescription);

        FlowPane methodButtons = new FlowPane(8, 8);
        var existing = pathItem.getOperations().keySet();
        for (HttpMethod method : HttpMethod.values()) {
            if (!existing.contains(method)) {
                Button addButton = new Button("+ " + method.name());
                addButton.getStyleClass().addAll(Styles.SMALL, Styles.BUTTON_OUTLINED);
                addButton.setOnAction(e -> {
                    pathItem.addOperation(method);
                    onStructureChanged.run();
                });
                methodButtons.getChildren().add(addButton);
            }
        }

        Label addOperationLabel = new Label("Add operation");
        addOperationLabel.getStyleClass().add(Styles.TEXT_MUTED);

        return FormFields.root(FormFields.heading("Path"), grid, addOperationLabel, methodButtons);
    }
}
