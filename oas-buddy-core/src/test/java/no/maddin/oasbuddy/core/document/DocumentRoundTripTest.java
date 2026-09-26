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
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void schemaStructureRoundTripsInYaml() throws IOException {
        assertRoundTrips("structured.yaml", DocumentFormat.YAML);
    }

    @Test
    void schemaStructureRoundTripsInJson() throws IOException {
        assertRoundTrips("structured.json", DocumentFormat.JSON);
    }

    @Test
    void reusableComponentsRoundTripInYaml() throws IOException {
        assertRoundTrips("reusable.yaml", DocumentFormat.YAML);
    }

    @Test
    void reusableComponentsRoundTripInJson() throws IOException {
        assertRoundTrips("reusable.json", DocumentFormat.JSON);
    }

    /** Component responses and references to them, built through the facade, round-trip in order. */
    @Test
    void componentResponsesBuiltThroughTheFacadeRoundTripInYaml() throws IOException {
        assertComponentResponsesRoundTrip(DocumentFormat.YAML);
    }

    @Test
    void componentResponsesBuiltThroughTheFacadeRoundTripInJson() throws IOException {
        assertComponentResponsesRoundTrip(DocumentFormat.JSON);
    }

    private static void assertComponentResponsesRoundTrip(DocumentFormat format) throws IOException {
        OasDocument original = OasDocument.newDocument(format);
        original.getComponents().getResponses().addResponse("NotFound").setDescription("Not found");
        var responses = original.getPaths().addPath("/pets")
                .addOperation(no.maddin.oasbuddy.core.model.HttpMethod.GET).getResponses();
        responses.addResponse("200").setDescription("OK");
        responses.referTo("404", "NotFound");

        OasDocument reloaded = DocumentReader.read(DocumentWriter.write(original), format);

        assertEquals(fieldOrder(original.getRoot()), fieldOrder(reloaded.getRoot()));
        assertEquals(original.getRoot(), reloaded.getRoot());
        assertEquals("NotFound", reloaded.getPaths().getPathItem("/pets")
                .getOperation(no.maddin.oasbuddy.core.model.HttpMethod.GET)
                .getResponses().getResponse("404").getReferencedResponseName());
    }

    /** Component parameters, path-level parameters and references to both, built through the facade. */
    @Test
    void parametersBuiltThroughTheFacadeRoundTripInYaml() throws IOException {
        assertParametersRoundTrip(DocumentFormat.YAML);
    }

    @Test
    void parametersBuiltThroughTheFacadeRoundTripInJson() throws IOException {
        assertParametersRoundTrip(DocumentFormat.JSON);
    }

    private static void assertParametersRoundTrip(DocumentFormat format) throws IOException {
        OasDocument original = OasDocument.newDocument(format);
        original.getComponents().getParameters().addParameter("Limit", "limit", "query")
                .getSchema().setType("integer");
        var path = original.getPaths().addPath("/pets/{petId}");
        var petId = path.getParameters().add("petId", "path");
        petId.setRequired(true);
        petId.getSchema().setType("string");
        path.getParameters().addReference("Limit");
        var get = path.addOperation(no.maddin.oasbuddy.core.model.HttpMethod.GET);
        get.getParameters().addReference("Limit");
        get.getParameters().add("sort", "query");

        OasDocument reloaded = DocumentReader.read(DocumentWriter.write(original), format);

        assertEquals(fieldOrder(original.getRoot()), fieldOrder(reloaded.getRoot()));
        assertEquals(original.getRoot(), reloaded.getRoot());
        assertEquals("Limit", reloaded.getPaths().getPathItem("/pets/{petId}").getParameters().all().get(1)
                .getReferencedParameterName());
    }

    /** Root tags and an operation's own tags, built through the facade, round-trip in order. */
    @Test
    void tagsRoundTripInYaml() throws IOException {
        assertTagsRoundTrip(DocumentFormat.YAML);
    }

    @Test
    void tagsRoundTripInJson() throws IOException {
        assertTagsRoundTrip(DocumentFormat.JSON);
    }

    private static void assertTagsRoundTrip(DocumentFormat format) throws IOException {
        OasDocument original = OasDocument.newDocument(format);
        original.getTags().add("pets").setDescription("Everything about pets");
        original.getTags().add("store");
        original.getPaths().addPath("/pets").addOperation(no.maddin.oasbuddy.core.model.HttpMethod.GET)
                .setTags(List.of("store", "pets"));

        OasDocument reloaded = DocumentReader.read(DocumentWriter.write(original), format);

        assertEquals(fieldOrder(original.getRoot()), fieldOrder(reloaded.getRoot()));
        assertEquals(original.getRoot(), reloaded.getRoot());
        assertEquals(List.of("pets", "store"), reloaded.getTags().names());
        assertEquals("Everything about pets", reloaded.getTags().get("pets").getDescription());
        assertEquals(List.of("store", "pets"),
                reloaded.getPaths().getPathItem("/pets")
                        .getOperation(no.maddin.oasbuddy.core.model.HttpMethod.GET).getTags());
    }

    /**
     * Array items, property references and both forms of {@code additionalProperties}, built
     * through the facade, must come out exactly as the fixture that states them by hand.
     */
    @Test
    void schemaStructureBuiltThroughTheFacadeMatchesTheFixture() throws IOException {
        OasDocument built = OasDocument.newDocument(DocumentFormat.YAML);
        var schemas = built.getComponents().getSchemas();

        var pets = schemas.addSchema("Pets");
        pets.changeTypeTo("array");
        pets.createItems().referTo("Pet");

        var pet = schemas.addSchema("Pet");
        pet.changeTypeTo("object");
        pet.setRequired(List.of("id", "name"));
        var id = pet.addProperty("id");
        id.changeTypeTo("integer");
        id.setFormat("int64");
        pet.addProperty("name").changeTypeTo("string");
        var tags = pet.addProperty("tags");
        tags.changeTypeTo("array");
        tags.createItems().changeTypeTo("string");
        pet.addProperty("owner").referTo("Owner");
        var friends = pet.addProperty("friends");
        friends.changeTypeTo("array");
        friends.createItems().referTo("Pet");
        pet.setAdditionalPropertiesAllowed(false);

        var owner = schemas.addSchema("Owner");
        owner.changeTypeTo("object");
        owner.addProperty("name").changeTypeTo("string");
        owner.createAdditionalPropertiesSchema().changeTypeTo("string");

        var labels = schemas.addSchema("Labels");
        labels.changeTypeTo("object");
        labels.createAdditionalPropertiesSchema().referTo("Owner");

        JsonNode expected = loadFixture("structured.yaml", DocumentFormat.YAML).getRoot().get("components");
        OasDocument reloaded = DocumentReader.read(DocumentWriter.write(built), DocumentFormat.YAML);

        assertEquals(fieldOrder(expected), fieldOrder(reloaded.getRoot().get("components")));
        assertEquals(expected, reloaded.getRoot().get("components"));
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

    @Test
    void editedSecurityRoundTripsInYaml() throws IOException {
        assertEditedSecurityRoundTrips("secured.yaml", DocumentFormat.YAML);
    }

    @Test
    void editedSecurityRoundTripsInJson() throws IOException {
        assertEditedSecurityRoundTrips("secured.json", DocumentFormat.JSON);
    }

    /**
     * Changing what an API requires must survive a save, including the distinction between an
     * operation that is explicitly public (an empty array) and one that inherits (no key at all).
     */
    private static void assertEditedSecurityRoundTrips(String fixture, DocumentFormat format)
            throws IOException {
        OasDocument original = loadFixture(fixture, format);
        original.getSecurity().add("OAuth2Auth").setScopes("OAuth2Auth", List.of("read:pets"));
        original.getPaths().getPathItem("/pets")
                .getOperation(no.maddin.oasbuddy.core.model.HttpMethod.GET)
                .getSecurity().declarePublic();

        OasDocument reloaded = DocumentReader.read(DocumentWriter.write(original), format);

        assertEquals(fieldOrder(original.getRoot()), fieldOrder(reloaded.getRoot()));
        assertEquals(original.getRoot(), reloaded.getRoot());
        assertEquals(List.of("read:pets"),
                reloaded.getSecurity().requirements().get(1).getScopes("OAuth2Auth"));
        assertEquals(0, reloaded.getRoot().get("paths").get("/pets").get("get").get("security").size(),
                "an explicitly public operation must stay an empty array, not vanish");
    }

    /**
     * The point of {@code JsonNodes.renameField}: a rename must show up in the saved file as one
     * changed line, not a removal plus an addition at the end.
     */
    @Test
    void renamingASchemaChangesOnlyItsOwnKeyInTheWrittenYaml() throws IOException {
        OasDocument original = loadFixture("petstore.yaml", DocumentFormat.YAML);
        List<String> before = List.of(DocumentWriter.write(original).split("\n", -1));

        boolean renamed = original.getComponents().getSchemas().renameSchema("NewPet", "PetDraft");

        List<String> after = List.of(DocumentWriter.write(original).split("\n", -1));
        assertTrue(renamed);
        assertEquals(before.size(), after.size(), "a rename must not add or remove any line");

        List<String> changedLines = changedLines(before, after);
        assertEquals(List.of("    PetDraft:"), changedLines,
                "only the schema's own key should change, e.g. not move to the end of the map");
    }

    /** Renaming a schema and rewriting its references together, as the desktop rename flow does. */
    @Test
    void renamingASchemaAndRewritingItsReferencesRoundTrips() throws IOException {
        OasDocument original = loadFixture("petstore.yaml", DocumentFormat.YAML);

        original.getComponents().getSchemas().renameSchema("Pet", "Animal");
        int rewritten = no.maddin.oasbuddy.core.model.SchemaReferences.rewrite(original, "Pet", "Animal");

        OasDocument reloaded = DocumentReader.read(DocumentWriter.write(original), DocumentFormat.YAML);
        assertEquals(2, rewritten);
        assertEquals(fieldOrder(original.getRoot()), fieldOrder(reloaded.getRoot()));
        assertEquals(original.getRoot(), reloaded.getRoot());
        assertTrue(no.maddin.oasbuddy.core.model.SchemaReferences.find(reloaded, "Pet").isEmpty());
        assertEquals(List.of(
                        "paths → /pets → get → responses → 200 → content → application/json → schema → items",
                        "paths → /pets/{petId} → get → responses → 200 → content → application/json → schema"),
                no.maddin.oasbuddy.core.model.SchemaReferences.find(reloaded, "Animal"));
    }

    private static List<String> changedLines(List<String> before, List<String> after) {
        List<String> changed = new ArrayList<>();
        for (int i = 0; i < before.size(); i++) {
            if (!before.get(i).equals(after.get(i))) {
                changed.add(after.get(i));
            }
        }
        return changed;
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
