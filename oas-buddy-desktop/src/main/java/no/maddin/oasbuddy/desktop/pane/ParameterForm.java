package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.model.Parameter;
import no.maddin.oasbuddy.core.model.Schema;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.List;

/**
 * The fields of an inline parameter, shared by the operation and path parameter rows and the
 * component parameter editor, so they cannot drift apart. None carries an id: several are on
 * screen at once, and the tests find them by type within their row.
 */
final class ParameterForm {

    static final List<String> LOCATIONS = List.of("query", "path", "header", "cookie");

    private ParameterForm() {
    }

    static TextField nameField(Parameter parameter) {
        TextField field = new TextField(nullToEmpty(parameter.getName()));
        field.textProperty().addListener((obs, oldVal, newVal) -> parameter.setName(newVal));
        return field;
    }

    /** @param onChanged run after the new location is written, e.g. to rebuild what depends on it */
    static ComboBox<String> inBox(Parameter parameter, Runnable onChanged) {
        ComboBox<String> box = new ComboBox<>();
        box.getItems().addAll(LOCATIONS);
        box.setValue(parameter.getIn());
        box.valueProperty().addListener((obs, oldVal, newVal) -> {
            parameter.setIn(newVal);
            onChanged.run();
        });
        return box;
    }

    static CheckBox requiredBox(Parameter parameter) {
        CheckBox box = new CheckBox("Required");
        box.setSelected(Boolean.TRUE.equals(parameter.isRequired()));
        box.selectedProperty().addListener((obs, oldVal, newVal) -> parameter.setRequired(newVal));
        return box;
    }

    /**
     * The type field, or for a parameter that uses {@code content} instead of {@code schema} a note
     * saying so: typing a type there would add a {@code schema} beside the {@code content}, which
     * the spec does not allow.
     */
    static javafx.scene.Node typeControl(Parameter parameter) {
        if (parameter.hasContent()) {
            javafx.scene.control.Label note = new javafx.scene.control.Label("content: "
                    + String.join(", ", parameter.getContent().mediaTypes()));
            note.getStyleClass().add("text-muted");
            return note;
        }
        return typeField(parameter);
    }

    /**
     * A plain setter per keystroke, deliberately: see {@code Schema.setType}. Reads without
     * creating, so showing a parameter without a schema does not add an empty one.
     */
    static TextField typeField(Parameter parameter) {
        Schema schema = parameter.findSchema();
        TextField field = new TextField(schema == null ? "" : nullToEmpty(schema.getType()));
        field.setPromptText("type");
        field.textProperty().addListener((obs, oldVal, newVal) -> parameter.getSchema().setType(newVal));
        return field;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
