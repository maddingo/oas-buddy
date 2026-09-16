package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.model.HttpMethod;
import no.maddin.oasbuddy.core.model.PathItem;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public final class PathItemPane {

    private PathItemPane() {
    }

    public static Node build(PathItem pathItem, Runnable onStructureChanged) {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);

        int row = 0;
        FormFields.textRow(grid, row++, "Summary", pathItem::getSummary, pathItem::setSummary);
        FormFields.textAreaRow(grid, row, "Description", pathItem::getDescription, pathItem::setDescription);

        FlowPane methodButtons = new FlowPane(6, 6);
        var existing = pathItem.getOperations().keySet();
        for (HttpMethod method : HttpMethod.values()) {
            if (!existing.contains(method)) {
                Button addButton = new Button("+ " + method.name());
                addButton.setOnAction(e -> {
                    pathItem.addOperation(method);
                    onStructureChanged.run();
                });
                methodButtons.getChildren().add(addButton);
            }
        }

        VBox root = new VBox(12, grid, new Label("Add operation:"), methodButtons);
        root.setPadding(new Insets(12));
        return root;
    }
}
