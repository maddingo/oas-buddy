package no.maddin.oasbuddy.desktop.pane;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.List;

/** Reading the list grids the panes build, shared by the pane tests. */
final class GridPanes {

    private GridPanes() {
    }

    /** The text of every first-column label below the heading row, in display order. */
    static List<String> entries(GridPane grid) {
        List<String> entries = new ArrayList<>();
        for (Node cell : grid.getChildren()) {
            if (cell instanceof Label label && column(cell) == 0 && row(cell) > 0) {
                entries.add(label.getText());
            }
        }
        return entries;
    }

    static Button buttonInRowOf(GridPane grid, String entry) {
        int targetRow = grid.getChildren().stream()
                .filter(cell -> cell instanceof Label label && entry.equals(label.getText()))
                .mapToInt(GridPanes::row)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no row for " + entry));
        return grid.getChildren().stream()
                .filter(cell -> cell instanceof Button && row(cell) == targetRow)
                .map(Button.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no button for " + entry));
    }

    /** The cell in the given column of an entry's row, or {@code null} if that cell is empty. */
    static Node cellInRowOf(GridPane grid, String entry, int column) {
        int targetRow = grid.getChildren().stream()
                .filter(cell -> cell instanceof Label label && entry.equals(label.getText()) && column(cell) == 0)
                .mapToInt(GridPanes::row)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no row for " + entry));
        return grid.getChildren().stream()
                .filter(cell -> row(cell) == targetRow && column(cell) == column)
                .findFirst()
                .orElse(null);
    }

    static int row(Node cell) {
        Integer index = GridPane.getRowIndex(cell);
        return index == null ? 0 : index;
    }

    static int column(Node cell) {
        Integer index = GridPane.getColumnIndex(cell);
        return index == null ? 0 : index;
    }
}
