package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.JsonNode;
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

/** Operation- and path-level parameter lists, component parameters, and references between them. */
class ParametersTest {

    @Test
    void readsAPathLevelParameter() throws IOException {
        OasDocument document = ComponentResponsesTest.loadReusable();

        List<Parameter> parameters = document.getPaths().getPathItem("/pets/{petId}").getParameters().all();

        assertAll(
                () -> assertEquals(List.of("petId"), parameters.stream().map(Parameter::getName).toList()),
                () -> assertEquals("path", parameters.getFirst().getIn()),
                () -> assertEquals("string", parameters.getFirst().findSchema().getType()));
    }

    @Test
    void readsReferencedAndInlineOperationParametersInOrder() throws IOException {
        OasDocument document = ComponentResponsesTest.loadReusable();

        List<Parameter> parameters = listPets(document).getParameters().all();

        assertAll(
                () -> assertTrue(parameters.get(0).isReference()),
                () -> assertEquals("Limit", parameters.get(0).getReferencedParameterName()),
                () -> assertFalse(parameters.get(1).isReference()),
                () -> assertEquals("sort", parameters.get(1).getName()));
    }

    @Test
    void readsTheComponentParameters() throws IOException {
        OasDocument document = ComponentResponsesTest.loadReusable();
        ComponentParameters components = document.getComponents().getParameters();

        assertAll(
                () -> assertEquals(List.of("Limit"), components.names()),
                () -> assertEquals("limit", components.getParameter("Limit").getName()),
                () -> assertEquals("How many items to return", components.getParameter("Limit").getDescription()));
    }

    @Test
    void readingNeverAddsAParametersKeyOrSection() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        PathItem pets = document.getPaths().addPath("/pets");
        Operation get = pets.addOperation(HttpMethod.GET);

        assertAll(
                () -> assertEquals(List.of(), pets.getParameters().all()),
                () -> assertEquals(List.of(), get.getParameters().all()),
                () -> assertEquals(List.of(), document.getComponents().getParameters().names()),
                () -> assertNull(document.getComponents().getParameters().getParameter("Limit")),
                () -> assertFalse(node(document, "/pets").has("parameters")),
                () -> assertFalse(node(document, "/pets").get("get").has("parameters")),
                () -> assertFalse(document.getRoot().has("components")));
    }

    @Test
    void removingTheLastParameterTakesTheKeyOut() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        Parameters parameters = document.getPaths().addPath("/pets").getParameters();
        Parameter only = parameters.add("limit", "query");

        parameters.remove(only);

        assertFalse(node(document, "/pets").has("parameters"), "no [] husk left behind");
    }

    @Test
    void removesTheEntryItWasGivenEvenWhenAnotherHasEqualContent() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        Parameters parameters = document.getPaths().addPath("/pets").getParameters();
        Parameter first = parameters.add("limit", "query");
        Parameter second = parameters.add("limit", "query");

        parameters.remove(second);

        assertEquals(1, parameters.all().size());
        assertTrue(parameters.all().getFirst().node() == first.node(), "the first entry must survive");
    }

    @Test
    void referringReplacesTheInlineEntryInPlace() throws IOException {
        OasDocument document = ComponentResponsesTest.loadReusable();
        Parameters parameters = listPets(document).getParameters();
        Parameter sort = parameters.all().get(1);

        parameters.referTo(sort, "Limit");

        List<Parameter> after = parameters.all();
        assertAll(
                () -> assertEquals(2, after.size()),
                () -> assertEquals("Limit", after.get(1).getReferencedParameterName()),
                () -> assertNull(after.get(1).getName(), "the inline definition is gone"));
    }

    @Test
    void switchingToInlineStartsFromACopyOfTheComponent() throws IOException {
        OasDocument document = ComponentResponsesTest.loadReusable();
        Parameters parameters = listPets(document).getParameters();
        Parameter component = document.getComponents().getParameters().getParameter("Limit");

        Parameter inline = parameters.defineInline(parameters.all().getFirst(), component);
        inline.setDescription("Only here");

        assertAll(
                () -> assertFalse(parameters.all().getFirst().isReference()),
                () -> assertEquals("limit", parameters.all().getFirst().getName()),
                () -> assertEquals("integer", parameters.all().getFirst().findSchema().getType()),
                () -> assertEquals("How many items to return", component.getDescription(),
                        "the copy must not share nodes with the component"));
    }

    @Test
    void addsAReferenceToAComponentParameter() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getComponents().getParameters().addParameter("Limit", "limit", "query");
        Parameters parameters = document.getPaths().addPath("/pets").addOperation(HttpMethod.GET).getParameters();

        parameters.addReference("Limit");

        assertEquals("#/components/parameters/Limit", parameters.all().getFirst().getRef());
    }

    @Test
    void findsEveryReferenceToAComponentParameter() throws IOException {
        OasDocument document = ComponentResponsesTest.loadReusable();
        document.getPaths().getPathItem("/pets/{petId}").getParameters().addReference("Limit");

        assertEquals(List.of(
                        "paths → /pets → get → parameters → [0]",
                        "paths → /pets/{petId} → parameters → [1]"),
                ComponentReferences.find(document, ComponentParameters.SECTION, "Limit"));
    }

    @Test
    void findingASchemaNeverCreatesOne() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        Parameter parameter = document.getPaths().addPath("/pets").getParameters().add("limit", "query");

        assertNull(parameter.findSchema());
        assertFalse(node(document, "/pets").get("parameters").get(0).has("schema"));
    }

    private static Operation listPets(OasDocument document) {
        return document.getPaths().getPathItem("/pets").getOperation(HttpMethod.GET);
    }

    private static JsonNode node(OasDocument document, String path) {
        return document.getRoot().get("paths").get(path);
    }
}
