package no.maddin.oasbuddy.core.model;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** One oauth2 flow: its URLs and its scope map. */
class OAuthFlowTest {

    @Test
    void urlsWriteThroughToTheTree() {
        OAuthFlow flow = newFlow("authorizationCode");

        flow.setUrl(OAuthFlowUrl.AUTHORIZATION_URL, "https://example.com/auth");
        flow.setUrl(OAuthFlowUrl.TOKEN_URL, "https://example.com/token");
        flow.setUrl(OAuthFlowUrl.REFRESH_URL, "https://example.com/refresh");

        assertAll(
                () -> assertEquals("https://example.com/auth", flow.getUrl(OAuthFlowUrl.AUTHORIZATION_URL)),
                () -> assertEquals("https://example.com/token", flow.getUrl(OAuthFlowUrl.TOKEN_URL)),
                () -> assertEquals("https://example.com/refresh", flow.getUrl(OAuthFlowUrl.REFRESH_URL)));
    }

    @Test
    void clearingAUrlRemovesItRatherThanWritingNull() {
        OAuthFlow flow = newFlow("implicit");
        flow.setUrl(OAuthFlowUrl.AUTHORIZATION_URL, "https://example.com/auth");

        flow.setUrl(OAuthFlowUrl.AUTHORIZATION_URL, null);

        assertNull(flow.getUrl(OAuthFlowUrl.AUTHORIZATION_URL));
    }

    @Test
    void scopesAreANamedMapOfDescriptions() {
        OAuthFlow flow = newFlow("implicit");

        flow.setScope("read:pets", "read your pets");
        flow.setScope("write:pets", "modify your pets");

        assertAll(
                () -> assertEquals(List.of("read:pets", "write:pets"), flow.scopeNames()),
                () -> assertEquals("read your pets", flow.getScopeDescription("read:pets")));
    }

    @Test
    void settingAnExistingScopeUpdatesItsDescriptionInPlace() {
        OAuthFlow flow = newFlow("implicit");
        flow.setScope("read:pets", "read your pets");
        flow.setScope("write:pets", "modify your pets");

        flow.setScope("read:pets", "read everything");

        assertAll(
                () -> assertEquals("read everything", flow.getScopeDescription("read:pets")),
                () -> assertEquals(List.of("read:pets", "write:pets"), flow.scopeNames()));
    }

    @Test
    void removingAScopeTakesItOut() {
        OAuthFlow flow = newFlow("implicit");
        flow.setScope("read:pets", "read your pets");
        flow.setScope("write:pets", "modify your pets");

        flow.removeScope("read:pets");

        assertEquals(List.of("write:pets"), flow.scopeNames());
    }

    /**
     * Jackson has no rename, and remove-then-add would move the scope to the end of the map — every
     * rename would reorder the saved file. The whole point of the ordered tree is that it does not.
     */
    @Test
    void renamingAScopeKeepsItsPositionAndDescription() {
        OAuthFlow flow = newFlow("implicit");
        flow.setScope("read:pets", "read your pets");
        flow.setScope("write:pets", "modify your pets");
        flow.setScope("admin", "everything");

        flow.renameScope("write:pets", "write:animals");

        assertAll(
                () -> assertEquals(List.of("read:pets", "write:animals", "admin"), flow.scopeNames()),
                () -> assertEquals("modify your pets", flow.getScopeDescription("write:animals")));
    }

    @Test
    void renamingOntoAnExistingScopeChangesNothing() {
        OAuthFlow flow = newFlow("implicit");
        flow.setScope("read:pets", "read your pets");
        flow.setScope("write:pets", "modify your pets");

        flow.renameScope("read:pets", "write:pets");

        assertAll(
                () -> assertEquals(List.of("read:pets", "write:pets"), flow.scopeNames()),
                () -> assertEquals("read your pets", flow.getScopeDescription("read:pets")),
                () -> assertEquals("modify your pets", flow.getScopeDescription("write:pets")));
    }

    @Test
    void renamingAScopeThatIsNotThereChangesNothing() {
        OAuthFlow flow = newFlow("implicit");
        flow.setScope("read:pets", "read your pets");

        flow.renameScope("nope", "something");

        assertEquals(List.of("read:pets"), flow.scopeNames());
    }

    /** Drives whether turning a flow off needs to ask first. */
    @Test
    void aFlowIsEmptyUntilItHasAUrlOrAScope() {
        OAuthFlow bare = newFlow("implicit");
        OAuthFlow withUrl = newFlow("implicit");
        withUrl.setUrl(OAuthFlowUrl.AUTHORIZATION_URL, "https://example.com/auth");
        OAuthFlow withScope = newFlow("password");
        withScope.setScope("read:pets", "read your pets");

        assertAll(
                () -> assertTrue(bare.isEmpty()),
                () -> assertFalse(withUrl.isEmpty()),
                () -> assertFalse(withScope.isEmpty()));
    }

    private static OAuthFlow newFlow(String flowName) {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        SecurityScheme scheme = document.getComponents().getSecuritySchemes().addScheme("OAuth");
        scheme.setType("oauth2");
        return scheme.getFlows().addFlow(flowName);
    }
}
