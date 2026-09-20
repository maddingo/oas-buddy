package no.maddin.oasbuddy.core.model;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.OasDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** One scheme's fields, and what changing its type does to the ones that no longer apply. */
class SecuritySchemeTest {

    @Test
    void apiKeyFieldsWriteThroughToTheTree() {
        SecurityScheme scheme = newScheme();

        scheme.setType("apiKey");
        scheme.setName("X-API-Key");
        scheme.setIn("header");
        scheme.setDescription("Key issued by support");

        assertAll(
                () -> assertEquals("apiKey", scheme.getType()),
                () -> assertEquals("X-API-Key", scheme.getName()),
                () -> assertEquals("header", scheme.getIn()),
                () -> assertEquals("Key issued by support", scheme.getDescription()));
    }

    @Test
    void httpFieldsWriteThroughToTheTree() {
        SecurityScheme scheme = newScheme();

        scheme.setType("http");
        scheme.setScheme("bearer");
        scheme.setBearerFormat("JWT");

        assertAll(
                () -> assertEquals("bearer", scheme.getScheme()),
                () -> assertEquals("JWT", scheme.getBearerFormat()));
    }

    @Test
    void openIdConnectUrlWritesThroughToTheTree() {
        SecurityScheme scheme = newScheme();

        scheme.setType("openIdConnect");
        scheme.setOpenIdConnectUrl("https://example.com/.well-known/openid-configuration");

        assertEquals("https://example.com/.well-known/openid-configuration", scheme.getOpenIdConnectUrl());
    }

    /**
     * A leftover {@code name} on an http scheme is invisible in the form — that field is not
     * rendered for http — but would still be written to the file. Silent state that changes the
     * output is worse than losing a value the user can retype.
     */
    @Test
    void switchingTypeDropsTheFieldsThatNoLongerApply() {
        SecurityScheme scheme = newScheme();
        scheme.setType("apiKey");
        scheme.setName("X-API-Key");
        scheme.setIn("header");
        scheme.setDescription("Kept across the switch");

        scheme.setType("http");

        assertAll(
                () -> assertNull(scheme.getName()),
                () -> assertNull(scheme.getIn()),
                () -> assertEquals("Kept across the switch", scheme.getDescription()),
                () -> assertEquals("http", scheme.getType()));
    }

    /**
     * The strip list is the union of the three editable types' own fields and nothing else, so an
     * oauth2 scheme's {@code flows} and any {@code x-} extension survive whatever happens around
     * them. The editor must never quietly drop what it cannot edit.
     */
    @Test
    void switchingTypeLeavesFieldsItDoesNotUnderstandAlone() {
        OasDocument document = DocumentReader.read("""
                openapi: 3.0.3
                info:
                  title: Secured
                  version: 1.0.0
                paths: {}
                components:
                  securitySchemes:
                    Odd:
                      type: apiKey
                      name: X-API-Key
                      in: header
                      x-internal: true
                      flows:
                        implicit:
                          authorizationUrl: https://example.com/auth
                          scopes: {}
                """, DocumentFormat.YAML);
        SecurityScheme scheme = document.getComponents().getSecuritySchemes().getScheme("Odd");

        scheme.setType("openIdConnect");

        var node = document.getRoot().get("components").get("securitySchemes").get("Odd");
        assertAll(
                () -> assertFalse(node.has("name"), "apiKey's name survived the switch"),
                () -> assertFalse(node.has("in"), "apiKey's in survived the switch"),
                () -> assertTrue(node.has("x-internal"), "an extension was dropped"),
                () -> assertTrue(node.has("flows"), "oauth2 flows were dropped"));
    }

    @Test
    void clearingAFieldRemovesItRatherThanWritingNull() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        SecurityScheme scheme = document.getComponents().getSecuritySchemes().addScheme("Auth");
        scheme.setType("apiKey");
        scheme.setName("X-API-Key");

        scheme.setName(null);

        assertFalse(document.getRoot().get("components").get("securitySchemes").get("Auth").has("name"));
    }

    @Test
    void theEditableTypesAreTheThreeSimpleOnes() {
        assertEquals(List.of("apiKey", "http", "openIdConnect"), SecurityScheme.EDITABLE_TYPES);
    }

    private static SecurityScheme newScheme() {
        return OasDocument.newDocument(DocumentFormat.YAML)
                .getComponents().getSecuritySchemes().addScheme("Auth");
    }
}
