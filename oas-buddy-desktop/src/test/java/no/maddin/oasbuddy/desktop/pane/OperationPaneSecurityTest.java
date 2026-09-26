package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.HttpMethod;
import no.maddin.oasbuddy.core.model.Operation;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * An operation's security: inherit the document default, declare itself public, or override.
 *
 * <p>Inherit and public are different things in the spec — no {@code security} key versus an empty
 * array — and the three-way choice here is what keeps them apart.
 */
class OperationPaneSecurityTest extends ApplicationTest {

    private OasDocument document;
    private StackPane holder;

    @Override
    public void start(Stage stage) {
        document = DocumentReader.read("""
                openapi: 3.0.3
                info:
                  title: Secured
                  version: 1.0.0
                security:
                  - ApiKeyAuth: []
                paths:
                  /pets:
                    get:
                      operationId: listPets
                      responses:
                        '200':
                          description: ok
                components:
                  securitySchemes:
                    ApiKeyAuth:
                      type: apiKey
                      name: X-API-Key
                      in: header
                    BearerAuth:
                      type: http
                      scheme: bearer
                """, DocumentFormat.YAML);
        holder = new StackPane();
        stage.setScene(new Scene(holder, 1000, 800));
        stage.show();
        rebuild();
    }

    private void rebuild() {
        holder.getChildren().setAll(OperationPane.build(operation(), List::of,
                TagCatalog.of(document), SecuritySchemeCatalog.of(document),
                ComponentCatalog.of(document), (question, details) -> true, () -> { }));
    }

    private Operation operation() {
        return document.getPaths().getPathItem("/pets").getOperation(HttpMethod.GET);
    }

    @Test
    void anOperationWithNoSecurityOfItsOwnShowsAsInheriting() {
        assertAll(
                () -> assertTrue(radio("inherit").isSelected()),
                () -> assertFalse(operation().getSecurity().isDeclared()));
    }

    @Test
    void declaringItPublicWritesAnEmptyArray() {
        interact(() -> radio("public").setSelected(true));

        assertAll(
                () -> assertTrue(operation().getSecurity().isDeclared()),
                () -> assertEquals(List.of(), operation().getSecurity().requirements()),
                () -> assertTrue(document.getRoot().get("paths").get("/pets").get("get")
                        .get("security").isEmpty()));
    }

    @Test
    void goingBackToInheritingRemovesTheKeyAgain() {
        interact(() -> radio("public").setSelected(true));

        interact(() -> radio("inherit").setSelected(true));

        assertFalse(document.getRoot().get("paths").get("/pets").get("get").has("security"),
                "inheriting means no security key at all");
    }

    @Test
    void overridingOffersTheDocumentsDeclaredSchemes() {
        interact(() -> radio("require").setSelected(true));

        assertEquals(List.of("ApiKeyAuth", "BearerAuth"), List.copyOf(schemePicker().getItems()));
    }

    @Test
    void anOverrideIsWrittenAgainstTheOperation() {
        interact(() -> radio("require").setSelected(true));

        interact(() -> {
            schemePicker().setValue("BearerAuth");
            lookup("#add-security-requirement").queryButton().fire();
        });

        assertEquals(List.of("BearerAuth"),
                operation().getSecurity().requirements().get(0).schemeNames());
    }

    @Test
    void anOperationThatAlreadyOverridesShowsAsOverriding() {
        interact(() -> {
            operation().getSecurity().add("BearerAuth");
            rebuild();
        });

        assertTrue(radio("require").isSelected());
    }

    private RadioButton radio(String mode) {
        return lookup("#operation-security-" + mode).queryAs(RadioButton.class);
    }

    @SuppressWarnings("unchecked")
    private ComboBox<String> schemePicker() {
        return lookup("#security-requirement-scheme").queryAs(ComboBox.class);
    }
}
