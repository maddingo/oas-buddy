package no.maddin.oasbuddy.core.model;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Renaming a schema, a path and a property: each keeps its position and its body, and refuses a
 * name that already exists rather than overwriting it.
 */
class SchemaRenamingTest {

    @Test
    void renamingASchemaKeepsItsPositionAndBody() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        Schemas schemas = document.getComponents().getSchemas();
        schemas.addSchema("Pet").setType("object");
        schemas.addSchema("Owner").setType("object");
        schemas.addSchema("Tag").setType("string");

        boolean renamed = schemas.renameSchema("Owner", "Person");

        assertAll(
                () -> assertTrue(renamed),
                () -> assertEquals(List.of("Pet", "Person", "Tag"), schemas.names()),
                () -> assertEquals("object", schemas.getSchema("Person").getType()));
    }

    @Test
    void renamingASchemaOntoAnExistingNameIsRefused() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        Schemas schemas = document.getComponents().getSchemas();
        schemas.addSchema("Pet").setType("object");
        schemas.addSchema("Owner").setType("string");

        boolean renamed = schemas.renameSchema("Pet", "Owner");

        assertAll(
                () -> assertFalse(renamed),
                () -> assertEquals(List.of("Pet", "Owner"), schemas.names()),
                () -> assertEquals("string", schemas.getSchema("Owner").getType()),
                () -> assertEquals("object", schemas.getSchema("Pet").getType()));
    }

    @Test
    void renamingASchemaThatDoesNotExistIsRefused() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getComponents().getSchemas().addSchema("Pet").setType("object");

        assertFalse(document.getComponents().getSchemas().renameSchema("Nope", "Something"));
    }

    @Test
    void renamingAPathKeepsItsPositionAndBody() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        Paths paths = document.getPaths();
        paths.addPath("/pets").addOperation(HttpMethod.GET).setOperationId("listPets");
        paths.addPath("/owners");
        paths.addPath("/tags");

        boolean renamed = paths.renamePath("/owners", "/people");

        assertAll(
                () -> assertTrue(renamed),
                () -> assertEquals(List.of("/pets", "/people", "/tags"), paths.pathNames()),
                () -> assertEquals("listPets",
                        paths.getPathItem("/pets").getOperation(HttpMethod.GET).getOperationId()));
    }

    @Test
    void renamingAPathOntoAnExistingNameIsRefused() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        Paths paths = document.getPaths();
        paths.addPath("/pets");
        paths.addPath("/owners");

        boolean renamed = paths.renamePath("/pets", "/owners");

        assertAll(
                () -> assertFalse(renamed),
                () -> assertEquals(List.of("/pets", "/owners"), paths.pathNames()));
    }

    @Test
    void renamingAPropertyKeepsItsPositionAndBody() {
        Schema pet = newObjectSchema();
        pet.addProperty("id").setType("integer");
        Schema name = pet.addProperty("name");
        name.setType("string");
        name.setDescription("the pet's name");
        pet.addProperty("tag").setType("string");

        boolean renamed = pet.renameProperty("name", "label");

        assertAll(
                () -> assertTrue(renamed),
                () -> assertEquals(List.of("id", "label", "tag"), pet.propertyNames()),
                () -> assertEquals("string", pet.getProperty("label").getType()),
                () -> assertEquals("the pet's name", pet.getProperty("label").getDescription()));
    }

    @Test
    void renamingAPropertyOntoAnExistingNameIsRefused() {
        Schema pet = newObjectSchema();
        pet.addProperty("id").setType("integer");
        pet.addProperty("name").setType("string");

        boolean renamed = pet.renameProperty("id", "name");

        assertAll(
                () -> assertFalse(renamed),
                () -> assertEquals(List.of("id", "name"), pet.propertyNames()),
                () -> assertEquals("integer", pet.getProperty("id").getType()));
    }

    @Test
    void renamingAPropertyThatIsRequiredRenamesItInTheRequiredListAtTheSameIndex() {
        Schema pet = newObjectSchema();
        pet.addProperty("id").setType("integer");
        pet.addProperty("name").setType("string");
        pet.addProperty("tag").setType("string");
        pet.setRequired(List.of("tag", "name", "id"));

        pet.renameProperty("name", "label");

        assertEquals(List.of("tag", "label", "id"), pet.getRequired());
    }

    @Test
    void renamingAPropertyNotInRequiredLeavesItAlone() {
        Schema pet = newObjectSchema();
        pet.addProperty("id").setType("integer");
        pet.addProperty("name").setType("string");
        pet.setRequired(List.of("id"));

        pet.renameProperty("name", "label");

        assertEquals(List.of("id"), pet.getRequired());
    }

    private static Schema newObjectSchema() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        Schema schema = document.getComponents().getSchemas().addSchema("Pet");
        schema.setType("object");
        return schema;
    }
}
