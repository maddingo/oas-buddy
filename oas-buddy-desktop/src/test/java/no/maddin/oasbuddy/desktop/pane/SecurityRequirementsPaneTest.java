package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.SecurityRequirements;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
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

/** The requirement list shared by the document security pane and the operation pane. */
class SecurityRequirementsPaneTest extends ApplicationTest {

    private static final String DOCUMENT = """
            openapi: 3.0.3
            info:
              title: Secured
              version: 1.0.0
            paths: {}
            components:
              securitySchemes:
                ApiKeyAuth:
                  type: apiKey
                  name: X-API-Key
                  in: header
                OAuth2Auth:
                  type: oauth2
                  flows:
                    implicit:
                      authorizationUrl: https://example.com/auth
                      scopes:
                        read:pets: read your pets
                        write:pets: modify your pets
            """;

    private OasDocument document;
    private StackPane holder;

    @Override
    public void start(Stage stage) {
        document = DocumentReader.read(DOCUMENT, DocumentFormat.YAML);
        holder = new StackPane();
        stage.setScene(new Scene(holder, 1000, 700));
        stage.show();
        rebuild();
    }

    /** On the FX thread without {@code interact}: the pane calls this from inside a listener. */
    private void rebuild() {
        holder.getChildren().setAll(SecurityRequirementsPane.build(
                document.getSecurity(), SecuritySchemeCatalog.of(document), this::rebuild));
    }

    private SecurityRequirements security() {
        return document.getSecurity();
    }

    @Test
    void onlyDeclaredSchemesAreOfferedToRequire() {
        assertEquals(List.of("ApiKeyAuth", "OAuth2Auth"), List.copyOf(schemePicker().getItems()));
    }

    @Test
    void addingARequirementWritesItToTheDocument() {
        interact(() -> {
            schemePicker().setValue("ApiKeyAuth");
            lookup("#add-security-requirement").queryButton().fire();
        });

        assertEquals(List.of("ApiKeyAuth"), security().requirements().get(0).schemeNames());
    }

    @Test
    void addingNothingRequiresNothing() {
        interact(() -> lookup("#add-security-requirement").queryButton().fire());

        assertFalse(security().isDeclared(), "a requirement with no scheme is not a requirement");
    }

    @Test
    void anOauth2RequirementOffersOnlyThatSchemesScopes() {
        givenARequirementOn("OAuth2Auth");

        assertEquals(List.of("read:pets", "write:pets"),
                offeredScopes().stream().map(CheckBox::getText).toList());
    }

    @Test
    void tickingAScopeRequiresIt() {
        givenARequirementOn("OAuth2Auth");

        interact(() -> scopeBox("read:pets").setSelected(true));

        assertEquals(List.of("read:pets"), security().requirements().get(0).getScopes("OAuth2Auth"));
    }

    @Test
    void untickingAScopeStopsRequiringItButKeepsTheScheme() {
        givenARequirementOn("OAuth2Auth");
        interact(() -> scopeBox("read:pets").setSelected(true));

        interact(() -> scopeBox("read:pets").setSelected(false));

        assertAll(
                () -> assertEquals(List.of(), security().requirements().get(0).getScopes("OAuth2Auth")),
                () -> assertEquals(List.of("OAuth2Auth"), security().requirements().get(0).schemeNames()));
    }

    @Test
    void aSchemeWithNoScopesOffersNone() {
        givenARequirementOn("ApiKeyAuth");

        assertTrue(lookup("#security-requirement-0-scopes").tryQuery().isEmpty(),
                "an apiKey requirement has no scopes to offer");
    }

    @Test
    void removingARequirementTakesItOut() {
        givenARequirementOn("ApiKeyAuth");

        interact(() -> lookup("#remove-security-requirement-0").queryButton().fire());

        assertEquals(List.of(), security().requirements());
    }

    /**
     * A requirement naming several schemes demands all of them at once, which this editor does not
     * build. It still has to be visible, and left alone.
     */
    @Test
    void aCombinedRequirementIsShownButNotOfferedForEditing() {
        interact(() -> {
            var requirement = security().add("ApiKeyAuth");
            requirement.setScopes("OAuth2Auth", List.of());
            rebuild();
        });

        assertAll(
                () -> assertTrue(rowLabels().stream().anyMatch(text -> text.contains("ApiKeyAuth")
                        && text.contains("OAuth2Auth")), "combined schemes not both shown: " + rowLabels()),
                () -> assertTrue(lookup("#security-requirement-0-scheme").tryQuery().isEmpty(),
                        "a combined requirement must not offer a scheme editor"),
                () -> assertTrue(lookup("#remove-security-requirement-0").tryQuery().isPresent(),
                        "it should still be removable"));
    }

    private void givenARequirementOn(String schemeName) {
        interact(() -> {
            security().add(schemeName);
            rebuild();
        });
    }

    @SuppressWarnings("unchecked")
    private ComboBox<String> schemePicker() {
        return lookup("#security-requirement-scheme").queryAs(ComboBox.class);
    }

    /** By label, not by id: scope names contain colons, which a CSS id selector cannot express. */
    private CheckBox scopeBox(String scope) {
        return offeredScopes().stream()
                .filter(check -> scope.equals(check.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no checkbox for scope " + scope));
    }

    private List<CheckBox> offeredScopes() {
        return lookup("#security-requirement-0-scopes").queryAs(FlowPane.class)
                .getChildren().stream()
                .filter(CheckBox.class::isInstance)
                .map(CheckBox.class::cast)
                .toList();
    }

    private List<String> rowLabels() {
        GridPane grid = lookup("#security-requirements").queryAs(GridPane.class);
        List<String> labels = new ArrayList<>();
        for (Node cell : grid.getChildren()) {
            if (cell instanceof Label label) {
                labels.add(label.getText());
            }
        }
        return labels;
    }
}
