package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Paths {

    private final ObjectNode node;

    public Paths(ObjectNode node) {
        this.node = node;
    }

    public List<String> pathNames() {
        List<String> names = new ArrayList<>();
        Iterator<String> it = node.fieldNames();
        while (it.hasNext()) {
            names.add(it.next());
        }
        return names;
    }

    public PathItem getPathItem(String path) {
        var existing = node.get(path);
        return existing instanceof ObjectNode objectNode ? new PathItem(objectNode) : null;
    }

    public PathItem addPath(String path) {
        return new PathItem(JsonNodes.objectChild(node, path));
    }

    public void removePath(String path) {
        node.remove(path);
    }

    /**
     * Renames a path, keeping its position in the document. Nothing can {@code $ref} a path, so
     * unlike a schema rename there is nothing else in the document to fix up.
     *
     * @return {@code false}, changing nothing, if {@code from} does not exist or {@code to} is
     *         already taken (a collision is refused, never silently overwritten)
     */
    public boolean renamePath(String from, String to) {
        return JsonNodes.renameField(node, from, to);
    }
}
