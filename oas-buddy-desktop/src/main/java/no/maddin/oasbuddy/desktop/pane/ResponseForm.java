package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.model.ApiResponse;
import javafx.scene.control.TextField;

/**
 * The description field of an inline response, shared by an operation's response rows and the
 * component response editor; the content below it is a {@link ContentEditor} in both.
 */
final class ResponseForm {

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
}
