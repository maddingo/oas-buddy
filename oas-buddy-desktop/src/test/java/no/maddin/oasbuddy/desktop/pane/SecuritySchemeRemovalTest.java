package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.OasDocument;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The removal flow, with the confirmation stubbed out. Mirrors {@link SchemaRemovalTest}. */
class SecuritySchemeRemovalTest {

    @Test
    void removesTheSchemeAndNotifiesWhenConfirmed() {
        OasDocument document = secured();
        AtomicBoolean notified = new AtomicBoolean();

        SecuritySchemeRemoval.remove(document, "ApiKeyAuth", (question, details) -> true,
                () -> notified.set(true));

        assertFalse(document.getComponents().getSecuritySchemes().names().contains("ApiKeyAuth"));
        assertTrue(notified.get());
    }

    @Test
    void keepsTheSchemeAndStaysQuietWhenDeclined() {
        OasDocument document = secured();
        AtomicBoolean notified = new AtomicBoolean();

        SecuritySchemeRemoval.remove(document, "ApiKeyAuth", (question, details) -> false,
                () -> notified.set(true));

        assertTrue(document.getComponents().getSecuritySchemes().names().contains("ApiKeyAuth"));
        assertFalse(notified.get());
    }

    @Test
    void tellsTheUserWhichRequirementsTheRemovalWouldLeaveDangling() {
        List<String> asked = new ArrayList<>();

        SecuritySchemeRemoval.remove(secured(), "ApiKeyAuth", (question, details) -> {
            asked.add(question + " | " + details);
            return false;
        }, () -> { });

        assertEquals(List.of("Remove security scheme \"ApiKeyAuth\"? | Still required by:"
                + "\n  security → [0]"
                + "\n  paths → /pets → get → security → [0]"
                + "\n\nThose requirements will be left dangling."), asked);
    }

    @Test
    void asksForConfirmationEvenWhenNothingRequiresTheScheme() {
        List<String> asked = new ArrayList<>();

        SecuritySchemeRemoval.remove(secured(), "Unused", (question, details) -> {
            asked.add(question + " | " + details);
            return false;
        }, () -> { });

        assertEquals(List.of("Remove security scheme \"Unused\"? | Nothing requires it."), asked);
    }

    private static OasDocument secured() {
        return DocumentReader.read("""
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
                      security:
                        - ApiKeyAuth: []
                      responses:
                        '200':
                          description: ok
                components:
                  securitySchemes:
                    ApiKeyAuth:
                      type: apiKey
                      name: X-API-Key
                      in: header
                    Unused:
                      type: http
                      scheme: basic
                """, DocumentFormat.YAML);
    }
}
