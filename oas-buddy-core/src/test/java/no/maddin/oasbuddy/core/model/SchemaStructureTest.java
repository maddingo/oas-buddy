package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Array element types, references to other schemas and {@code additionalProperties}. */
class SchemaStructureTest {

    private OasDocument document;
    private Schema pet;

    @BeforeEach
    void setUp() {
        document = OasDocument.newDocument(DocumentFormat.YAML);
        pet = document.getComponents().getSchemas().addSchema("Pet");
        pet.setType("object");
    }

    @Test
    void readingItemsNeverAddsThem() {
        Schema name = pet.addProperty("name");
        name.setType("string");

        assertNull(name.getItems());
        assertFalse(rawProperty("name").has("items"));
    }

    @Test
    void anArrayPropertyCanHoldAScalarElementType() {
        Schema tags = pet.addProperty("tags");
        tags.changeTypeTo("array");
        tags.createItems().changeTypeTo("string");

        assertEquals("string", rawProperty("tags").get("items").get("type").asText());
        assertEquals("string", tags.getItems().getType());
    }

    @Test
    void anArrayPropertyCanHoldReferencedElements() {
        Schema friends = pet.addProperty("friends");
        friends.changeTypeTo("array");
        friends.createItems().referTo("Pet");

        assertEquals("#/components/schemas/Pet", rawProperty("friends").get("items").get("$ref").asText());
        assertEquals("Pet", friends.getItems().getReferencedSchemaName());
    }

    @Test
    void referringToASchemaDropsTheInlineTypeItReplaces() {
        Schema owner = pet.addProperty("owner");
        owner.changeTypeTo("array");
        owner.setFormat("whatever");
        owner.createItems().changeTypeTo("string");

        owner.referTo("Person");

        assertEquals(List.of("$ref"), fieldNames(rawProperty("owner")));
    }

    @Test
    void referringToASchemaLeavesWhatThePickerDoesNotControl() {
        Schema owner = pet.addProperty("owner");
        owner.changeTypeTo("string");
        owner.setDescription("who looks after it");
        rawProperty("owner").put("x-internal", true);

        owner.referTo("Person");

        assertEquals(List.of("description", "x-internal", "$ref"), fieldNames(rawProperty("owner")));
    }

    @Test
    void changingAReferenceToATypeDropsTheReference() {
        Schema owner = pet.addProperty("owner");
        owner.referTo("Person");

        owner.changeTypeTo("string");

        assertAll(
                () -> assertEquals(List.of("type"), fieldNames(rawProperty("owner"))),
                () -> assertNull(owner.getReferencedSchemaName()));
    }

    @Test
    void changingAwayFromArrayDropsTheItems() {
        Schema tags = pet.addProperty("tags");
        tags.changeTypeTo("array");
        tags.createItems().changeTypeTo("string");

        tags.changeTypeTo("string");

        assertEquals(List.of("type"), fieldNames(rawProperty("tags")));
    }

    @Test
    void keepingArrayKeepsTheItems() {
        Schema tags = pet.addProperty("tags");
        tags.changeTypeTo("array");
        tags.createItems().changeTypeTo("string");

        tags.changeTypeTo("array");

        assertEquals("string", tags.getItems().getType());
    }

    @Test
    void theBareSetterLeavesItemsAloneWhileAValueIsBeingTyped() {
        Schema tags = pet.addProperty("tags");
        tags.changeTypeTo("array");
        tags.createItems().changeTypeTo("string");

        tags.setType("arr");
        tags.setType("array");

        assertEquals("string", tags.getItems().getType());
    }

    @Test
    void aReferenceOutsideTheComponentSchemasHasNoSchemaName() {
        Schema external = pet.addProperty("external");
        external.setRef("other.yaml#/Thing");

        assertNull(external.getReferencedSchemaName());
    }

    @Test
    void additionalPropertiesIsAbsentUntilSet() {
        assertAll(
                () -> assertNull(pet.getAdditionalPropertiesAllowed()),
                () -> assertNull(pet.getAdditionalPropertiesSchema()),
                () -> assertFalse(rawPet().has("additionalProperties")));
    }

    @Test
    void additionalPropertiesInItsBooleanForm() {
        pet.setAdditionalPropertiesAllowed(false);

        assertAll(
                () -> assertTrue(rawPet().get("additionalProperties").isBoolean()),
                () -> assertEquals(Boolean.FALSE, pet.getAdditionalPropertiesAllowed()),
                () -> assertNull(pet.getAdditionalPropertiesSchema()));
    }

    @Test
    void additionalPropertiesInItsSchemaForm() {
        pet.createAdditionalPropertiesSchema().changeTypeTo("integer");

        assertAll(
                () -> assertEquals("integer", rawPet().get("additionalProperties").get("type").asText()),
                () -> assertEquals("integer", pet.getAdditionalPropertiesSchema().getType()),
                () -> assertNull(pet.getAdditionalPropertiesAllowed()));
    }

    @Test
    void theSchemaFormReplacesTheBooleanFormAndBack() {
        pet.setAdditionalPropertiesAllowed(true);
        pet.createAdditionalPropertiesSchema().referTo("Tag");
        assertEquals("Tag", pet.getAdditionalPropertiesSchema().getReferencedSchemaName());

        pet.setAdditionalPropertiesAllowed(false);
        assertEquals(Boolean.FALSE, pet.getAdditionalPropertiesAllowed());

        pet.setAdditionalPropertiesAllowed(null);
        assertFalse(rawPet().has("additionalProperties"));
    }

    @Test
    void creatingTheSchemaFormKeepsAnExistingSchema() {
        pet.createAdditionalPropertiesSchema().changeTypeTo("integer");

        assertEquals("integer", pet.createAdditionalPropertiesSchema().getType());
    }

    private ObjectNode rawPet() {
        return (ObjectNode) document.getRoot().get("components").get("schemas").get("Pet");
    }

    private ObjectNode rawProperty(String name) {
        return (ObjectNode) rawPet().get("properties").get(name);
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
