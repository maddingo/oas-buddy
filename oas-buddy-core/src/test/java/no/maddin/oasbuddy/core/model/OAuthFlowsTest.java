package no.maddin.oasbuddy.core.model;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.OasDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The {@code flows} object of an oauth2 scheme. */
class OAuthFlowsTest {

    @Test
    void theFourFlowNamesAreTheOnesTheSpecDefines() {
        assertEquals(List.of("implicit", "password", "clientCredentials", "authorizationCode"),
                OAuthFlows.FLOW_NAMES);
    }

    /**
     * Which URLs a flow carries is spec knowledge, and it lives here so the editor renders whatever
     * core says instead of hardcoding the spec a second time.
     */
    @Test
    void eachFlowDefinesOnlyTheUrlsTheSpecGivesIt() {
        assertAll(
                () -> assertEquals(List.of(OAuthFlowUrl.AUTHORIZATION_URL, OAuthFlowUrl.REFRESH_URL),
                        OAuthFlows.urlsOf("implicit")),
                () -> assertEquals(List.of(OAuthFlowUrl.TOKEN_URL, OAuthFlowUrl.REFRESH_URL),
                        OAuthFlows.urlsOf("password")),
                () -> assertEquals(List.of(OAuthFlowUrl.TOKEN_URL, OAuthFlowUrl.REFRESH_URL),
                        OAuthFlows.urlsOf("clientCredentials")),
                () -> assertEquals(List.of(OAuthFlowUrl.AUTHORIZATION_URL, OAuthFlowUrl.TOKEN_URL,
                                OAuthFlowUrl.REFRESH_URL),
                        OAuthFlows.urlsOf("authorizationCode")));
    }

    @Test
    void anUnknownFlowNameDefinesNoUrls() {
        assertEquals(List.of(), OAuthFlows.urlsOf("notAFlow"));
    }

    @Test
    void flowsAreReadFromALoadedDocument() throws IOException {
        OAuthFlows flows = oauth2Scheme().getFlows();

        assertAll(
                () -> assertEquals(List.of("authorizationCode"), flows.names()),
                () -> assertEquals("https://example.com/auth",
                        flows.getFlow("authorizationCode").getUrl(OAuthFlowUrl.AUTHORIZATION_URL)),
                () -> assertEquals("https://example.com/token",
                        flows.getFlow("authorizationCode").getUrl(OAuthFlowUrl.TOKEN_URL)));
    }

    @Test
    void anAbsentFlowHasNoNode() throws IOException {
        assertNull(oauth2Scheme().getFlows().getFlow("implicit"));
    }

    @Test
    void addingAFlowPutsItInTheDocument() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        SecurityScheme scheme = document.getComponents().getSecuritySchemes().addScheme("OAuth");
        scheme.setType("oauth2");

        scheme.getFlows().addFlow("password").setUrl(OAuthFlowUrl.TOKEN_URL, "https://example.com/token");

        assertEquals("https://example.com/token",
                document.getRoot().get("components").get("securitySchemes").get("OAuth")
                        .get("flows").get("password").get("tokenUrl").asText());
    }

    @Test
    void removingAFlowTakesItOutAndLeavesTheOthers() throws IOException {
        OAuthFlows flows = oauth2Scheme().getFlows();
        flows.addFlow("implicit").setUrl(OAuthFlowUrl.AUTHORIZATION_URL, "https://example.com/implicit");

        flows.removeFlow("authorizationCode");

        assertEquals(List.of("implicit"), flows.names());
    }

    @Test
    void namesComeBackInDocumentOrder() {
        OAuthFlows flows = newOauth2Scheme().getFlows();

        flows.addFlow("clientCredentials");
        flows.addFlow("implicit");

        assertEquals(List.of("clientCredentials", "implicit"), flows.names());
    }

    @Test
    void readingFlowsNeverAddsThemToTheDocument() {
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
                """, DocumentFormat.YAML);
        SecurityScheme scheme = document.getComponents().getSecuritySchemes().getScheme("OAuth");

        assertAll(
                () -> assertEquals(List.of(), scheme.getFlows().names()),
                () -> assertTrue(scheme.getFlows().isEmpty()),
                () -> assertFalse(document.getRoot().get("components").get("securitySchemes")
                        .get("OAuth").has("flows"), "reading added a flows object"));
    }

    @Test
    void flowsHoldingAFlowAreNotEmpty() throws IOException {
        assertFalse(oauth2Scheme().getFlows().isEmpty());
    }

    private static SecurityScheme newOauth2Scheme() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        SecurityScheme scheme = document.getComponents().getSecuritySchemes().addScheme("OAuth");
        scheme.setType("oauth2");
        return scheme;
    }

    private static SecurityScheme oauth2Scheme() throws IOException {
        try (InputStream is = OAuthFlowsTest.class.getResourceAsStream("/fixtures/secured.yaml")) {
            if (is == null) {
                throw new IOException("fixture not found");
            }
            OasDocument document = DocumentReader.read(
                    new String(is.readAllBytes(), StandardCharsets.UTF_8), DocumentFormat.YAML);
            return document.getComponents().getSecuritySchemes().getScheme("OAuth2Auth");
        }
    }
}
