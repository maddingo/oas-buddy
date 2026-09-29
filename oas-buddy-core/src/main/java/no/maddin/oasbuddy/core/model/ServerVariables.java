package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

import java.util.ArrayList;
import java.util.List;

/**
 * A server's {@code variables} map. Lazy like {@link Tags}: a read never adds the key, and removing
 * the last variable takes it out again rather than leaving {@code variables: {}}.
 */
public final class ServerVariables {

    private static final String FIELD = "variables";

    private final ObjectNode server;

    public ServerVariables(ObjectNode server) {
        this.server = server;
    }

    /** Every key, including one whose value is not an object, so the file is never hidden from the user. */
    public List<String> names() {
        List<String> names = new ArrayList<>();
        if (server.get(FIELD) instanceof ObjectNode variables) {
            variables.fieldNames().forEachRemaining(names::add);
        }
        return names;
    }

    /** The named variable, or {@code null} if it is absent or not an object. */
    public ServerVariable get(String name) {
        return server.get(FIELD) instanceof ObjectNode variables && variables.get(name) instanceof ObjectNode node
                ? new ServerVariable(node) : null;
    }

    /** Adds a variable with an empty default (the spec requires one), or returns the existing one. */
    public ServerVariable add(String name) {
        ServerVariable existing = get(name);
        if (existing != null) {
            return existing;
        }
        ObjectNode node = JsonNodes.objectChild(JsonNodes.objectChild(server, FIELD), name);
        node.put("default", "");
        return new ServerVariable(node);
    }

    public void remove(String name) {
        if (server.get(FIELD) instanceof ObjectNode variables) {
            variables.remove(name);
            if (variables.isEmpty()) {
                server.remove(FIELD);
            }
        }
    }

    /** In place, so the saved file is not reordered; {@code false} means refused. See {@link JsonNodes#renameField}. */
    public boolean rename(String from, String to) {
        return server.get(FIELD) instanceof ObjectNode variables && JsonNodes.renameField(variables, from, to);
    }
}
