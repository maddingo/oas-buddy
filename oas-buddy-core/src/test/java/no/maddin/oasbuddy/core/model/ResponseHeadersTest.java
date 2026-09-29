package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

class ResponseHeadersTest {

    private final ObjectNode node = JsonNodeFactory.instance.objectNode();
    private final ApiResponse response = new ApiResponse(node);

    @Test
    void readingNeverAddsTheKey() {
        assertTrue(response.getHeaders().names().isEmpty());
        assertNull(response.getHeaders().get("X-Rate-Limit"));
        assertFalse(node.has("headers"));
    }

    @Test
    void theKeyGoesWithTheLastHeader() {
        ResponseHeaders headers = response.getHeaders();
        headers.add("Location");
        headers.add("X-Total");

        headers.remove("Location");
        assertTrue(node.has("headers"));
        headers.remove("X-Total");
        assertFalse(node.has("headers"));
    }

    @Test
    void aHeaderHasADescriptionARequiredFlagAndASchema() {
        Header header = response.getHeaders().add("X-Rate-Limit");
        header.setDescription("Calls left");
        header.setRequired(true);
        header.getSchema().setType("integer");

        ObjectNode written = (ObjectNode) node.get("headers").get("X-Rate-Limit");
        assertEquals("Calls left", written.get("description").asText());
        assertEquals("integer", written.get("schema").get("type").asText());
        assertFalse(written.has("name"));
        assertFalse(written.has("in"));
    }

    @Test
    void renamingKeepsThePositionAndRefusesACollision() {
        ResponseHeaders headers = response.getHeaders();
        headers.add("A");
        headers.add("B");
        headers.add("C");

        assertTrue(headers.rename("B", "Z"));
        assertEquals(List.of("A", "Z", "C"), headers.names());
        assertFalse(headers.rename("Z", "A"));
    }

    @Test
    void switchingBetweenInlineAndReferenceReplacesTheEntryInPlace() {
        ResponseHeaders headers = response.getHeaders();
        headers.add("A");
        headers.add("B").setDescription("mine");
        headers.add("C");

        headers.referTo("B", "Shared");
        assertEquals(List.of("A", "B", "C"), headers.names());
        assertEquals("Shared", headers.get("B").getReferencedHeaderName());
        assertEquals("#/components/headers/Shared", headers.get("B").getRef());

        Header template = new Header(JsonNodeFactory.instance.objectNode().put("description", "from component"));
        Header inline = headers.defineInline("B", template);
        assertFalse(inline.isReference());
        assertEquals("from component", headers.get("B").getDescription());
        assertEquals(List.of("A", "B", "C"), headers.names());
    }

    @Test
    void aHeaderThatIsNotAnObjectIsListedButNotReadable() throws IOException {
        OasDocument document = DocumentReader.read(
                "{\"openapi\":\"3.0.3\",\"paths\":{\"/a\":{\"get\":{\"responses\":{\"200\":"
                        + "{\"description\":\"ok\",\"headers\":{\"X-Odd\":\"~\"}}}}}}}", DocumentFormat.JSON);
        ApiResponse ok = document.getPaths().getPathItem("/a").getOperation(HttpMethod.GET)
                .getResponses().getResponse("200");

        assertEquals(List.of("X-Odd"), ok.getHeaders().names());
        assertNull(ok.getHeaders().get("X-Odd"));
    }

    @Test
    void componentHeadersAreLazyAndRoundTripThroughYamlAndJson() throws IOException {
        for (DocumentFormat format : DocumentFormat.values()) {
            OasDocument document = OasDocument.newDocument(format);
            assertTrue(document.getComponents().getHeaders().names().isEmpty());
            assertFalse(document.getRoot().has("components"));

            document.getComponents().getHeaders().addHeader("Rate").setDescription("shared");
            var ok = document.getPaths().addPath("/a").addOperation(HttpMethod.GET)
                    .getResponses().addResponse("200");
            ok.getHeaders().referTo("X-Rate", "Rate");
            ok.getHeaders().add("Location").getSchema().setType("string");

            OasDocument reloaded = DocumentReader.read(DocumentWriter.write(document), format);

            assertEquals(document.getRoot(), reloaded.getRoot());
            assertEquals(List.of("Rate"), reloaded.getComponents().getHeaders().names());
        }
    }
}
