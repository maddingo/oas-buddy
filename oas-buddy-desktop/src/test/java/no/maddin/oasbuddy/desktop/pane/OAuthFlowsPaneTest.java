package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.OAuthFlowUrl;
import no.maddin.oasbuddy.core.model.OAuthFlows;
import no.maddin.oasbuddy.core.model.SecurityScheme;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
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
 * The oauth2 flows section: four flows switched on and off independently, each showing only the URLs
 * the spec gives it, each with its own scope map.
 */
class OAuthFlowsPaneTest extends ApplicationTest {

    /** Answers asked of the confirmation, and the answer to give. */
    private final List<String> confirmations = new ArrayList<>();
    private boolean confirm = true;

    private OasDocument document;
    private SecurityScheme scheme;
    private StackPane holder;

    @Override
    public void start(Stage stage) {
        document = OasDocument.newDocument(DocumentFormat.YAML);
        scheme = document.getComponents().getSecuritySchemes().addScheme("OAuth");
        scheme.setType("oauth2");

        holder = new StackPane();
        stage.setScene(new Scene(holder, 1000, 700));
        stage.show();
        rebuild();
    }

    /**
     * Runs on the JavaFX thread, deliberately without {@code interact}: the pane hands this back as
     * its rebuild callback, so it is invoked from inside a control's listener — and {@code interact}
     * called from the FX thread waits for itself.
     */
    private void rebuild() {
        holder.getChildren().setAll(OAuthFlowsPane.build(scheme, (question, details) -> {
            confirmations.add(question);
            return confirm;
        }, this::rebuild));
    }

    @Test
    void everyFlowTheSpecDefinesGetsASwitch() {
        assertAll(OAuthFlows.FLOW_NAMES.stream().map(flow -> () ->
                assertTrue(lookup("#flow-" + flow + "-enabled").tryQuery().isPresent(),
                        "no switch for " + flow)));
    }

    @Test
    void noFlowIsOnUntilOneIsDeclared() {
        assertAll(OAuthFlows.FLOW_NAMES.stream().map(flow -> () ->
                assertFalse(checkBox(flow).isSelected(), flow + " is on without being declared")));
    }

    @Test
    void switchingAFlowOnDeclaresItInTheDocument() {
        interact(() -> checkBox("password").setSelected(true));

        assertEquals(List.of("password"), scheme.getFlows().names());
    }

    @Test
    void anImplicitFlowShowsNoTokenUrl() {
        interact(() -> checkBox("implicit").setSelected(true));

        assertAll(
                () -> assertTrue(urlLabels("implicit").contains("Authorization URL")),
                () -> assertTrue(urlLabels("implicit").contains("Refresh URL")),
                () -> assertFalse(urlLabels("implicit").contains("Token URL"),
                        "implicit has no token URL in OAS 3.0"));
    }

    @Test
    void aClientCredentialsFlowShowsNoAuthorizationUrl() {
        interact(() -> checkBox("clientCredentials").setSelected(true));

        assertAll(
                () -> assertTrue(urlLabels("clientCredentials").contains("Token URL")),
                () -> assertFalse(urlLabels("clientCredentials").contains("Authorization URL"),
                        "clientCredentials has no authorization URL in OAS 3.0"));
    }

    @Test
    void anAuthorizationCodeFlowShowsAllThreeUrls() {
        interact(() -> checkBox("authorizationCode").setSelected(true));

        assertEquals(List.of("Authorization URL", "Token URL", "Refresh URL"),
                urlLabels("authorizationCode"));
    }

    @Test
    void editingAUrlWritesThroughToTheDocument() {
        interact(() -> checkBox("password").setSelected(true));

        TextField tokenUrl = lookup("#flow-password-tokenUrl").queryAs(TextField.class);
        interact(() -> tokenUrl.setText("https://example.com/token"));

        assertEquals("https://example.com/token",
                scheme.getFlows().getFlow("password").getUrl(OAuthFlowUrl.TOKEN_URL));
    }

    @Test
    void switchingAnEmptyFlowOffAsksNothingAndJustRemovesIt() {
        interact(() -> checkBox("implicit").setSelected(true));

        interact(() -> checkBox("implicit").setSelected(false));

        assertAll(
                () -> assertEquals(List.of(), scheme.getFlows().names()),
                () -> assertEquals(List.of(), confirmations, "nothing was at stake, so nothing to ask"));
    }

    @Test
    void switchingAPopulatedFlowOffAsksFirst() {
        givenAPopulatedImplicitFlow();

        interact(() -> checkBox("implicit").setSelected(false));

        assertAll(
                () -> assertEquals(List.of("Turn off the implicit flow?"), confirmations),
                () -> assertEquals(List.of(), scheme.getFlows().names()));
    }

    @Test
    void decliningLeavesBothTheDocumentAndTheSwitchAlone() {
        givenAPopulatedImplicitFlow();
        confirm = false;

        interact(() -> checkBox("implicit").setSelected(false));

        assertAll(
                () -> assertEquals(List.of("implicit"), scheme.getFlows().names()),
                () -> assertTrue(checkBox("implicit").isSelected(),
                        "the switch must not contradict the document"),
                () -> assertEquals("https://example.com/auth",
                        scheme.getFlows().getFlow("implicit").getUrl(OAuthFlowUrl.AUTHORIZATION_URL)));
    }

    @Test
    void scopesCanBeAddedRenamedAndRemoved() {
        interact(() -> checkBox("implicit").setSelected(true));

        interact(() -> {
            lookup("#flow-implicit-new-scope").queryAs(TextField.class).setText("read:pets");
            lookup("#flow-implicit-add-scope").queryButton().fire();
        });
        assertEquals(List.of("read:pets"), scheme.getFlows().getFlow("implicit").scopeNames());

        // A rename commits on Enter or focus-loss, not per keystroke: it rebuilds the scope map to
        // keep the scope in place, which would fight the caret if it ran on every character.
        interact(() -> {
            TextField name = lookup("#flow-implicit-scope-name-0").queryAs(TextField.class);
            name.setText("read:animals");
            name.fireEvent(new ActionEvent());
        });
        assertEquals(List.of("read:animals"), scheme.getFlows().getFlow("implicit").scopeNames());

        interact(() -> lookup("#flow-implicit-scope-description-0").queryAs(TextField.class)
                .setText("read your animals"));
        assertEquals("read your animals",
                scheme.getFlows().getFlow("implicit").getScopeDescription("read:animals"));

        interact(() -> lookup("#flow-implicit-remove-scope-0").queryButton().fire());
        assertEquals(List.of(), scheme.getFlows().getFlow("implicit").scopeNames());
    }

    /**
     * A hand-edited {@code implicit: ~}: present, so its switch is on and the form agrees with the
     * file, but there is nothing in it to edit, and switching it off removes it without asking.
     */
    @Test
    void aFlowThatIsNotAnObjectShowsAsOnWithANoteAndCanBeSwitchedOff() {
        interact(() -> {
            scheme.getFlows().addFlow("password");
            ((com.fasterxml.jackson.databind.node.ObjectNode) document.getRoot()
                    .at("/components/securitySchemes/OAuth/flows")).putNull("implicit");
            rebuild();
        });

        assertAll(
                () -> assertTrue(checkBox("implicit").isSelected()),
                () -> assertTrue(lookup("#flow-implicit-not-editable").tryQuery().isPresent()),
                () -> assertFalse(lookup("#flow-implicit-urls").tryQuery().isPresent()));

        interact(() -> checkBox("implicit").setSelected(false));

        assertAll(
                () -> assertEquals(List.of("password"), scheme.getFlows().names()),
                () -> assertEquals(List.of(), confirmations, "nothing to lose, so nothing is asked"));
    }

    private void givenAPopulatedImplicitFlow() {
        interact(() -> {
            scheme.getFlows().addFlow("implicit")
                    .setUrl(OAuthFlowUrl.AUTHORIZATION_URL, "https://example.com/auth");
            rebuild();
        });
    }

    private CheckBox checkBox(String flow) {
        return lookup("#flow-" + flow + "-enabled").queryAs(CheckBox.class);
    }

    /** The first-column labels of one flow's URL grid, in display order. */
    private List<String> urlLabels(String flow) {
        GridPane grid = lookup("#flow-" + flow + "-urls").queryAs(GridPane.class);
        List<String> labels = new ArrayList<>();
        for (Node cell : grid.getChildren()) {
            if (cell instanceof Label label && GridPanes.column(cell) == 0) {
                labels.add(label.getText());
            }
        }
        return labels;
    }
}
