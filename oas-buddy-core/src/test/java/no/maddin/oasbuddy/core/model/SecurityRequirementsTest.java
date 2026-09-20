package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.JsonNode;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Security requirements: the document default and the per-operation override.
 *
 * <p>The distinction the spec makes and the editor must keep is between an operation with no
 * {@code security} key — which inherits the document default — and one with an empty array, which
 * is explicitly public. Collapsing the two would silently change what an API requires.
 */
class SecurityRequirementsTest {

    @Test
    void aDocumentWithoutSecurityDeclaresNothing() {
        OasDocument document = bare();

        assertAll(
                () -> assertFalse(document.getSecurity().isDeclared()),
                () -> assertEquals(List.of(), document.getSecurity().requirements()),
                () -> assertFalse(document.getRoot().has("security"), "reading added a security key"));
    }

    @Test
    void anOperationWithoutSecurityInheritsTheDefault() {
        OasDocument document = bareWithOperation();
        Operation operation = operationOf(document);

        assertAll(
                () -> assertFalse(operation.getSecurity().isDeclared(), "should be inheriting"),
                () -> assertFalse(getOperationNode(document).has("security"),
                        "reading turned an inheriting operation into something else"));
    }

    @Test
    void anOperationCanDeclareItselfPublic() {
        OasDocument document = bareWithOperation();
        Operation operation = operationOf(document);

        operation.getSecurity().declarePublic();

        assertAll(
                () -> assertTrue(operation.getSecurity().isDeclared()),
                () -> assertEquals(List.of(), operation.getSecurity().requirements()),
                () -> assertTrue(getOperationNode(document).get("security").isArray()),
                () -> assertTrue(getOperationNode(document).get("security").isEmpty(),
                        "public means an empty array, not an absent key"));
    }

    @Test
    void anOperationCanGoBackToInheriting() {
        OasDocument document = bareWithOperation();
        Operation operation = operationOf(document);
        operation.getSecurity().declarePublic();

        operation.getSecurity().undeclare();

        assertAll(
                () -> assertFalse(operation.getSecurity().isDeclared()),
                () -> assertFalse(getOperationNode(document).has("security")));
    }

    @Test
    void addingARequirementNamesItsScheme() {
        OasDocument document = bare();

        document.getSecurity().add("ApiKeyAuth");

        assertAll(
                () -> assertTrue(document.getSecurity().isDeclared()),
                () -> assertEquals(List.of("ApiKeyAuth"),
                        document.getSecurity().requirements().get(0).schemeNames()),
                () -> assertEquals(List.of(),
                        document.getSecurity().requirements().get(0).getScopes("ApiKeyAuth")),
                () -> assertTrue(document.getRoot().get("security").get(0).has("ApiKeyAuth")));
        }

    @Test
    void requirementsComeBackInDocumentOrder() {
        OasDocument document = bare();

        document.getSecurity().add("BearerAuth");
        document.getSecurity().add("ApiKeyAuth");

        assertEquals(List.of(List.of("BearerAuth"), List.of("ApiKeyAuth")),
                document.getSecurity().requirements().stream().map(SecurityRequirement::schemeNames).toList());
    }

    @Test
    void removingARequirementLeavesTheOthersInOrder() {
        OasDocument document = bare();
        document.getSecurity().add("A");
        document.getSecurity().add("B");
        document.getSecurity().add("C");

        document.getSecurity().remove(1);

        assertEquals(List.of(List.of("A"), List.of("C")),
                document.getSecurity().requirements().stream().map(SecurityRequirement::schemeNames).toList());
    }

    @Test
    void scopesAreWrittenAgainstTheSchemeThatRequiresThem() {
        OasDocument document = bare();
        SecurityRequirement requirement = document.getSecurity().add("OAuth2Auth");

        requirement.setScopes("OAuth2Auth", List.of("read:pets", "write:pets"));

        assertAll(
                () -> assertEquals(List.of("read:pets", "write:pets"), requirement.getScopes("OAuth2Auth")),
                () -> assertEquals(2, document.getRoot().get("security").get(0).get("OAuth2Auth").size()));
    }

    @Test
    void clearingScopesLeavesAnEmptyArrayNotAnAbsentKey() {
        OasDocument document = bare();
        SecurityRequirement requirement = document.getSecurity().add("OAuth2Auth");
        requirement.setScopes("OAuth2Auth", List.of("read:pets"));

        requirement.setScopes("OAuth2Auth", List.of());

        assertAll(
                () -> assertEquals(List.of(), requirement.getScopes("OAuth2Auth")),
                () -> assertTrue(document.getRoot().get("security").get(0).has("OAuth2Auth"),
                        "the scheme is still required, it just needs no scopes"));
    }

    /**
     * Two schemes inside one requirement means both are needed at once. The editor does not build
     * those, but it must recognise one so it can show it without offering to edit it.
     */
    @Test
    void aRequirementNamingSeveralSchemesIsCombined() {
        OasDocument document = DocumentReader.read("""
                openapi: 3.0.3
                info:
                  title: Combined
                  version: 1.0.0
                security:
                  - ApiKeyAuth: []
                    BearerAuth: []
                  - ApiKeyAuth: []
                paths: {}
                """, DocumentFormat.YAML);

        List<SecurityRequirement> requirements = document.getSecurity().requirements();

        assertAll(
                () -> assertTrue(requirements.get(0).isCombined()),
                () -> assertEquals(List.of("ApiKeyAuth", "BearerAuth"), requirements.get(0).schemeNames()),
                () -> assertFalse(requirements.get(1).isCombined()));
    }

    @Test
    void bothLevelsAreReadFromALoadedDocument() throws IOException {
        OasDocument document = fixture();

        assertAll(
                () -> assertEquals(List.of("ApiKeyAuth"),
                        document.getSecurity().requirements().get(0).schemeNames()),
                () -> assertEquals(List.of("BearerAuth"),
                        document.getPaths().getPathItem("/pets").getOperation(HttpMethod.GET)
                                .getSecurity().requirements().get(0).schemeNames()));
    }

    private static OasDocument bare() {
        return DocumentReader.read("""
                openapi: 3.0.3
                info:
                  title: Bare
                  version: 1.0.0
                paths: {}
                """, DocumentFormat.YAML);
    }

    private static OasDocument bareWithOperation() {
        OasDocument document = bare();
        document.getPaths().addPath("/pets").addOperation(HttpMethod.GET).setOperationId("listPets");
        return document;
    }

    private static Operation operationOf(OasDocument document) {
        return document.getPaths().getPathItem("/pets").getOperation(HttpMethod.GET);
    }

    /** Read through the tree rather than via an accessor that exists only for tests. */
    private static JsonNode getOperationNode(OasDocument document) {
        return document.getRoot().get("paths").get("/pets").get("get");
    }

    private static OasDocument fixture() throws IOException {
        try (InputStream is = SecurityRequirementsTest.class.getResourceAsStream("/fixtures/secured.yaml")) {
            if (is == null) {
                throw new IOException("fixture not found");
            }
            return DocumentReader.read(new String(is.readAllBytes(), StandardCharsets.UTF_8),
                    DocumentFormat.YAML);
        }
    }
}
