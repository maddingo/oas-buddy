package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.TextNode;
import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Media types beyond application/json, their examples, and reusable component examples. */
class ContentTest {

    @Test
    void readsEveryMediaTypeOfAResponseInOrder() throws IOException {
        Content content = listPetsOk(ComponentResponsesTest.loadReusable()).getContent();

        assertAll(
                () -> assertEquals(List.of("application/json", "application/xml"), content.mediaTypes()),
                () -> assertEquals("string", content.get("application/xml").findSchema().getType()),
                () -> assertEquals("<pets/>", content.get("application/xml").getExample().asText()));
    }

    @Test
    void readsEveryMediaTypeOfARequestBody() throws IOException {
        RequestBody body = ComponentResponsesTest.loadReusable().getPaths().getPathItem("/pets")
                .getOperation(HttpMethod.POST).getRequestBody();

        assertAll(
                () -> assertEquals(List.of("application/json", "application/x-www-form-urlencoded"),
                        body.getContent().mediaTypes()),
                () -> assertEquals("Tom", body.getContent().get("application/json").getExample().get("name").asText()));
    }

    @Test
    void readsNamedExamplesBothInlineAndReferenced() throws IOException {
        MediaTypeExamples examples = listPetsOk(ComponentResponsesTest.loadReusable()).getContent()
                .get("application/json").getExamples();

        assertAll(
                () -> assertEquals(List.of("cat", "dog"), examples.names()),
                () -> assertEquals("Cat", examples.get("cat").getReferencedExampleName()),
                () -> assertFalse(examples.get("dog").isReference()),
                () -> assertEquals("A dog", examples.get("dog").getSummary()),
                () -> assertEquals("Rex", examples.get("dog").getValue().get(0).get("name").asText()));
    }

    @Test
    void readingNeverAddsContentOrExamples() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        ApiResponse response = document.getPaths().addPath("/pets").addOperation(HttpMethod.GET)
                .getResponses().addResponse("204");
        response.setDescription("No content");

        assertAll(
                () -> assertEquals(List.of(), response.getContent().mediaTypes()),
                () -> assertNull(response.getContent().get("application/json")),
                () -> assertNull(response.findSchema("application/json")),
                () -> assertEquals(List.of(), document.getComponents().getExamples().names()),
                () -> assertFalse(document.getRoot().has("components")),
                () -> assertEquals(List.of("description"), fieldNames(document, "204")));
    }

    @Test
    void addedMediaTypesKeepTheirOrder() {
        ApiResponse response = newResponse();

        response.getContent().add("application/xml");
        response.getContent().add("text/plain");
        response.getContent().add("*/*");

        assertEquals(List.of("application/xml", "text/plain", "*/*"), response.getContent().mediaTypes());
    }

    @Test
    void removingTheLastMediaTypeLeavesNoContentHusk() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        ApiResponse response = document.getPaths().addPath("/pets").addOperation(HttpMethod.GET)
                .getResponses().addResponse("200");
        response.setDescription("OK");
        response.getContent().add("text/plain").getSchema().setType("string");

        response.getContent().remove("text/plain");

        assertEquals(List.of("description"), fieldNames(document, "200"), "no content: {} left behind");
    }

    @Test
    void renamingAMediaTypeKeepsItsPlaceAndItsSchema() {
        ApiResponse response = newResponse();
        response.getContent().add("application/json").getSchema().setType("object");
        response.getContent().add("text/plain");

        boolean renamed = response.getContent().rename("application/json", "application/problem+json");

        assertAll(
                () -> assertTrue(renamed),
                () -> assertEquals(List.of("application/problem+json", "text/plain"), response.getContent().mediaTypes()),
                () -> assertEquals("object",
                        response.getContent().get("application/problem+json").findSchema().getType()),
                () -> assertFalse(response.getContent().rename("text/plain", "application/problem+json"),
                        "a collision is refused"));
    }

    @Test
    void removingTheLastNamedExampleTakesTheKeyOut() {
        MediaType json = newResponse().getContent().add("application/json");
        json.getExamples().add("one").setValue(TextNode.valueOf("x"));

        json.getExamples().remove("one");

        assertEquals(List.of(), json.getExamples().names());
    }

    @Test
    void switchingAnExampleBetweenInlineAndReferenceKeepsItsPlace() throws IOException {
        OasDocument document = ComponentResponsesTest.loadReusable();
        MediaTypeExamples examples = listPetsOk(document).getContent().get("application/json").getExamples();

        Example inline = examples.defineInline("cat", document.getComponents().getExamples().getExample("Cat"));
        inline.setSummary("Only here");
        examples.referTo("dog", "Cat");

        assertAll(
                () -> assertEquals(List.of("cat", "dog"), examples.names()),
                () -> assertEquals("Tom", examples.get("cat").getValue().get(0).get("name").asText()),
                () -> assertEquals("A cat", document.getComponents().getExamples().getExample("Cat").getSummary(),
                        "the copy must not share nodes with the component"),
                () -> assertEquals("Cat", examples.get("dog").getReferencedExampleName()));
    }

    @Test
    void findsEveryReferenceToAComponentExample() throws IOException {
        OasDocument document = ComponentResponsesTest.loadReusable();

        assertEquals(List.of("paths → /pets → get → responses → 200 → content → application/json → examples → cat"),
                ComponentReferences.find(document, ComponentExamples.SECTION, "Cat"));
    }

    @Test
    void componentExamplesAreANamedMap() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        ComponentExamples examples = document.getComponents().getExamples();

        examples.addExample("Cat").setSummary("A cat");
        examples.addExample("Dog").setExternalValue("https://example.com/dog.json");
        examples.removeExample("Cat");

        assertAll(
                () -> assertEquals(List.of("Dog"), examples.names()),
                () -> assertEquals("https://example.com/dog.json", examples.getExample("Dog").getExternalValue()));
    }

    private static ApiResponse listPetsOk(OasDocument document) {
        return document.getPaths().getPathItem("/pets").getOperation(HttpMethod.GET).getResponses().getResponse("200");
    }

    private static ApiResponse newResponse() {
        return OasDocument.newDocument(DocumentFormat.YAML).getPaths().addPath("/pets")
                .addOperation(HttpMethod.GET).getResponses().addResponse("200");
    }

    private static List<String> fieldNames(OasDocument document, String statusCode) {
        List<String> names = new java.util.ArrayList<>();
        document.getRoot().get("paths").get("/pets").get("get").get("responses").get(statusCode)
                .fieldNames().forEachRemaining(names::add);
        return names;
    }
}
