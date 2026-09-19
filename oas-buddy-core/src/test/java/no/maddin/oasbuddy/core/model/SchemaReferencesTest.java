package no.maddin.oasbuddy.core.model;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.OasDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

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

    private static OasDocument loadPetstore() throws IOException {
        try (InputStream is = SchemaReferencesTest.class.getResourceAsStream("/fixtures/petstore.yaml")) {
            if (is == null) {
                throw new IOException("Fixture not found: petstore.yaml");
            }
            return DocumentReader.read(new String(is.readAllBytes(), StandardCharsets.UTF_8), DocumentFormat.YAML);
        }
    }
}
