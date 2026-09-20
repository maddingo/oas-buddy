package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.SecurityScheme;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The single-scheme editor. Its one real behaviour is that the visible fields follow the chosen
 * type — everything else is the same write-through form the other panes are.
 */
class SecuritySchemePaneTest extends ApplicationTest {

    private final List<String> removalRequests = new ArrayList<>();
    private final List<String> confirmations = new ArrayList<>();
    private boolean confirm = true;

    private OasDocument document;
    private StackPane holder;

    @Override
    public void start(Stage stage) {
        document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getComponents().getSecuritySchemes().addScheme("ApiKeyAuth").setType("apiKey");
        document.getComponents().getSecuritySchemes().addScheme("OAuth2Auth").setType("oauth2");
        // A type this editor has no form for, and is therefore obliged to leave alone.
        document.getComponents().getSecuritySchemes().addScheme("MutualTls").setType("mutualTLS");

        holder = new StackPane();
        stage.setScene(new Scene(holder, 900, 600));
        stage.show();
        show("ApiKeyAuth");
    }

    private void show(String schemeName) {
        Node pane = SecuritySchemePane.build(document, schemeName, (question, details) -> {
            confirmations.add(question);
            return confirm;
        }, removalRequests::add);
        interact(() -> holder.getChildren().setAll(pane));
    }

    @Test
    void anApiKeySchemeShowsOnlyItsOwnFields() {
        assertAll(
                () -> assertTrue(fieldLabels().contains("Name")),
                () -> assertTrue(fieldLabels().contains("In")),
                () -> assertTrue(!fieldLabels().contains("Scheme"), "http's field is showing"),
                () -> assertTrue(!fieldLabels().contains("OpenID Connect URL"), "oidc's field is showing"));
    }

    @Test
    void choosingAnotherTypeSwapsTheFieldsForThatTypes() {
        interact(() -> typeBox().setValue("http"));

        assertAll(
                () -> assertTrue(fieldLabels().contains("Scheme")),
                () -> assertTrue(fieldLabels().contains("Bearer format")),
                () -> assertTrue(!fieldLabels().contains("Name"), "apiKey's field survived the switch"),
                () -> assertTrue(!fieldLabels().contains("In"), "apiKey's field survived the switch"));
    }

    @Test
    void choosingAnotherTypeWritesItThroughToTheDocument() {
        interact(() -> typeBox().setValue("openIdConnect"));

        assertEquals("openIdConnect", scheme("ApiKeyAuth").getType());
    }

    @Test
    void editingAFieldWritesThroughToTheDocument() {
        TextField nameField = lookup("#security-scheme-name").queryAs(TextField.class);

        interact(() -> nameField.setText("X-API-Key"));

        assertEquals("X-API-Key", scheme("ApiKeyAuth").getName());
    }

    @Test
    void theApiKeyLocationIsPickedFromTheThreeTheSpecAllows() {
        ComboBox<String> inBox = lookup("#security-scheme-in").queryAs(ComboBox.class);

        assertEquals(List.of("query", "header", "cookie"), List.copyOf(inBox.getItems()));
    }

    @Test
    void aSchemeOfATypeWithNoFormSaysSoAndOffersNoTypePicker() {
        show("MutualTls");

        assertAll(
                () -> assertTrue(lookup("#security-scheme-readonly").tryQuery().isPresent(),
                        "no explanation for a scheme the editor cannot edit"),
                () -> assertTrue(lookup("#security-scheme-type").tryQuery().isEmpty(),
                        "an uneditable scheme must not offer a type picker"),
                () -> assertTrue(lookup("#security-scheme-fields").tryQuery().isEmpty(),
                        "an uneditable scheme must not offer a form"));
    }

    @Test
    void aSchemeOfATypeWithNoFormCanStillBeDeleted() {
        show("MutualTls");

        interact(() -> lookup("#delete-security-scheme").queryButton().fire());

        assertEquals(List.of("MutualTls"), removalRequests);
    }

    @Test
    void anOauth2SchemeShowsTheFlowsSection() {
        show("OAuth2Auth");

        assertTrue(lookup("#oauth-flows").tryQuery().isPresent(), "no flows section for an oauth2 scheme");
    }

    @Test
    void switchingAwayFromAPopulatedOauth2SchemeAsksFirst() {
        givenAPopulatedOauth2Scheme();

        interact(() -> typeBox().setValue("apiKey"));

        assertAll(
                () -> assertEquals(List.of("Change \"OAuth2Auth\" from oauth2 to apiKey?"), confirmations),
                () -> assertEquals("apiKey", scheme("OAuth2Auth").getType()),
                () -> assertTrue(scheme("OAuth2Auth").getFlows().isEmpty()));
    }

    @Test
    void decliningThatChangeLeavesTheSchemeAndThePickerAlone() {
        givenAPopulatedOauth2Scheme();
        confirm = false;

        interact(() -> typeBox().setValue("apiKey"));

        assertAll(
                () -> assertEquals("oauth2", scheme("OAuth2Auth").getType()),
                () -> assertFalse(scheme("OAuth2Auth").getFlows().isEmpty()),
                () -> assertEquals("oauth2", typeBox().getValue(),
                        "the picker must not contradict the document"));
    }

    @Test
    void switchingAwayFromAnEmptyOauth2SchemeAsksNothing() {
        show("OAuth2Auth");

        interact(() -> typeBox().setValue("apiKey"));

        assertAll(
                () -> assertEquals(List.of(), confirmations, "nothing was at stake, so nothing to ask"),
                () -> assertEquals("apiKey", scheme("OAuth2Auth").getType()));
    }

    private void givenAPopulatedOauth2Scheme() {
        scheme("OAuth2Auth").getFlows().addFlow("implicit")
                .setUrl(no.maddin.oasbuddy.core.model.OAuthFlowUrl.AUTHORIZATION_URL,
                        "https://example.com/auth");
        show("OAuth2Auth");
    }

    @Test
    void theEditableTypesAreTheOnesOfferedByThePicker() {
        assertEquals(SecurityScheme.EDITABLE_TYPES, List.copyOf(typeBox().getItems()));
    }

    @SuppressWarnings("unchecked")
    private ComboBox<String> typeBox() {
        return lookup("#security-scheme-type").queryAs(ComboBox.class);
    }

    private SecurityScheme scheme(String name) {
        return document.getComponents().getSecuritySchemes().getScheme(name);
    }

    /** The first-column labels of the type-specific grid. */
    private List<String> fieldLabels() {
        GridPane fields = lookup("#security-scheme-fields").queryAs(GridPane.class);
        List<String> labels = new ArrayList<>();
        for (Node cell : fields.getChildren()) {
            if (cell instanceof Label label && GridPanes.column(cell) == 0) {
                labels.add(label.getText());
            }
        }
        return labels;
    }
}
