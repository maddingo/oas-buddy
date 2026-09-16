package no.maddin.oasbuddy.core.model;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.OasDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OasDocumentFacadeTest {

    @Test
    void readsInfoServersPathsAndSchemasFromFixture() throws IOException {
        OasDocument document = loadFixture("petstore.yaml");

        Info info = document.getInfo();
        assertEquals("Petstore", info.getTitle());
        assertEquals("1.0.0", info.getVersion());
        assertEquals("API Support", info.getContact().getName());
        assertEquals("Apache 2.0", info.getLicense().getName());

        List<Server> servers = document.getServers();
        assertEquals(1, servers.size());
        assertEquals("https://api.example.com/v1", servers.get(0).getUrl());

        Paths paths = document.getPaths();
        assertTrue(paths.pathNames().contains("/pets"));
        assertTrue(paths.pathNames().contains("/pets/{petId}"));

        PathItem petsPath = paths.getPathItem("/pets");
        Operation getPets = petsPath.getOperation(HttpMethod.GET);
        assertNotNull(getPets);
        assertEquals("listPets", getPets.getOperationId());
        assertEquals(List.of("pets"), getPets.getTags());

        Operation postPets = petsPath.getOperation(HttpMethod.POST);
        RequestBody requestBody = postPets.getRequestBody();
        assertTrue(requestBody.isRequired());
        assertEquals("#/components/schemas/NewPet", requestBody.getSchema("application/json").getRef());

        Schema pet = document.getComponents().getSchemas().getSchema("Pet");
        assertEquals("object", pet.getType());
        assertEquals(List.of("id", "name"), pet.getRequired());
        assertEquals("integer", pet.getProperty("id").getType());
    }

    @Test
    void mutatingFacadeWritesThroughToUnderlyingTree() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);

        document.getInfo().setTitle("My API");
        assertEquals("My API", document.getRoot().get("info").get("title").asText());

        Server server = document.addServer("https://example.com");
        server.setDescription("Example server");
        assertEquals("https://example.com", document.getRoot().get("servers").get(0).get("url").asText());

        PathItem pathItem = document.getPaths().addPath("/widgets");
        Operation operation = pathItem.addOperation(HttpMethod.GET);
        operation.setOperationId("listWidgets");
        assertEquals("listWidgets",
                document.getRoot().get("paths").get("/widgets").get("get").get("operationId").asText());

        Schema schema = document.getComponents().getSchemas().addSchema("Widget");
        schema.setType("object");
        schema.addProperty("name").setType("string");
        assertEquals("string",
                document.getRoot().get("components").get("schemas").get("Widget")
                        .get("properties").get("name").get("type").asText());
    }

    private static OasDocument loadFixture(String name) throws IOException {
        try (InputStream is = OasDocumentFacadeTest.class.getResourceAsStream("/fixtures/" + name)) {
            if (is == null) {
                throw new IOException("Fixture not found: " + name);
            }
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return DocumentReader.read(content, DocumentFormat.YAML);
        }
    }
}
