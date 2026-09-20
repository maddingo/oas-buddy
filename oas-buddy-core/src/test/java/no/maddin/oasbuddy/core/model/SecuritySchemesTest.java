package no.maddin.oasbuddy.core.model;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.OasDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The named-map facade over {@code components.securitySchemes}. */
class SecuritySchemesTest {

    @Test
    void anAddedSchemeLandsUnderComponentsSecuritySchemes() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);

        document.getComponents().getSecuritySchemes().addScheme("ApiKeyAuth").setType("apiKey");

        assertEquals("apiKey",
                document.getRoot().get("components").get("securitySchemes").get("ApiKeyAuth").get("type").asText());
    }

    @Test
    void namesComeBackInDocumentOrder() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        SecuritySchemes schemes = document.getComponents().getSecuritySchemes();

        schemes.addScheme("BearerAuth");
        schemes.addScheme("ApiKeyAuth");

        assertEquals(List.of("BearerAuth", "ApiKeyAuth"),
                document.getComponents().getSecuritySchemes().names());
    }

    @Test
    void anUnknownNameHasNoScheme() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);

        assertNull(document.getComponents().getSecuritySchemes().getScheme("Nope"));
    }

    @Test
    void removingASchemeTakesItOutOfTheDocument() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        SecuritySchemes schemes = document.getComponents().getSecuritySchemes();
        schemes.addScheme("ApiKeyAuth");
        schemes.addScheme("BearerAuth");

        schemes.removeScheme("ApiKeyAuth");

        assertEquals(List.of("BearerAuth"), document.getComponents().getSecuritySchemes().names());
    }

    @Test
    void schemesAreReadFromALoadedDocument() {
        OasDocument document = DocumentReader.read("""
                openapi: 3.0.3
                info:
                  title: Secured
                  version: 1.0.0
                paths: {}
                components:
                  securitySchemes:
                    ApiKeyAuth:
                      type: apiKey
                      name: X-API-Key
                      in: header
                """, DocumentFormat.YAML);

        SecurityScheme scheme = document.getComponents().getSecuritySchemes().getScheme("ApiKeyAuth");

        assertEquals("apiKey", scheme.getType());
        assertEquals("X-API-Key", scheme.getName());
        assertEquals("header", scheme.getIn());
    }

    /**
     * Building the outline asks for every section of the document. Asking must not change the
     * document, or opening a file and saving it straight back would add empty sections that were
     * never there — the editor's whole point is a clean git diff.
     */
    @Test
    void readingASectionNeverAddsItToTheDocument() {
        OasDocument document = DocumentReader.read("""
                openapi: 3.0.3
                info:
                  title: Bare
                  version: 1.0.0
                paths: {}
                """, DocumentFormat.YAML);

        List<String> schemeNames = document.getComponents().getSecuritySchemes().names();
        List<String> schemaNames = document.getComponents().getSchemas().names();
        assertNull(document.getComponents().getSecuritySchemes().getScheme("Nope"));
        assertNull(document.getComponents().getSchemas().getSchema("Nope"));

        assertTrue(schemeNames.isEmpty());
        assertTrue(schemaNames.isEmpty());
        assertFalse(document.getRoot().has("components"), "reading added a components section");
    }

    @Test
    void writingCreatesTheSectionsItNeeds() {
        OasDocument document = DocumentReader.read("""
                openapi: 3.0.3
                info:
                  title: Bare
                  version: 1.0.0
                paths: {}
                """, DocumentFormat.YAML);

        document.getComponents().getSecuritySchemes().addScheme("ApiKeyAuth").setType("apiKey");

        assertTrue(document.getRoot().get("components").get("securitySchemes").has("ApiKeyAuth"));
    }
}
