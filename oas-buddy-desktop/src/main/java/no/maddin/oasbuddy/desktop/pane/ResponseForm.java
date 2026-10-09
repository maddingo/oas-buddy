package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.model.ApiResponse;
import javafx.scene.control.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * The description field of an inline response, shared by an operation's response rows and the
 * component response editor; the content below it is a {@link ContentEditor} in both.
 */
final class ResponseForm {

    private static final Logger log = LoggerFactory.getLogger(ResponseForm.class);

    private ResponseForm() {
    }

    /**
     * Writes whatever is typed, an empty string included: {@code description} is the one field
     * OAS requires of a response, so clearing it must not remove the key.
     */
    @SuppressWarnings("unused")
    static TextField descriptionField(ApiResponse response) {
        if (response == null) {
            log.warn("Response is null");
            return new TextField("");
        }
        String description = Optional.of(response)
                .map(ApiResponse::getDescription)
                .orElse("");
        TextField field = new TextField(description);
        field.textProperty().addListener((obs, oldVal, newVal) -> response.setDescription(newVal));
        return field;
    }
}
