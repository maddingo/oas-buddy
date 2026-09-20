package no.maddin.oasbuddy.core.document;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentRoundTripTest {

    @Test
    void yamlRoundTripPreservesKeyOrder() throws IOException {
        OasDocument original = loadFixture("petstore.yaml", DocumentFormat.YAML);
        String written = DocumentWriter.write(original);
        OasDocument reloaded = DocumentReader.read(written, DocumentFormat.YAML);

        assertEquals(fieldOrder(original.getRoot()), fieldOrder(reloaded.getRoot()));
        assertEquals(original.getRoot(), reloaded.getRoot());
    }

    @Test
    void jsonRoundTripPreservesKeyOrder() throws IOException {
        OasDocument original = loadFixture("petstore.json", DocumentFormat.JSON);
        String written = DocumentWriter.write(original);
        OasDocument reloaded = DocumentReader.read(written, DocumentFormat.JSON);

        assertEquals(fieldOrder(original.getRoot()), fieldOrder(reloaded.getRoot()));
        assertEquals(original.getRoot(), reloaded.getRoot());
    }

    @Test
    void securitySchemesRoundTripInYaml() throws IOException {
        assertRoundTrips("secured.yaml", DocumentFormat.YAML);
    }

    @Test
    void securitySchemesRoundTripInJson() throws IOException {
        assertRoundTrips("secured.json", DocumentFormat.JSON);
    }

    /**
     * Regression guard rather than a driver: the editor cannot edit an oauth2 scheme, so the one
     * thing it owes such a scheme is to leave it exactly as loaded even while the schemes around it
     * are edited. Nothing in the facade touches it today; this fails the day something does.
     */
    @Test
    void editingOneSchemeLeavesAnOauth2SchemeExactlyAsLoaded() throws IOException {
        OasDocument original = loadFixture("secured.yaml", DocumentFormat.YAML);
        JsonNode oauth2AsLoaded = schemes(original).get("OAuth2Auth").deepCopy();

        original.getComponents().getSecuritySchemes().getScheme("ApiKeyAuth").setType("http");
        OasDocument reloaded = DocumentReader.read(DocumentWriter.write(original), DocumentFormat.YAML);

        assertEquals(oauth2AsLoaded, schemes(reloaded).get("OAuth2Auth"));
        assertEquals(List.of("ApiKeyAuth", "BearerAuth", "OidcAuth", "OAuth2Auth"),
                fieldNames(schemes(reloaded)));
    }

    /**
     * Editing an oauth2 flow must append rather than reshuffle: a new URL or scope goes after what
     * is already there, so the diff of a saved file shows only what the user actually changed.
     */
    @Test
    void editingAnOauthFlowAppendsWithoutReorderingTheFile() throws IOException {
        OasDocument original = loadFixture("secured.yaml", DocumentFormat.YAML);
        var flow = original.getComponents().getSecuritySchemes().getScheme("OAuth2Auth")
                .getFlows().getFlow("authorizationCode");

        flow.setUrl(no.maddin.oasbuddy.core.model.OAuthFlowUrl.REFRESH_URL, "https://example.com/refresh");
        flow.setScope("write:pets", "modify your pets");

        OasDocument reloaded = DocumentReader.read(DocumentWriter.write(original), DocumentFormat.YAML);
        JsonNode reloadedFlow = schemes(reloaded).get("OAuth2Auth").get("flows").get("authorizationCode");

        assertEquals(List.of("authorizationUrl", "tokenUrl", "scopes", "refreshUrl"),
                fieldNames(reloadedFlow));
        assertEquals(List.of("read:pets", "write:pets"), fieldNames(reloadedFlow.get("scopes")));
    }

    private static void assertRoundTrips(String fixture, DocumentFormat format) throws IOException {
        OasDocument original = loadFixture(fixture, format);
        OasDocument reloaded = DocumentReader.read(DocumentWriter.write(original), format);

        assertEquals(fieldOrder(original.getRoot()), fieldOrder(reloaded.getRoot()));
        assertEquals(original.getRoot(), reloaded.getRoot());
    }

    private static JsonNode schemes(OasDocument document) {
        return document.getRoot().get("components").get("securitySchemes");
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    private static OasDocument loadFixture(String name, DocumentFormat format) throws IOException {
        try (InputStream is = DocumentRoundTripTest.class.getResourceAsStream("/fixtures/" + name)) {
            if (is == null) {
                throw new IOException("Fixture not found: " + name);
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return DocumentReader.read(content, format);
        }
    }

    private static List<String> fieldOrder(JsonNode node) {
        List<String> order = new ArrayList<>();
        collectFieldOrder(node, order);
        return order;
    }

    private static void collectFieldOrder(JsonNode node, List<String> order) {
        if (node instanceof ObjectNode objectNode) {
            Iterator<String> names = objectNode.fieldNames();
            while (names.hasNext()) {
                String name = names.next();
                order.add(name);
                collectFieldOrder(objectNode.get(name), order);
            }
        } else if (node instanceof ArrayNode arrayNode) {
            for (JsonNode element : arrayNode) {
                collectFieldOrder(element, order);
            }
        }
    }
}
