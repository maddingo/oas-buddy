package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.ApiResponse;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** One response under {@code components.responses}: the same fields an inline response has. */
public final class ResponsePane {

    private ResponsePane() {
    }

    /**
     * @param onRemoveResponse asked to remove this response; the pane only reports the request, it
     *                         never removes anything itself
     */
    public static Node build(OasDocument document, String responseName, Supplier<List<String>> schemaNames,
                             Consumer<String> onRemoveResponse) {
        ApiResponse response = document.getComponents().getResponses().getResponse(responseName);

        GridPane grid = FormFields.grid();
        TextField description = ResponseForm.descriptionField(response);
        description.setId("response-description");
        ComboBox<String> schema = ResponseForm.schemaPicker(response, schemaNames);
        schema.setId("response-schema");
        grid.addRow(0, new Label("Description"), description);
        grid.addRow(1, new Label("Schema (" + ResponseForm.MEDIA_TYPE + ")"), schema);

        return FormFields.root(
                FormFields.headerWithDelete("Response: " + responseName, "delete-response", "Delete response",
                        () -> onRemoveResponse.accept(responseName)),
                grid);
    }
}
