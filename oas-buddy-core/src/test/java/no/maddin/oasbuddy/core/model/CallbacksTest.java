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

class CallbacksTest {

    private final ObjectNode node = JsonNodeFactory.instance.objectNode();
    private final Operation operation = new Operation(node);

    @Test
    void readingNeverAddsTheKey() {
        assertTrue(operation.getCallbacks().names().isEmpty());
        assertNull(operation.getCallbacks().get("onEvent"));
        assertFalse(node.has("callbacks"));
    }

    @Test
    void theKeyGoesWithTheLastCallback() {
        Callbacks callbacks = operation.getCallbacks();
        callbacks.add("A");
        callbacks.add("B");

        callbacks.remove("A");
        assertTrue(node.has("callbacks"));
        callbacks.remove("B");
        assertFalse(node.has("callbacks"));
    }

    @Test
    void aCallbackMapsExpressionsToFullPathItems() {
        Callback callback = operation.getCallbacks().add("onEvent");
        PathItem item = callback.add("{$request.body#/callbackUrl}");
        item.setSummary("Event delivery");
        item.addOperation(HttpMethod.POST).getResponses().addResponse("200").setDescription("ack");
        callback.add("{$request.body#/other}");

        assertEquals(List.of("{$request.body#/callbackUrl}", "{$request.body#/other}"), callback.expressions());
        PathItem read = callback.get("{$request.body#/callbackUrl}");
        assertEquals("Event delivery", read.getSummary());
        assertTrue(read.getOperations().containsKey(HttpMethod.POST));
    }

    @Test
    void expressionsRenameInPlaceAndRefuseACollision() {
        Callback callback = operation.getCallbacks().add("cb");
        callback.add("a");
        callback.add("b");
        callback.add("c");

        assertTrue(callback.rename("b", "z"));
        assertEquals(List.of("a", "z", "c"), callback.expressions());
        assertFalse(callback.rename("z", "a"));
    }

    @Test
    void extensionsAndRefAreNotExpressions() {
        Callback callback = new Callback(JsonNodeFactory.instance.objectNode());
        callback.add("expr");
        callback.node().put("x-note", "kept");

        assertEquals(List.of("expr"), callback.expressions());
        assertNull(callback.get("x-note"));
        callback.remove("x-note");
        assertTrue(callback.node().has("x-note"));
        assertFalse(callback.rename("x-note", "other"));
    }

    @Test
    void renamingAndSwitchingKeepPositions() {
        Callbacks callbacks = operation.getCallbacks();
        callbacks.add("A");
        callbacks.add("B").add("expr");
        callbacks.add("C");

        assertTrue(callbacks.rename("B", "Z"));
        assertEquals(List.of("A", "Z", "C"), callbacks.names());

        callbacks.referTo("Z", "Shared");
        assertEquals("Shared", callbacks.get("Z").getReferencedCallbackName());
        assertEquals("#/components/callbacks/Shared", callbacks.get("Z").getRef());

        Callback template = new Callback(JsonNodeFactory.instance.objectNode());
        template.add("{$request.body#/url}");
        callbacks.defineInline("Z", template);
        assertFalse(callbacks.get("Z").isReference());
        assertEquals(List.of("{$request.body#/url}"), callbacks.get("Z").expressions());
        assertEquals(List.of("A", "Z", "C"), callbacks.names());
    }

    @Test
    void aCallbackThatIsNotAnObjectIsListedButNotReadable() throws IOException {
        OasDocument document = DocumentReader.read("""
                openapi: 3.0.3
                paths:
                  /a:
                    post:
                      callbacks:
                        odd: ~
                        oddExpr:
                          '{$request.body#/u}': ~
                """, DocumentFormat.YAML);
        Operation post = document.getPaths().getPathItem("/a").getOperation(HttpMethod.POST);

        assertEquals(List.of("odd", "oddExpr"), post.getCallbacks().names());
        assertNull(post.getCallbacks().get("odd"));
        assertEquals(List.of("{$request.body#/u}"), post.getCallbacks().get("oddExpr").expressions());
        assertNull(post.getCallbacks().get("oddExpr").get("{$request.body#/u}"));
    }

    @Test
    void componentCallbacksAreLazyAndRoundTripThroughYamlAndJson() throws IOException {
        for (DocumentFormat format : DocumentFormat.values()) {
            OasDocument document = OasDocument.newDocument(format);
            assertTrue(document.getComponents().getCallbacks().names().isEmpty());
            assertFalse(document.getRoot().has("components"));

            document.getComponents().getCallbacks().addCallback("Event")
                    .add("{$request.body#/url}").addOperation(HttpMethod.POST);
            Operation subscribe = document.getPaths().addPath("/subscribe").addOperation(HttpMethod.POST);
            subscribe.getCallbacks().referTo("shared", "Event");
            subscribe.getCallbacks().add("inline").add("{$request.body#/other}")
                    .addOperation(HttpMethod.PUT).getResponses().addResponse("204");

            OasDocument reloaded = DocumentReader.read(DocumentWriter.write(document), format);

            assertEquals(document.getRoot(), reloaded.getRoot());
            assertEquals(List.of("Event"), reloaded.getComponents().getCallbacks().names());
        }
    }
}
