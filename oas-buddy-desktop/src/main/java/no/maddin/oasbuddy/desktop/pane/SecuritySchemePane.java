package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.SecurityScheme;
import atlantafx.base.theme.Styles;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * One security scheme.
 *
 * <p>The only behaviour beyond the usual write-through form is that the visible fields follow the
 * chosen type: an {@code apiKey} has nothing to say about a bearer format, and an {@code oauth2}
 * shows its flows instead of any flat field at all. A scheme whose type this editor has no form for
 * — {@code mutualTLS}, or a typo — gets an explanation instead of a form, and no type picker, so it
 * cannot be converted into something else by accident.
 */
public final class SecuritySchemePane {

    private SecuritySchemePane() {
    }

    /**
     * @param confirmation   asked before a type change or a flow switch discards something
     * @param onRemoveScheme asked to remove this scheme; the pane only reports the request, it
     *                       never removes anything itself
     */
    public static Node build(OasDocument document, String schemeName,
                             RemovalConfirmation confirmation, Consumer<String> onRemoveScheme) {
        SecurityScheme scheme = document.getComponents().getSecuritySchemes().getScheme(schemeName);

        Node header = FormFields.headerWithDelete("Security scheme: " + schemeName,
                "delete-security-scheme", "Delete scheme", () -> onRemoveScheme.accept(schemeName));
        if (scheme == null) {
            return FormFields.root(header, FormFields.notEditable(schemeName, "a security scheme"));
        }

        if (!scheme.isEditable()) {
            Label explanation = new Label(scheme.getType() + " schemes cannot be edited yet. "
                    + "This one is left exactly as it was loaded, and is saved unchanged.");
            explanation.setId("security-scheme-readonly");
            explanation.getStyleClass().add(Styles.TEXT_MUTED);
            explanation.setWrapText(true);
            return FormFields.root(header, explanation);
        }

        // Holds whichever editor the current type calls for; swapped wholesale on a type change,
        // because oauth2 needs a whole section where the flat types need a few grid rows.
        VBox typed = new VBox();
        typed.setId("security-scheme-typed");

        GridPane common = FormFields.grid();
        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.setId("security-scheme-type");
        typeBox.getItems().addAll(SecurityScheme.EDITABLE_TYPES);
        typeBox.setValue(scheme.getType());
        // Reverting a declined change would otherwise re-enter this listener.
        boolean[] reverting = {false};
        typeBox.valueProperty().addListener((obs, oldType, newType) -> {
            if (reverting[0]) {
                return;
            }
            if (!confirmTypeChange(scheme, schemeName, oldType, newType, confirmation)) {
                reverting[0] = true;
                typeBox.setValue(oldType);
                reverting[0] = false;
                return;
            }
            scheme.setType(newType);
            fillTyped(scheme, typed, confirmation);
        });
        common.addRow(0, new Label("Type"), typeBox);
        FormFields.textAreaRow(common, 1, "Description", scheme::getDescription, scheme::setDescription);

        fillTyped(scheme, typed, confirmation);

        return FormFields.root(header, common, typed);
    }

    /**
     * Whether the type change may go ahead. Changing type drops the old type's fields, which only
     * matters when they hold something — and for oauth2 that something is a whole flows object.
     */
    private static boolean confirmTypeChange(SecurityScheme scheme, String schemeName,
                                             String oldType, String newType,
                                             RemovalConfirmation confirmation) {
        if (!"oauth2".equals(oldType) || scheme.getFlows().isEmpty()) {
            return true;
        }
        return confirmation.confirm(
                "Change \"" + schemeName + "\" from " + oldType + " to " + newType + "?",
                "This discards the oauth2 flows: " + String.join(", ", scheme.getFlows().names()));
    }

    /** Rebuilt whenever the type changes, so only the chosen type's editor is on screen. */
    private static void fillTyped(SecurityScheme scheme, VBox typed, RemovalConfirmation confirmation) {
        typed.getChildren().clear();

        if ("oauth2".equals(scheme.getType())) {
            typed.getChildren().add(OAuthFlowsPane.build(scheme, confirmation,
                    () -> fillTyped(scheme, typed, confirmation)));
            return;
        }

        GridPane fields = FormFields.grid();
        fields.setId("security-scheme-fields");
        typed.getChildren().add(fields);

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
