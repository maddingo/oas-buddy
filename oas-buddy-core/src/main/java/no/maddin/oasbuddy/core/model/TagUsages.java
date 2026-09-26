package no.maddin.oasbuddy.core.model;

import no.maddin.oasbuddy.core.document.OasDocument;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds the operations tagged with a given name, so a caller can tell the user what a tag removal
 * would leave dangling.
 *
 * <p>The counterpart to {@link SchemaReferences} / {@link SecuritySchemeUsages}, but simpler: a tag
 * can only ever appear in an operation's own {@code tags} array, so this walks
 * {@code paths → <path> → <http method> → tags} directly through the typed facade instead of the raw
 * tree, and renders each usage as {@code GET /pets} rather than an arrow path — there is exactly one
 * shape a usage can take, so the friendlier form loses nothing the arrow path would have added.
 */
public final class TagUsages {

    private TagUsages() {
    }

    /**
     * @return the operations using {@code tagName}, in document order, each rendered as
     *         {@code METHOD /path}. Empty if no operation uses the tag.
     */
    public static List<String> find(OasDocument document, String tagName) {
        List<String> usages = new ArrayList<>();
        Paths paths = document.getPaths();
        for (String path : paths.pathNames()) {
            PathItem pathItem = paths.getPathItem(path);
            if (pathItem == null) {
                // not an object: it has no operations, so nothing in it can use a tag
                continue;
            }
            for (var entry : pathItem.getOperations().entrySet()) {
                if (entry.getValue().getTags().contains(tagName)) {
                    usages.add(entry.getKey().name() + " " + path);
                }
            }
        }
        return usages;
    }
}
