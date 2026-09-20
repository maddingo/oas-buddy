package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.OasDocument;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The document-wide default security. */
class SecurityPaneTest extends ApplicationTest {

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
                paths: {}
                components:
                  securitySchemes:
                    ApiKeyAuth:
                      type: apiKey
                      name: X-API-Key
                      in: header
                """, DocumentFormat.YAML);
        holder = new StackPane();
        stage.setScene(new Scene(holder, 1000, 700));
        stage.show();
        rebuild();
    }

    private void rebuild() {
        holder.getChildren().setAll(SecurityPane.build(document, this::rebuild));
    }

    @Test
    void showsTheDocumentsOwnRequirements() {
        assertAll(
                () -> assertTrue(lookup("#security-requirements").tryQuery().isPresent()),
                () -> assertEquals(List.of("ApiKeyAuth"),
                        document.getSecurity().requirements().get(0).schemeNames()));
    }

    /**
     * At document level an empty array and an absent key mean the same thing, so removing the last
     * requirement takes the key out rather than leaving a `security: []` behind. (For an operation
     * the two differ, which is why that distinction lives in the operation pane instead.)
     */
    @Test
    void removingTheLastRequirementLeavesNoSecurityKeyBehind() {
        interact(() -> lookup("#remove-security-requirement-0").queryButton().fire());

        assertAll(
                () -> assertFalse(document.getSecurity().isDeclared()),
                () -> assertFalse(document.getRoot().has("security"),
                        "an empty security array is noise at document level"));
    }
}
