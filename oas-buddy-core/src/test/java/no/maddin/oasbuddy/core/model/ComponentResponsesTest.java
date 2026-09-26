package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.OasDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentResponsesTest {

    @Test
    void readsTheNamedResponsesInDocumentOrder() throws IOException {
        OasDocument document = loadReusable();

        ComponentResponses responses = document.getComponents().getResponses();

        assertAll(
                () -> assertEquals(List.of("NotFound", "Error"), responses.names()),
                () -> assertEquals("The pet does not exist", responses.getResponse("NotFound").getDescription()),
                () -> assertEquals("#/components/schemas/Error",
                        responses.getResponse("Error").findSchema("application/json").getRef()));
    }

    @Test
    void readingTheSectionNeverAddsItToTheDocument() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);

        assertAll(
                () -> assertEquals(List.of(), document.getComponents().getResponses().names()),
                () -> assertNull(document.getComponents().getResponses().getResponse("NotFound")),
                () -> assertFalse(document.getRoot().has("components")));
    }

    @Test
    void addsAndRemovesAResponse() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        ComponentResponses responses = document.getComponents().getResponses();

        responses.addResponse("NotFound").setDescription("Not here");
        responses.addResponse("Error");
        responses.removeResponse("NotFound");

        assertEquals(List.of("Error"), responses.names());
    }

    @Test
    void anOperationResponseReferringToAComponentNamesIt() throws IOException {
        OasDocument document = loadReusable();

        ApiResponse notFound = showPetById(document).getResponses().getResponse("404");

        assertAll(
                () -> assertTrue(notFound.isReference()),
                () -> assertEquals("NotFound", notFound.getReferencedResponseName()),
                () -> assertFalse(showPetById(document).getResponses().getResponse("200").isReference()));
    }

    @Test
    void referringReplacesTheInlineDefinitionInPlace() throws IOException {
        OasDocument document = loadReusable();
        Responses responses = showPetById(document).getResponses();

        responses.referTo("200", "NotFound");

        ObjectNode replaced = (ObjectNode) responsesNode(document).get("200");
        assertAll(
                () -> assertEquals(List.of("200", "404", "default"), responses.statusCodes(),
                        "the status code keeps its position"),
                () -> assertEquals(List.of("$ref"), List.copyOf(fieldNames(replaced))),
                () -> assertEquals("NotFound", responses.getResponse("200").getReferencedResponseName()));
    }

    @Test
    void switchingToInlineStartsFromACopyOfTheReferencedResponse() throws IOException {
        OasDocument document = loadReusable();
        Responses responses = showPetById(document).getResponses();
        ApiResponse component = document.getComponents().getResponses().getResponse("Error");

        ApiResponse inline = responses.defineInline("default", component);
        inline.setDescription("Only this operation's error");

        assertAll(
                () -> assertFalse(inline.isReference()),
                () -> assertEquals("#/components/schemas/Error",
                        inline.findSchema("application/json").getRef()),
                () -> assertEquals("Something went wrong", component.getDescription(),
                        "the copy must not share nodes with the component"),
                () -> assertEquals(List.of("200", "404", "default"), responses.statusCodes()));
    }

    @Test
    void switchingToInlineWithoutATemplateGivesTheOneRequiredField() throws IOException {
        OasDocument document = loadReusable();
        Responses responses = showPetById(document).getResponses();

        ApiResponse inline = responses.defineInline("404", null);

        assertAll(
                () -> assertEquals("", inline.getDescription()),
                () -> assertTrue(inline.isEmpty()));
    }

    @Test
    void aResponseWithNothingButABlankDescriptionHasNothingToLose() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        Responses responses = document.getPaths().addPath("/pets").addOperation(HttpMethod.GET).getResponses();

        ApiResponse blank = responses.addResponse("200");
        ApiResponse described = responses.addResponse("201");
        described.setDescription("Created");
        ApiResponse withContent = responses.addResponse("202");
        withContent.setDescription(" ");
        withContent.getSchema("application/json").setType("string");

        assertAll(
                () -> assertTrue(blank.isEmpty()),
                () -> assertFalse(described.isEmpty()),
                () -> assertFalse(withContent.isEmpty()));
    }

    @Test
    void findingASchemaNeverCreatesContent() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        ApiResponse response = document.getPaths().addPath("/pets").addOperation(HttpMethod.GET)
                .getResponses().addResponse("200");

        assertNull(response.findSchema("application/json"));
        assertTrue(response.isEmpty());
    }

    @Test
    void findsEveryOperationReferringToAComponentResponse() throws IOException {
        OasDocument document = loadReusable();

        assertEquals(List.of(
                        "paths → /pets/{petId} → get → responses → 404",
                        "paths → /pets/{petId} → delete → responses → 404"),
                ComponentReferences.find(document, ComponentResponses.SECTION, "NotFound"));
    }

    @Test
    void aSchemaAndAResponseSharingANameAreNotConfused() throws IOException {
        OasDocument document = loadReusable();

        assertAll(
                () -> assertEquals(List.of("paths → /pets/{petId} → get → responses → default"),
                        ComponentReferences.find(document, ComponentResponses.SECTION, "Error")),
                () -> assertEquals(List.of("components → responses → Error → content → application/json → schema"),
                        SchemaReferences.find(document, "Error")));
    }

    private static Operation showPetById(OasDocument document) {
        return document.getPaths().getPathItem("/pets/{petId}").getOperation(HttpMethod.GET);
    }

    private static ObjectNode responsesNode(OasDocument document) {
        return (ObjectNode) document.getRoot().get("paths").get("/pets/{petId}").get("get").get("responses");
    }

    private static List<String> fieldNames(ObjectNode node) {
        List<String> names = new java.util.ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }

    static OasDocument loadReusable() throws IOException {
        try (InputStream is = ComponentResponsesTest.class.getResourceAsStream("/fixtures/reusable.yaml")) {
            if (is == null) {
                throw new IOException("Fixture not found: reusable.yaml");
            }
            return DocumentReader.read(new String(is.readAllBytes(), StandardCharsets.UTF_8), DocumentFormat.YAML);
        }
    }
}
