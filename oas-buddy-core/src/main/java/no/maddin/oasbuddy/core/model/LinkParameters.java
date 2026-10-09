package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A link's {@code parameters}: target parameter name → runtime expression (or constant). Lazy, and
 * the key goes with the last entry, like every other named map here.
 */
public final class LinkParameters {

    private static final String FIELD = "parameters";

    private final ObjectNode parent;

    LinkParameters(ObjectNode parent) {
        this.parent = parent;
    }

    public List<String> names() {
        List<String> names = new ArrayList<>();
        if (parent.get(FIELD) instanceof ObjectNode parameters) {
            Iterator<String> it = parameters.fieldNames();
            while (it.hasNext()) {
                names.add(it.next());
            }
        }
        return names;
    }

    /** The value, or {@code null} if there is no such parameter. */
    public JsonNode get(String name) {
        return parent.get(FIELD) instanceof ObjectNode parameters ? parameters.get(name) : null;
    }

    /** Adds or replaces; an existing key keeps its position. */
    public void put(String name, JsonNode value) {
        JsonNodes.objectChild(parent, FIELD).set(name, value);
    }

    public void remove(String name) {
        if (parent.get(FIELD) instanceof ObjectNode parameters) {
            parameters.remove(name);
            if (parameters.isEmpty()) {
                parent.remove(FIELD);
            }
        }
    }

    /** In place, so the saved file is not reordered; {@code false} means refused. See {@link JsonNodes#renameField}. */
    public boolean rename(String from, String to) {
        return parent.get(FIELD) instanceof ObjectNode parameters && JsonNodes.renameField(parameters, from, to);
    }
}
