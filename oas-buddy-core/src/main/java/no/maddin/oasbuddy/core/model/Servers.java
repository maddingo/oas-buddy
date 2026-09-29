package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@code servers} array: the document default, or an override on a path item or an operation.
 *
 * <p>Like {@link SecurityRequirements} it takes its parent and never creates the key on a read.
 * At document level absent and empty mean the same thing, but for an override they differ — no key
 * inherits the document's servers, an empty array overrides them with none — so {@link #isDeclared}
 * and {@link #undeclare} are separate from adding and removing entries, and removing the last entry
 * never removes the key by itself.
 *
 * <p>Entries are addressed by the {@link Server} object (node identity), not by index, so a
 * non-object element in a hand-edited array cannot shift which entry an edit lands on.
 */
public final class Servers {

    private static final String FIELD = "servers";

    private final ObjectNode parent;

    public Servers(ObjectNode parent) {
        this.parent = parent;
    }

    /** Whether the {@code servers} key is present at all. */
    public boolean isDeclared() {
        return parent.get(FIELD) instanceof ArrayNode;
    }

    /** The declared servers, in document order; empty when nothing is declared. */
    public List<Server> all() {
        List<Server> servers = new ArrayList<>();
        if (parent.get(FIELD) instanceof ArrayNode array) {
            for (var element : array) {
                if (element instanceof ObjectNode objectNode) {
                    servers.add(new Server(objectNode));
                }
            }
        }
        return servers;
    }

    public Server add(String url) {
        ObjectNode node = array().addObject();
        node.put("url", url);
        return new Server(node);
    }

    public void remove(Server server) {
        if (parent.get(FIELD) instanceof ArrayNode array) {
            for (int i = 0; i < array.size(); i++) {
                if (array.get(i) == server.node()) {
                    array.remove(i);
                    return;
                }
            }
        }
    }

    /** Declares an empty array: an override with no servers, rather than inheriting. */
    public void declare() {
        array();
    }

    /** Removes the key entirely, so the document's servers apply again. */
    public void undeclare() {
        parent.remove(FIELD);
    }

    private ArrayNode array() {
        if (parent.get(FIELD) instanceof ArrayNode existing) {
            return existing;
        }
        ArrayNode created = JsonNodeFactory.instance.arrayNode();
        parent.set(FIELD, created);
        return created;
    }
}
