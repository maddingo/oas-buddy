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
