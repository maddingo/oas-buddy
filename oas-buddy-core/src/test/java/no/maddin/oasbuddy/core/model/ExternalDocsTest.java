package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.DocumentWriter;
import no.maddin.oasbuddy.core.document.OasDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalDocsTest {

    private final ObjectNode parent = JsonNodeFactory.instance.objectNode();

    @Test
    void readingNeverAddsTheKey() {
        ExternalDocs docs = new ExternalDocs(parent);

        assertNull(docs.getUrl());
        assertNull(docs.getDescription());
        assertFalse(parent.has("externalDocs"));
    }

    @Test
    void writingCreatesTheObjectAndClearingBothFieldsRemovesTheKey() {
        ExternalDocs docs = new ExternalDocs(parent);

        docs.setUrl("https://example.com/docs");
        docs.setDescription("More");
        assertEquals("https://example.com/docs", parent.get("externalDocs").get("url").asText());

        docs.setUrl("");
        assertTrue(parent.has("externalDocs"), "description is still there");
        docs.setDescription(null);
        assertFalse(parent.has("externalDocs"));
    }

    @Test
    void clearingAFieldThatWasNeverSetLeavesNoObjectBehind() {
        new ExternalDocs(parent).setDescription("  ");

        assertFalse(parent.has("externalDocs"));
    }

    @Test
    void anExtensionKeepsTheObjectAlive() throws IOException {
        OasDocument document = DocumentReader.read(
                "{\"openapi\":\"3.0.3\",\"externalDocs\":{\"url\":\"u\",\"x-note\":1}}", DocumentFormat.JSON);

        document.getExternalDocs().setUrl(null);

        assertTrue(document.getRoot().get("externalDocs").has("x-note"));
    }

    @Test
    void anEntryThatIsNotAnObjectIsNotEditableAndIsLeftAlone() {
        parent.put("externalDocs", "oops");
        ExternalDocs docs = new ExternalDocs(parent);

        assertFalse(docs.isEditable());
        assertNull(docs.getUrl());
        docs.setUrl("https://example.com");
        assertEquals("oops", parent.get("externalDocs").asText());
    }

    @Test
    void reachableFromRootTagOperationAndSchemaAndRoundTrips() throws IOException {
        for (DocumentFormat format : DocumentFormat.values()) {
            OasDocument document = OasDocument.newDocument(format);
            document.getExternalDocs().setUrl("https://root");
            document.getTags().add("pets");
            document.getTags().get("pets").getExternalDocs().setUrl("https://tag");
            var path = document.getPaths().addPath("/pets");
            Operation get = path.addOperation(HttpMethod.GET);
            get.getExternalDocs().setUrl("https://op");
            document.getComponents().getSchemas().addSchema("Pet").getExternalDocs().setUrl("https://schema");

            OasDocument reloaded = DocumentReader.read(DocumentWriter.write(document), format);

            assertEquals(document.getRoot(), reloaded.getRoot());
            assertEquals("https://root", reloaded.getExternalDocs().getUrl());
            assertEquals("https://tag", reloaded.getTags().get("pets").getExternalDocs().getUrl());
        }
    }
}
