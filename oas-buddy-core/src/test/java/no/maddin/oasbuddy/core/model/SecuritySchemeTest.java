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
     * The strip list is the union of the editable types' own fields and nothing else, so anything
     * the editor does not understand survives whatever happens around it — an {@code x-} extension,
     * or a field belonging to a type this editor has no form for. It must never quietly drop what it
     * cannot edit. ({@code flows} is no longer in that category: oauth2 is editable, so it is owned
     * and is stripped when the type changes — see {@link #switchingAwayFromOauth2DropsItsFlows()}.)
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
                      x-audit-owner: platform-team
                """, DocumentFormat.YAML);
        SecurityScheme scheme = document.getComponents().getSecuritySchemes().getScheme("Odd");

        scheme.setType("openIdConnect");

        var node = document.getRoot().get("components").get("securitySchemes").get("Odd");
        assertAll(
                () -> assertFalse(node.has("name"), "apiKey's name survived the switch"),
                () -> assertFalse(node.has("in"), "apiKey's in survived the switch"),
                () -> assertTrue(node.has("x-internal"), "an extension was dropped"),
                () -> assertTrue(node.has("x-audit-owner"), "an extension was dropped"));
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
    void theEditableTypesAreTheFourWithAForm() {
        assertEquals(List.of("apiKey", "http", "oauth2", "openIdConnect"), SecurityScheme.EDITABLE_TYPES);
    }

    @Test
    void switchingAwayFromOauth2DropsItsFlows() {
        OasDocument document = DocumentReader.read("""
                openapi: 3.0.3
                info:
                  title: Secured
                  version: 1.0.0
                paths: {}
                components:
                  securitySchemes:
                    OAuth:
                      type: oauth2
                      flows:
                        implicit:
                          authorizationUrl: https://example.com/auth
                          scopes: {}
                """, DocumentFormat.YAML);
        SecurityScheme scheme = document.getComponents().getSecuritySchemes().getScheme("OAuth");

        scheme.setType("apiKey");

        assertFalse(document.getRoot().get("components").get("securitySchemes").get("OAuth").has("flows"),
                "flows belong to oauth2 and should go with it");
    }

    @Test
    void switchingToOauth2DropsTheFlatTypesFields() {
        SecurityScheme scheme = newScheme();
        scheme.setType("apiKey");
        scheme.setName("X-API-Key");
        scheme.setIn("header");

        scheme.setType("oauth2");

        assertAll(
                () -> assertNull(scheme.getName()),
                () -> assertNull(scheme.getIn()),
                () -> assertTrue(scheme.getFlows().isEmpty(), "switching type should not invent flows"));
    }

    /**
     * What a requirement on this scheme may ask for. The union across flows, because a requirement
     * names the scheme, not one of its flows, and a scope defined by any flow is legitimate.
     */
    @Test
    void anOauth2SchemeOffersTheScopesItsFlowsDeclare() {
        SecurityScheme scheme = newScheme();
        scheme.setType("oauth2");
        OAuthFlow implicit = scheme.getFlows().addFlow("implicit");
        implicit.setScope("read:pets", "read your pets");
        implicit.setScope("write:pets", "modify your pets");
        OAuthFlow password = scheme.getFlows().addFlow("password");
        password.setScope("write:pets", "modify your pets");
        password.setScope("admin", "everything");

        assertEquals(List.of("read:pets", "write:pets", "admin"), scheme.declaredScopes());
    }

    @Test
    void aSchemeThatIsNotOauth2DeclaresNoScopes() {
        SecurityScheme scheme = newScheme();
        scheme.setType("apiKey");

        assertEquals(List.of(), scheme.declaredScopes());
    }

    private static SecurityScheme newScheme() {
        return OasDocument.newDocument(DocumentFormat.YAML)
                .getComponents().getSecuritySchemes().addScheme("Auth");
    }
}
