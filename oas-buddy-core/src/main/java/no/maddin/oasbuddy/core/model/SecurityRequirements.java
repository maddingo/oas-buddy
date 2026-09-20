package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@code security} array, either the document default or an operation's override.
 *
 * <p>Three states matter, and the spec distinguishes all three:
 * <ul>
 *   <li>no {@code security} key — an operation inherits the document default;
 *   <li>an empty array — explicitly public, overriding the default with nothing;
 *   <li>a non-empty array — any one of those requirements is enough.
 * </ul>
 *
 * <p>Collapsing the first two would silently change what an API requires, so the key is created
 * only by a write: {@link #isDeclared()} and {@link #requirements()} never add one. That is why this
 * takes its parent and field name rather than a node — the array may not exist yet.
 */
public final class SecurityRequirements {

    private static final String FIELD = "security";

    private final ObjectNode parent;

    public SecurityRequirements(ObjectNode parent) {
        this.parent = parent;
    }

    /** Whether the {@code security} key is present at all. */
    public boolean isDeclared() {
        return parent.get(FIELD) instanceof ArrayNode;
    }

    /** The declared alternatives, in document order; empty when nothing is declared. */
    public List<SecurityRequirement> requirements() {
        List<SecurityRequirement> requirements = new ArrayList<>();
        if (parent.get(FIELD) instanceof ArrayNode array) {
            for (var element : array) {
                if (element instanceof ObjectNode objectNode) {
                    requirements.add(new SecurityRequirement(objectNode));
                }
            }
        }
        return requirements;
    }

    /** Adds an alternative requiring the named scheme, with no scopes. */
    public SecurityRequirement add(String schemeName) {
        ObjectNode requirement = array().addObject();
        requirement.set(schemeName, JsonNodeFactory.instance.arrayNode());
        return new SecurityRequirement(requirement);
    }

    public void remove(int index) {
        if (parent.get(FIELD) instanceof ArrayNode array && index >= 0 && index < array.size()) {
            array.remove(index);
        }
    }

    /** Declares an empty array: explicitly public, rather than inheriting. */
    public void declarePublic() {
        array().removeAll();
    }

    /** Removes the key entirely, so an operation inherits the document default again. */
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
