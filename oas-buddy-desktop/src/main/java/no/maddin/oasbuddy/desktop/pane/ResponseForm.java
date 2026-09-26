package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.model.ApiResponse;
import no.maddin.oasbuddy.core.model.Schema;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.function.Supplier;

/**
 * The fields of an inline response, shared by an operation's response rows and the component
 * response editor, so the two cannot drift apart.
 */
final class ResponseForm {

    static final String MEDIA_TYPE = "application/json";
    private static final String SCHEMA_REF_PREFIX = "#/components/schemas/";

    private ResponseForm() {
    }

    /**
     * Writes whatever is typed, an empty string included: {@code description} is the one field
     * OAS requires of a response, so clearing it must not remove the key.
     */
    static TextField descriptionField(ApiResponse response) {
        TextField field = new TextField(response.getDescription() == null ? "" : response.getDescription());
        field.textProperty().addListener((obs, oldVal, newVal) -> response.setDescription(newVal));
        return field;
    }

    /** Reads without creating: a response with no content must not grow one just by being shown. */
    static ComboBox<String> schemaPicker(ApiResponse response, Supplier<List<String>> schemaNames) {
        ComboBox<String> schemaBox = new ComboBox<>();
        schemaBox.getItems().addAll(schemaNames.get());
        Schema existing = response.findSchema(MEDIA_TYPE);
        schemaBox.setValue(existing == null ? null : existing.getReferencedSchemaName());
        schemaBox.valueProperty().addListener((obs, oldVal, newVal) ->
                response.getSchema(MEDIA_TYPE).setRef(newVal == null ? null : SCHEMA_REF_PREFIX + newVal));
        return schemaBox;
    }
}
