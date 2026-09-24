package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.HttpMethod;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Renaming a schema, with the confirmation stubbed out. Unlike a path, a schema can be
 * {@code $ref}erenced, so a rename with references at stake asks first and rewrites them.
 */
class SchemaRenameTest {

    @Test
    void renamesTheSchemaAndNotifiesWhenNothingReferencesIt() {
        OasDocument document = documentWithPet();
        AtomicBoolean notified = new AtomicBoolean();

        boolean renamed = SchemaRename.rename(document, "Pet", "Animal", (question, details) -> {
            throw new AssertionError("must not ask when nothing references the schema");
        }, () -> notified.set(true));

        assertTrue(renamed);
        assertEquals(List.of("Animal"), document.getComponents().getSchemas().names());
        assertTrue(notified.get());
    }

    @Test
    void asksFirstWhenSomethingReferencesTheSchemaAndRewritesOnConfirmation() {
        OasDocument document = documentWithPet();
        document.getPaths().addPath("/pets").addOperation(HttpMethod.GET)
                .getResponses().addResponse("200").getSchema("application/json")
                .setRef("#/components/schemas/Pet");
        List<String> asked = new ArrayList<>();

        boolean renamed = SchemaRename.rename(document, "Pet", "Animal", (question, details) -> {
            asked.add(question + " | " + details);
            return true;
        }, () -> { });

        assertTrue(renamed);
        assertEquals(List.of("Rename schema \"Pet\" to \"Animal\"? | "
                        + "1 reference will be updated:\n"
                        + "  paths → /pets → get → responses → 200 → content → application/json → schema"),
                asked);
        assertEquals("#/components/schemas/Animal",
                document.getPaths().getPathItem("/pets").getOperation(HttpMethod.GET)
                        .getResponses().getResponse("200").getSchema("application/json").getRef());
    }

    @Test
    void declinedRenameChangesNothing() {
        OasDocument document = documentWithPet();
        document.getComponents().getSchemas().getSchema("Pet").addProperty("owner")
                .setRef("#/components/schemas/Pet");
        AtomicBoolean notified = new AtomicBoolean();

        boolean renamed = SchemaRename.rename(document, "Pet", "Animal", (question, details) -> false,
                () -> notified.set(true));

        assertFalse(renamed);
        assertEquals(List.of("Pet"), document.getComponents().getSchemas().names());
        assertFalse(notified.get());
    }

    @Test
    void renamingOntoAnExistingNameIsRefusedWithoutAsking() {
        OasDocument document = documentWithPet();
        document.getComponents().getSchemas().addSchema("Owner").setType("object");

        boolean renamed = SchemaRename.rename(document, "Pet", "Owner", (question, details) -> {
            throw new AssertionError("must not ask about a rename that is refused anyway");
        }, () -> { });

        assertFalse(renamed);
        assertEquals(List.of("Pet", "Owner"), document.getComponents().getSchemas().names());
    }

    @Test
    void renamingToItselfIsANoOp() {
        OasDocument document = documentWithPet();

        boolean renamed = SchemaRename.rename(document, "Pet", "Pet", (question, details) -> true, () -> { });

        assertFalse(renamed);
    }

    @Test
    void describesASingleReferenceInTheSingular() {
        assertEquals("1 reference will be updated:\n  paths → /pets",
                SchemaRename.describe(List.of("paths → /pets")));
    }

    @Test
    void describesMultipleReferencesInThePlural() {
        assertEquals("2 references will be updated:\n  paths → /pets\n  paths → /owners",
                SchemaRename.describe(List.of("paths → /pets", "paths → /owners")));
    }

    private static OasDocument documentWithPet() {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getComponents().getSchemas().addSchema("Pet").setType("object");
        return document;
    }
}
