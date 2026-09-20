package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.SecurityScheme;
import atlantafx.base.theme.Styles;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * One security scheme.
 *
 * <p>The only behaviour beyond the usual write-through form is that the visible fields follow the
 * chosen type: an {@code apiKey} has nothing to say about a bearer format. A scheme whose type this
 * editor has no form for — {@code oauth2} today — gets an explanation instead of a form, and no
 * type picker, so it cannot be converted into something else by accident.
 */
public final class SecuritySchemePane {

    private SecuritySchemePane() {
    }

    /**
     * @param onRemoveScheme asked to remove this scheme; the pane only reports the request, it
     *                       never removes anything itself
     */
    public static Node build(OasDocument document, String schemeName, Consumer<String> onRemoveScheme) {
        SecurityScheme scheme = document.getComponents().getSecuritySchemes().getScheme(schemeName);

        Node header = FormFields.headerWithDelete("Security scheme: " + schemeName,
                "delete-security-scheme", "Delete scheme", () -> onRemoveScheme.accept(schemeName));

        if (!scheme.isEditable()) {
            Label explanation = new Label(scheme.getType() + " schemes cannot be edited yet. "
                    + "This one is left exactly as it was loaded, and is saved unchanged.");
            explanation.setId("security-scheme-readonly");
            explanation.getStyleClass().add(Styles.TEXT_MUTED);
            explanation.setWrapText(true);
            return FormFields.root(header, explanation);
        }

        GridPane fields = FormFields.grid();
        fields.setId("security-scheme-fields");

        GridPane common = FormFields.grid();
        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.setId("security-scheme-type");
        typeBox.getItems().addAll(SecurityScheme.EDITABLE_TYPES);
        typeBox.setValue(scheme.getType());
        typeBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            scheme.setType(newVal);
            fillFields(scheme, fields);
        });
        common.addRow(0, new Label("Type"), typeBox);
        FormFields.textAreaRow(common, 1, "Description", scheme::getDescription, scheme::setDescription);

        fillFields(scheme, fields);

        return FormFields.root(header, common, new VBox(fields));
    }

    /** Rebuilt whenever the type changes, so only the chosen type's fields are on screen. */
    private static void fillFields(SecurityScheme scheme, GridPane fields) {
        fields.getChildren().clear();

        switch (scheme.getType() == null ? "" : scheme.getType()) {
            case "apiKey" -> {
                FormFields.textRow(fields, 0, "Name", scheme::getName, scheme::setName)
                        .setId("security-scheme-name");

                ComboBox<String> inBox = new ComboBox<>();
                inBox.setId("security-scheme-in");
                inBox.getItems().addAll(SecurityScheme.API_KEY_LOCATIONS);
                inBox.setValue(scheme.getIn());
                inBox.valueProperty().addListener((obs, oldVal, newVal) -> scheme.setIn(newVal));
                fields.addRow(1, new Label("In"), inBox);
            }
            case "http" -> {
                FormFields.textRow(fields, 0, "Scheme", scheme::getScheme, scheme::setScheme)
                        .setId("security-scheme-scheme");
                FormFields.textRow(fields, 1, "Bearer format", scheme::getBearerFormat, scheme::setBearerFormat)
                        .setId("security-scheme-bearer-format");
            }
            case "openIdConnect" -> FormFields.textRow(fields, 0, "OpenID Connect URL",
                            scheme::getOpenIdConnectUrl, scheme::setOpenIdConnectUrl)
                    .setId("security-scheme-openid-url");
            default -> {
                // isEditable() gates this method; nothing else can reach it
            }
        }
    }
}
