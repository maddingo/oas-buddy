package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.DocumentWriter;
import no.maddin.oasbuddy.core.document.OasDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResponseLinksTest {

    private final ObjectNode node = JsonNodeFactory.instance.objectNode();
    private final ApiResponse response = new ApiResponse(node);

    @Test
    void readingNeverAddsTheKey() {
        assertTrue(response.getLinks().names().isEmpty());
        assertNull(response.getLinks().get("GetPet"));
        assertFalse(node.has("links"));
        Link link = new Link(JsonNodeFactory.instance.objectNode());
        assertTrue(link.getParameters().names().isEmpty());
        assertNull(link.getServer());
        assertTrue(link.isEmpty());
    }

    @Test
    void theKeyGoesWithTheLastLink() {
        ResponseLinks links = response.getLinks();
        links.add("A");
        links.add("B");

        links.remove("A");
        assertTrue(node.has("links"));
        links.remove("B");
        assertFalse(node.has("links"));
    }

    @Test
    void aLinkCarriesATargetParametersAndARequestBody() {
        Link link = response.getLinks().add("GetPet");
        link.setOperationId("getPet");
        link.setDescription("The pet just created");
        link.getParameters().put("petId", TextNode.valueOf("$response.body#/id"));
        link.setRequestBody(TextNode.valueOf("$request.body"));

        ObjectNode written = (ObjectNode) node.get("links").get("GetPet");
        assertEquals("getPet", written.get("operationId").asText());
        assertEquals("$response.body#/id", written.get("parameters").get("petId").asText());
        assertEquals("$request.body", written.get("requestBody").asText());

        link.getParameters().remove("petId");
        assertFalse(written.has("parameters"));
        link.setRequestBody(null);
        assertFalse(written.has("requestBody"));
    }

    @Test
    void parameterRenameKeepsPositionAndRefusesACollision() {
        Link link = response.getLinks().add("L");
        link.getParameters().put("a", TextNode.valueOf("1"));
        link.getParameters().put("b", TextNode.valueOf("2"));
        link.getParameters().put("c", TextNode.valueOf("3"));

        assertTrue(link.getParameters().rename("b", "z"));
        assertEquals(List.of("a", "z", "c"), link.getParameters().names());
        assertFalse(link.getParameters().rename("z", "a"));
    }

    @Test
    void renamingAndSwitchingKeepPositions() {
        ResponseLinks links = response.getLinks();
        links.add("A");
        links.add("B").setOperationId("mine");
        links.add("C");

        assertTrue(links.rename("B", "Z"));
        assertEquals(List.of("A", "Z", "C"), links.names());
        assertFalse(links.rename("Z", "A"));

        links.referTo("Z", "Shared");
        assertEquals("Shared", links.get("Z").getReferencedLinkName());
        assertEquals("#/components/links/Shared", links.get("Z").getRef());

        Link template = new Link(JsonNodeFactory.instance.objectNode().put("operationId", "fromComponent"));
        links.defineInline("Z", template);
        assertFalse(links.get("Z").isReference());
        assertEquals("fromComponent", links.get("Z").getOperationId());
        assertEquals(List.of("A", "Z", "C"), links.names());
    }

    @Test
    void aLinkThatIsNotAnObjectIsListedButNotReadable() throws IOException {
        OasDocument document = DocumentReader.read(
                "{\"openapi\":\"3.0.3\",\"paths\":{\"/a\":{\"get\":{\"responses\":{\"200\":"
                        + "{\"description\":\"ok\",\"links\":{\"Odd\":\"~\"}}}}}}}", DocumentFormat.JSON);
        ApiResponse ok = document.getPaths().getPathItem("/a").getOperation(HttpMethod.GET)
                .getResponses().getResponse("200");

        assertEquals(List.of("Odd"), ok.getLinks().names());
        assertNull(ok.getLinks().get("Odd"));
    }

    @Test
    void operationIdsAreCollectedFromEveryPath() throws IOException {
        OasDocument document = DocumentReader.read("""
                openapi: 3.0.3
                paths:
                  /pets:
                    get: {operationId: listPets}
                    post: {summary: no id}
                  /odd: ~
                  /pets/{id}:
                    get: {operationId: getPet}
                """, DocumentFormat.YAML);

        assertEquals(List.of("listPets", "getPet"), OperationIds.all(document));
    }

    @Test
    void componentLinksAreLazyAndRoundTripThroughYamlAndJson() throws IOException {
        for (DocumentFormat format : DocumentFormat.values()) {
            OasDocument document = OasDocument.newDocument(format);
            assertTrue(document.getComponents().getLinks().names().isEmpty());
            assertFalse(document.getRoot().has("components"));

            Link shared = document.getComponents().getLinks().addLink("GetPet");
            shared.setOperationId("getPet");
            shared.getParameters().put("petId", TextNode.valueOf("$response.body#/id"));
            var ok = document.getPaths().addPath("/a").addOperation(HttpMethod.GET)
                    .getResponses().addResponse("200");
            ok.getLinks().referTo("pet", "GetPet");
            ok.getLinks().add("other").setOperationRef("#/paths/~1a/get");

            OasDocument reloaded = DocumentReader.read(DocumentWriter.write(document), format);

            assertEquals(document.getRoot(), reloaded.getRoot());
            assertEquals(List.of("GetPet"), reloaded.getComponents().getLinks().names());
        }
    }
}
