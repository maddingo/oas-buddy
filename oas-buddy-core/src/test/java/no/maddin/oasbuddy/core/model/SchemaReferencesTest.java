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
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchemaReferencesTest {

    @Test
    void findsEveryReferenceToASchema() throws IOException {
        OasDocument document = loadPetstore();

        List<String> usages = SchemaReferences.find(document, "Pet");

        assertEquals(List.of(
                        "paths → /pets → get → responses → 200 → content → application/json → schema → items",
                        "paths → /pets/{petId} → get → responses → 200 → content → application/json → schema"),
                usages);
    }

    @Test
    void findsAReferenceFromARequestBody() throws IOException {
        OasDocument document = loadPetstore();

        List<String> usages = SchemaReferences.find(document, "NewPet");

        assertEquals(List.of(
                        "paths → /pets → post → requestBody → content → application/json → schema"),
                usages);
    }

    @Test
    void findsNothingForAnUnreferencedSchema() throws IOException {
        OasDocument document = loadPetstore();
        document.getComponents().getSchemas().addSchema("Unused").setType("object");

        assertTrue(SchemaReferences.find(document, "Unused").isEmpty());
    }

    @Test
    void findsReferencesFromOtherSchemas() throws IOException {
        OasDocument document = loadPetstore();
        document.getComponents().getSchemas().getSchema("Pet").addProperty("owner")
                .setRef("#/components/schemas/Person");

        assertEquals(List.of("components → schemas → Pet → properties → owner"),
                SchemaReferences.find(document, "Person"));
    }

    @Test
    void doesNotMatchASchemaWhoseNameSharesAPrefix() throws IOException {
        OasDocument document = loadPetstore();

        assertTrue(SchemaReferences.find(document, "Pe").isEmpty());
    }

    @Test
    void rewritesEveryReferenceIncludingOneInsideAnArraysItems() throws IOException {
        OasDocument document = loadPetstore();

        int count = SchemaReferences.rewrite(document, "Pet", "Animal");

        assertAll(
                () -> assertEquals(2, count),
                () -> assertTrue(SchemaReferences.find(document, "Pet").isEmpty()),
                () -> assertEquals(List.of(
                                "paths → /pets → get → responses → 200 → content → application/json → schema → items",
                                "paths → /pets/{petId} → get → responses → 200 → content → application/json → schema"),
                        SchemaReferences.find(document, "Animal")));
    }

    @Test
    void rewritesAReferenceInsideAnotherSchemasProperties() throws IOException {
        OasDocument document = loadPetstore();
        document.getComponents().getSchemas().getSchema("Pet").addProperty("owner")
                .setRef("#/components/schemas/Person");

        int count = SchemaReferences.rewrite(document, "Person", "Human");

        assertAll(
                () -> assertEquals(1, count),
                () -> assertEquals("#/components/schemas/Human",
                        document.getComponents().getSchemas().getSchema("Pet").getProperty("owner").getRef()));
    }

    @Test
    void rewritesAReferenceInsideAnAllOf() throws IOException {
        OasDocument document = loadPetstore();
        ObjectNode schemas = (ObjectNode) document.getRoot().get("components").get("schemas");
        schemas.putObject("Dog").putArray("allOf").addObject().put("$ref", "#/components/schemas/Pet");

        int count = SchemaReferences.rewrite(document, "Pet", "Animal");

        assertAll(
                () -> assertEquals(3, count),
                () -> assertEquals("#/components/schemas/Animal",
                        document.getRoot().get("components").get("schemas").get("Dog")
                                .get("allOf").get(0).get("$ref").asText()));
    }

    @Test
    void leavesAReferenceToADifferentlyNamedSchemaThatSharesAPrefixUntouched() throws IOException {
        OasDocument document = loadPetstore();
        document.getComponents().getSchemas().addSchema("PetOwner").setType("object");
        document.getComponents().getSchemas().addSchema("Breeder").addProperty("owner")
                .setRef("#/components/schemas/PetOwner");

        int count = SchemaReferences.rewrite(document, "Pet", "Animal");

        assertAll(
                () -> assertEquals(2, count, "only the two genuine references to Pet are rewritten"),
                () -> assertEquals("#/components/schemas/PetOwner",
                        document.getComponents().getSchemas().getSchema("Breeder").getProperty("owner").getRef(),
                        "a $ref to PetOwner must not be mistaken for one to Pet"));
    }

    @Test
    void findsNoRewritesForASchemaThatIsNotReferenced() throws IOException {
        OasDocument document = loadPetstore();
        document.getComponents().getSchemas().addSchema("Unused").setType("object");

        assertEquals(0, SchemaReferences.rewrite(document, "Unused", "StillUnused"));
    }

    private static OasDocument loadPetstore() throws IOException {
        try (InputStream is = SchemaReferencesTest.class.getResourceAsStream("/fixtures/petstore.yaml")) {
            if (is == null) {
                throw new IOException("Fixture not found: petstore.yaml");
            }
            return DocumentReader.read(new String(is.readAllBytes(), StandardCharsets.UTF_8), DocumentFormat.YAML);
        }
    }
}
