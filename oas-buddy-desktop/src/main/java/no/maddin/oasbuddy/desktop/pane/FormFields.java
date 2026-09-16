package no.maddin.oasbuddy.desktop.pane;

import atlantafx.base.theme.Styles;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;
import java.util.function.Supplier;

final class FormFields {

    private FormFields() {
    }

    /** A top-level pane body: consistent outer spacing/padding for every editor pane. */
    static VBox root(Node... sections) {
        VBox box = new VBox(18, sections);
        box.setPadding(new Insets(16));
        return box;
    }

    /** A form grid with the standard column/row gaps used across every editor pane. */
    static GridPane grid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        return grid;
    }

    static Label heading(String text) {
        Label label = new Label(text);
        label.getStyleClass().add(Styles.TITLE_4);
        return label;
    }

    static TextField textRow(GridPane grid, int row, String label, Supplier<String> getter, Consumer<String> setter) {
        TextField field = new TextField(nullToEmpty(getter.get()));
        field.textProperty().addListener((obs, oldVal, newVal) ->
                setter.accept(newVal == null || newVal.isBlank() ? null : newVal));
        grid.addRow(row, new Label(label), field);
        return field;
    }

    static TextArea textAreaRow(GridPane grid, int row, String label, Supplier<String> getter, Consumer<String> setter) {
        TextArea area = new TextArea(nullToEmpty(getter.get()));
        area.setPrefRowCount(3);
        area.setWrapText(true);
        area.textProperty().addListener((obs, oldVal, newVal) ->
                setter.accept(newVal == null || newVal.isBlank() ? null : newVal));
        grid.addRow(row, new Label(label), area);
        return area;
    }

    static CheckBox checkBoxRow(GridPane grid, int row, String label, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        CheckBox box = new CheckBox();
        box.setSelected(Boolean.TRUE.equals(getter.get()));
        box.selectedProperty().addListener((obs, oldVal, newVal) -> setter.accept(newVal));
        grid.addRow(row, new Label(label), box);
        return box;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
