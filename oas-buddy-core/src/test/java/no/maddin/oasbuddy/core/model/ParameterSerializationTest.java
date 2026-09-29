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

class ParameterSerializationTest {

    private final ObjectNode node = JsonNodeFactory.instance.objectNode();
    private final Parameter parameter = new Parameter(node);

    @Test
    void theSpecStatesWhichStylesEachLocationAllowsAndTheirDefaults() {
        assertEquals(List.of("matrix", "label", "simple"), ParameterStyles.validFor("path"));
        assertEquals(List.of("form", "spaceDelimited", "pipeDelimited", "deepObject"),
                ParameterStyles.validFor("query"));
        assertEquals(List.of("simple"), ParameterStyles.validFor("header"));
        assertEquals(List.of("form"), ParameterStyles.validFor("cookie"));
        assertTrue(ParameterStyles.validFor("nowhere").isEmpty());

        assertEquals("simple", ParameterStyles.defaultFor("path"));
        assertEquals("form", ParameterStyles.defaultFor("query"));
        assertEquals("simple", ParameterStyles.defaultFor("header"));
        assertEquals("form", ParameterStyles.defaultFor("cookie"));
        assertNull(ParameterStyles.defaultFor("nowhere"));

        assertTrue(ParameterStyles.defaultExplode("form"));
        assertFalse(ParameterStyles.defaultExplode("simple"));
        assertFalse(ParameterStyles.defaultExplode("deepObject"));
    }

    @Test
    void changingInDropsAStyleTheNewLocationDoesNotAllow() {
        parameter.setIn("query");
        parameter.setStyle("deepObject");

        parameter.setIn("path");
        assertNull(parameter.getStyle());

        parameter.setStyle("matrix");
        parameter.setIn("path");
        assertEquals("matrix", parameter.getStyle(), "still valid, kept");
    }

    @Test
    void changingAwayFromQueryDropsAllowReservedAndAllowEmptyValue() {
        parameter.setIn("query");
        parameter.setAllowReserved(true);
        parameter.setAllowEmptyValue(true);

        parameter.setIn("header");

        assertFalse(node.has("allowReserved"));
        assertFalse(node.has("allowEmptyValue"));
    }

    @Test
    void anUnknownLocationTouchesNothing() {
        parameter.setIn("query");
        parameter.setStyle("form");
        parameter.setAllowReserved(true);

        parameter.setIn("typo");

        assertEquals("form", parameter.getStyle());
        assertTrue(parameter.isAllowReserved());
    }

    @Test
    void explodeIsTriStateAndFlagsWriteOnlyTrue() {
        assertNull(parameter.getExplode());
        parameter.setExplode(false);
        assertEquals(Boolean.FALSE, parameter.getExplode());
        parameter.setExplode(null);
        assertFalse(node.has("explode"));

        parameter.setDeprecated(true);
        assertTrue(parameter.isDeprecated());
        parameter.setDeprecated(false);
        assertFalse(node.has("deprecated"));
    }

    @Test
    void readingAddsNothing() {
        parameter.isDeprecated();
        parameter.getExamples().names();
        parameter.hasContent();

        assertTrue(node.isEmpty());
    }

    @Test
    void aParameterUsingContentIsRecognisedAndItsContentReadable() throws IOException {
        OasDocument document = DocumentReader.read("""
                {"openapi":"3.0.3","paths":{"/a":{"get":{"parameters":[
                  {"name":"filter","in":"query","content":{"application/json":{"schema":{"type":"object"}}}}]}}}}
                """, DocumentFormat.JSON);

        Parameter filter = document.getPaths().getPathItem("/a").getOperation(HttpMethod.GET)
                .getParameters().all().get(0);

        assertTrue(filter.hasContent());
        assertEquals(List.of("application/json"), filter.getContent().mediaTypes());
    }

    @Test
    void serializationDetailRoundTripsThroughYamlAndJson() throws IOException {
        for (DocumentFormat format : DocumentFormat.values()) {
            OasDocument document = OasDocument.newDocument(format);
            Parameter tags = document.getPaths().addPath("/a").addOperation(HttpMethod.GET)
                    .getParameters().add("tags", "query");
            tags.setStyle("pipeDelimited");
            tags.setExplode(false);
            tags.setAllowReserved(true);
            tags.setDeprecated(true);
            tags.setExample(JsonNodeFactory.instance.textNode("a|b"));
            tags.getExamples().add("two").setSummary("Two tags");

            OasDocument reloaded = DocumentReader.read(DocumentWriter.write(document), format);

            assertEquals(document.getRoot(), reloaded.getRoot());
        }
    }
}
