package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.model.Paths;
import atlantafx.base.theme.Styles;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public final class PathsPane {

    private PathsPane() {
    }

    public static Node build(Paths paths, Runnable onStructureChanged) {
        Label existing = new Label(paths.pathNames().isEmpty()
                ? "No paths yet."
                : "Existing paths: " + String.join(", ", paths.pathNames()));
        existing.getStyleClass().add(Styles.TEXT_MUTED);

        TextField pathField = new TextField();
        pathField.setPromptText("/example/{id}");
        Button addButton = new Button("Add path");
        addButton.getStyleClass().add(Styles.ACCENT);
        addButton.setOnAction(e -> {
            String path = pathField.getText();
            if (path != null && !path.isBlank()) {
                paths.addPath(path.strip());
                pathField.clear();
                onStructureChanged.run();
            }
        });

        return FormFields.root(FormFields.heading("Paths"), existing, new HBox(8, pathField, addButton));
    }
}
