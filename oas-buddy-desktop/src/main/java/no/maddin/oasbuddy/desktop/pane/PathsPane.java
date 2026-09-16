package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.model.Paths;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class PathsPane {

    private PathsPane() {
    }

    public static Node build(Paths paths, Runnable onStructureChanged) {
        VBox root = new VBox(8);
        root.setPadding(new Insets(12));

        TextField pathField = new TextField();
        pathField.setPromptText("/example/{id}");
        Button addButton = new Button("Add path");
        addButton.setOnAction(e -> {
            String path = pathField.getText();
            if (path != null && !path.isBlank()) {
                paths.addPath(path.strip());
                pathField.clear();
                onStructureChanged.run();
            }
        });

        root.getChildren().addAll(
                new Label("Existing paths: " + String.join(", ", paths.pathNames())),
                new HBox(6, pathField, addButton));
        return root;
    }
}
