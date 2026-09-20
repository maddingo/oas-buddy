package no.maddin.oasbuddy.core.document;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * An object somewhere below a root node that is added to the tree only once something is written
 * to it.
 *
 * <p>The editor asks the facade for every section of a document each time it rebuilds its outline.
 * If asking created the section, opening a file and saving it straight back would grow an empty
 * {@code components:} or {@code securitySchemes:} block that was never in the file — noise in
 * exactly the git diff this editor exists to keep clean. So reads go through {@link #peek}, which
 * returns {@code null} for a section that is not there, and only writes go through {@link #create}.
 *
 * <p>Sections that every valid OAS document has anyway ({@code info}, {@code paths}) are not worth
 * this treatment and keep using {@link JsonNodes#objectChild}.
 */
public final class LazyObjectNode {

    private final ObjectNode root;
    private final List<String> path;

    private LazyObjectNode(ObjectNode root, List<String> path) {
        this.root = root;
        this.path = path;
    }

    public static LazyObjectNode of(ObjectNode root, String... path) {
        return new LazyObjectNode(root, List.of(path));
    }

    /** A named child of this object, itself created only when written to. */
    public LazyObjectNode child(String field) {
        List<String> longer = new ArrayList<>(path);
        longer.add(field);
        return new LazyObjectNode(root, List.copyOf(longer));
    }

    /** The node if every step of the path is present, otherwise {@code null}. Reads only. */
    public ObjectNode peek() {
        ObjectNode current = root;
        for (String field : path) {
            if (!(current.get(field) instanceof ObjectNode child)) {
                return null;
            }
            current = child;
        }
        return current;
    }

    /** The node, adding it and any missing ancestor to the document. */
    public ObjectNode create() {
        ObjectNode current = root;
        for (String field : path) {
            current = JsonNodes.objectChild(current, field);
        }
        return current;
    }
}
