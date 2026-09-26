package no.maddin.oasbuddy.desktop.pane;

import atlantafx.base.theme.Styles;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;
import java.util.function.Function;
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

    /** A grid column with a fixed horizontal alignment and grow behaviour. */
    static ColumnConstraints column(HPos alignment, Priority hgrow) {
        ColumnConstraints constraints = new ColumnConstraints();
        constraints.setHalignment(alignment);
        constraints.setHgrow(hgrow);
        return constraints;
    }

    /** A muted column title for the list grids. */
    static Label columnHeading(String text) {
        Label label = new Label(text);
        label.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.TEXT_SMALL);
        return label;
    }

    /** A pane title with the control that deletes what the pane is editing. */
    static Node headerWithDelete(String title, String buttonId, String buttonText, Runnable onDelete) {
        return headerWithRenameAndDelete(title, null, buttonId, buttonText, onDelete);
    }

    /** As {@link #headerWithDelete}, with a rename control between the title and the delete button. */
    static Node headerWithRenameAndDelete(String title, Node renameField, String buttonId, String buttonText,
                                          Runnable onDelete) {
        Button deleteButton = new Button(buttonText);
        deleteButton.setId(buttonId);
        deleteButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
        deleteButton.setOnAction(e -> onDelete.run());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = renameField == null
                ? new HBox(8, heading(title), spacer, deleteButton)
                : new HBox(8, heading(title), renameField, spacer, deleteButton);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    /**
     * What a component editor shows instead of a form when the entry under {@code name} is not an
     * object at all — {@code NotFound: ~} or a string in a hand-edited file. The outline lists every
     * key, so such an entry is reachable, and it must neither crash the pane nor be rewritten into
     * something editable: it is left exactly as loaded, the same bargain a security scheme of an
     * unsupported type gets. The header above it still offers Delete.
     */
    static Label notEditable(String name, String kind) {
        Label label = new Label("\"" + name + "\" is not " + kind + " object, so it cannot be edited here. "
                + "It is left exactly as it was loaded, and is saved unchanged.");
        label.setId("not-editable");
        label.getStyleClass().add(Styles.TEXT_MUTED);
        label.setWrapText(true);
        return label;
    }

    /** The second-column text a list pane shows for an entry that is not an object; see {@link #notEditable}. */
    static Label notAnObject() {
        Label label = new Label("(not an object)");
        label.getStyleClass().addAll(Styles.TEXT_MUTED, Styles.WARNING);
        return label;
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

    /**
     * A text field for renaming something, committing on Enter or focus loss rather than per
     * keystroke — the same reason {@code OAuthFlow.renameScope} commits a scope rename as a whole
     * rather than rebuilding the underlying map on every character, which would fight the caret.
     *
     * <p>{@code attempt} performs the rename and returns {@code null} on success. Any other value
     * reverts the field to {@code initialName}; a non-blank value is also shown as an error, the
     * same red-field-plus-tooltip treatment {@code SchemaConstraints} uses for an invalid value.
     * A blank value reverts quietly — for a rename declined through its own confirmation dialog,
     * which already explained itself.
     */
    static TextField renameField(String initialName, Function<String, String> attempt) {
        TextField field = new TextField(initialName);
        Runnable commit = () -> {
            String candidate = field.getText() == null ? "" : field.getText().strip();
            if (candidate.equals(initialName)) {
                field.setText(initialName);
                return;
            }
            String error = candidate.isEmpty() ? "Name cannot be blank." : attempt.apply(candidate);
            if (error == null) {
                field.pseudoClassStateChanged(Styles.STATE_DANGER, false);
                field.setTooltip(null);
            } else {
                field.setText(initialName);
                field.pseudoClassStateChanged(Styles.STATE_DANGER, !error.isBlank());
                field.setTooltip(error.isBlank() ? null : new Tooltip(error));
            }
        };
        field.setOnAction(e -> commit.run());
        field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused) {
                commit.run();
            }
        });
        return field;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
