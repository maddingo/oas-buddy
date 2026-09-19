package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.Paths;
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

public final class PathsPane {

    private PathsPane() {
    }

    /**
     * @param onStructureChanged run after a path is added, so the outline picks it up
     * @param onRemovePath       asked to remove the named path; the pane only reports the request,
     *                           it never removes anything itself
     */
    public static Node build(OasDocument document, Runnable onStructureChanged, Consumer<String> onRemovePath) {
        Paths paths = document.getPaths();

        GridPane list = FormFields.grid();
        list.setId("paths-list");
        // the name column keeps a minimum width so Remove stays next to the paths
        // instead of being pushed to the far edge of a wide window
        ColumnConstraints pathColumn = FormFields.column(HPos.LEFT, Priority.NEVER);
        pathColumn.setMinWidth(260);
        list.getColumnConstraints().addAll(pathColumn, FormFields.column(HPos.LEFT, Priority.NEVER));
        fillList(paths, list, onRemovePath);

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

        return FormFields.root(FormFields.heading("Paths"), list, new HBox(8, pathField, addButton));
    }

    private static void fillList(Paths paths, GridPane list, Consumer<String> onRemovePath) {
        List<String> names = paths.pathNames();
        if (names.isEmpty()) {
            Label empty = new Label("No paths yet.");
            empty.getStyleClass().add(Styles.TEXT_MUTED);
            list.add(empty, 0, 0, 2, 1);
            return;
        }

        list.addRow(0, FormFields.columnHeading("Path"));

        int row = 1;
        for (String name : names) {
            Button removeButton = new Button("Remove");
            removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
            removeButton.setOnAction(e -> onRemovePath.accept(name));
            list.addRow(row++, new Label(name), removeButton);
        }
    }
}
