package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@code parameters} array — an operation's own, or a path item's, which applies to every
 * operation on that path. Each entry is inline or a reference to a component parameter.
 *
 * <p>Takes the parent and reads lazily, like {@link SecurityRequirements}: showing an operation must
 * not grow a {@code parameters: []} it never had, and removing the last entry takes the key out
 * again rather than leaving {@code []}, the same as every other collection in this codebase.
 *
 * <p>Entries are addressed by the {@link Parameter} handed out by {@link #all()} rather than by
 * index, so a non-object element in a hand-edited file cannot shift which entry an edit lands on.
 */
public final class Parameters {

    private static final String FIELD = "parameters";

    private final ObjectNode parent;

    public Parameters(ObjectNode parent) {
        this.parent = parent;
    }

    /** Every entry, inline and referenced alike, in document order. */
    public List<Parameter> all() {
        List<Parameter> parameters = new ArrayList<>();
        if (parent.get(FIELD) instanceof ArrayNode array) {
            for (JsonNode element : array) {
                if (element instanceof ObjectNode objectNode) {
                    parameters.add(new Parameter(objectNode));
                }
            }
        }
        return parameters;
    }

    public Parameter add(String name, String in) {
        Parameter parameter = new Parameter(array().addObject());
        parameter.setName(name);
        parameter.setIn(in);
        return parameter;
    }

    /** Adds a reference to a component parameter at the end. */
    public Parameter addReference(String componentName) {
        ObjectNode ref = array().addObject();
        ref.put("$ref", ComponentReferences.ref(ComponentParameters.SECTION, componentName));
        return new Parameter(ref);
    }

    /** Removes the entry, and the key with it once it was the last. */
    public void remove(Parameter parameter) {
        if (parent.get(FIELD) instanceof ArrayNode array) {
            int index = indexOf(array, parameter);
            if (index >= 0) {
                array.remove(index);
            }
            if (array.isEmpty()) {
                parent.remove(FIELD);
            }
        }
    }

    /**
     * Replaces the entry with a reference to a component parameter, in place — the inline
     * definition goes, so a caller that cares asks first.
     *
     * @return the new entry, or {@code null} if {@code parameter} is not in this list
     */
    public Parameter referTo(Parameter parameter, String componentName) {
        ObjectNode ref = JsonNodeFactory.instance.objectNode();
        ref.put("$ref", ComponentReferences.ref(ComponentParameters.SECTION, componentName));
        return replace(parameter, ref);
    }

    /**
     * Replaces the entry with an inline definition, in place, starting from a copy of
     * {@code template} — the parameter the reference pointed at, so nothing is lost — or, without
     * one, from a bare query parameter named {@code newParam}.
     *
     * @return the new entry, or {@code null} if {@code parameter} is not in this list
     */
    public Parameter defineInline(Parameter parameter, Parameter template) {
        ObjectNode inline;
        if (template != null) {
            inline = template.node().deepCopy();
        } else {
            inline = JsonNodeFactory.instance.objectNode();
            inline.put("name", "newParam");
            inline.put("in", "query");
        }
        return replace(parameter, inline);
    }

    private Parameter replace(Parameter parameter, ObjectNode replacement) {
        if (parent.get(FIELD) instanceof ArrayNode array) {
            int index = indexOf(array, parameter);
            if (index >= 0) {
                array.set(index, replacement);
                return new Parameter(replacement);
            }
        }
        return null;
    }

    /** By identity: two parameters with equal content are still two different entries. */
    private static int indexOf(ArrayNode array, Parameter parameter) {
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i) == parameter.node()) {
                return i;
            }
        }
        return -1;
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
