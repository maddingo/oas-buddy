package no.maddin.oasbuddy.desktop.pane;

import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.function.Consumer;
import java.util.function.Supplier;

final class FormFields {

    private FormFields() {
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
