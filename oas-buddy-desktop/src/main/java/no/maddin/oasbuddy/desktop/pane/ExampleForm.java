package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.model.Example;
import no.maddin.oasbuddy.core.model.SchemaValues;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.function.Supplier;

/**
 * The fields of an inline example, shared by a media type's named examples and the component
 * example editor. No ids: several are on screen at once.
 */
final class ExampleForm {

    private ExampleForm() {
    }

    static TextField summaryField(Example example) {
        TextField field = new TextField(nullToEmpty(example.getSummary()));
        field.setPromptText("summary");
        field.textProperty().addListener((obs, oldVal, newVal) ->
                example.setSummary(newVal == null || newVal.isBlank() ? null : newVal));
        return field;
    }

    /**
     * The value, read by the one rule every typed value in this editor follows ({@link SchemaValues}),
     * against the type the surrounding schema has at the moment of entry — {@code null} where there
     * is none to go by (a component example, or a media type whose schema is a reference), which
     * reads any valid JSON as JSON and anything else as a string.
     */
    static TextArea valueArea(Example example, Supplier<String> type) {
        TextArea area = new TextArea(SchemaValues.display(example.getValue()));
        area.getStyleClass().add("example-value");
        area.setPromptText("value — JSON, or plain text");
        area.setPrefRowCount(3);
        area.textProperty().addListener((obs, oldVal, newVal) ->
                example.setValue(SchemaValues.parse(newVal, type.get())));
        return area;
    }

    static TextField externalValueField(Example example) {
        TextField field = new TextField(nullToEmpty(example.getExternalValue()));
        field.setPromptText("https://…");
        field.textProperty().addListener((obs, oldVal, newVal) ->
                example.setExternalValue(newVal == null || newVal.isBlank() ? null : newVal));
        return field;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
