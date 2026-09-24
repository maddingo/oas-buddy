package no.maddin.oasbuddy.desktop.pane;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Reading the list grids the panes build, shared by the pane tests. */
final class GridPanes {

    private GridPanes() {
    }

    /** The text of every first-column label below the heading row, in display order. */
    static List<String> entries(GridPane grid) {
        List<String> entries = new ArrayList<>();
        for (Node cell : grid.getChildren()) {
            if (column(cell) == 0 && row(cell) > 0) {
                text(cell).ifPresent(entries::add);
            }
        }
        return entries;
    }

    static Button buttonInRowOf(GridPane grid, String entry) {
        int targetRow = grid.getChildren().stream()
                .filter(cell -> text(cell).filter(entry::equals).isPresent())
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
                .filter(cell -> column(cell) == 0 && text(cell).filter(entry::equals).isPresent())
                .mapToInt(GridPanes::row)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no row for " + entry));
        return grid.getChildren().stream()
                .filter(cell -> row(cell) == targetRow && column(cell) == column)
                .findFirst()
                .orElse(null);
    }

    /** A cell's identifying text, whether it is a plain label or a rename field. */
    private static Optional<String> text(Node cell) {
        if (cell instanceof Label label) {
            return Optional.ofNullable(label.getText());
        }
        if (cell instanceof TextField field) {
            return Optional.ofNullable(field.getText());
        }
        return Optional.empty();
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
