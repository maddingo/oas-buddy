package no.maddin.oasbuddy.core.model;

import no.maddin.oasbuddy.core.document.OasDocument;

import java.util.List;

/**
 * Finds where a component schema is referenced from, so a caller can tell the user what a
 * removal would break. The schema case of {@link ComponentReferences}.
 */
public final class SchemaReferences {

    private static final String SECTION = "schemas";

    private SchemaReferences() {
    }

    /**
     * @return the locations referencing {@code schemaName}, in document order, each rendered as a
     *         readable path such as {@code paths → /pets → get → responses → 200}. Empty if the
     *         schema is not referenced anywhere.
     */
    public static List<String> find(OasDocument document, String schemaName) {
        return ComponentReferences.find(document, SECTION, schemaName);
    }

    /**
     * Rewrites every {@code $ref} pointing at {@code from} to point at {@code to} instead, so a
     * schema rename never leaves a reference dangling.
     *
     * @return how many references were rewritten
     */
    public static int rewrite(OasDocument document, String from, String to) {
        return ComponentReferences.rewrite(document, SECTION, from, to);
    }
}
