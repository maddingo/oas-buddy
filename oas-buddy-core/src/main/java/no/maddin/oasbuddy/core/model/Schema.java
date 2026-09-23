package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Schema {

    /** Where a {@code $ref} to a schema declared in this document's components points. */
    public static final String COMPONENT_REF_PREFIX = "#/components/schemas/";

    private static final String TYPE = "type";
    private static final String FORMAT = "format";
    private static final String REF = "$ref";
    private static final String ITEMS = "items";
    private static final String ADDITIONAL_PROPERTIES = "additionalProperties";

    private final ObjectNode node;

    public Schema(ObjectNode node) {
        this.node = node;
    }

    public String getRef() {
        return JsonNodes.text(node, REF);
    }

    public void setRef(String ref) {
        JsonNodes.setText(node, REF, ref);
    }

    /**
     * @return the name of the component schema this schema refers to, or {@code null} if it is not
     *         a reference, or refers somewhere other than {@code #/components/schemas}
     */
    public String getReferencedSchemaName() {
        String ref = getRef();
        return ref != null && ref.startsWith(COMPONENT_REF_PREFIX)
                ? ref.substring(COMPONENT_REF_PREFIX.length())
                : null;
    }

    /**
     * Makes this schema a reference to a component schema, as a type picker does. The inline type
     * it replaces goes with it — {@code type}, {@code format} and {@code items}, which a
     * {@code $ref} would make dead weight — but nothing else: a description or an {@code x-}
     * extension is not the picker's to drop.
     */
    public void referTo(String schemaName) {
        node.remove(List.of(TYPE, FORMAT, ITEMS));
        setRef(COMPONENT_REF_PREFIX + schemaName);
    }

    public String getType() {
        return JsonNodes.text(node, TYPE);
    }

    public void setType(String type) {
        JsonNodes.setText(node, TYPE, type);
    }

    /**
     * Changes the type as a type picker does, dropping what the new type no longer uses: the
     * {@code $ref} it replaces, and {@code items} unless the new type is {@code array}.
     *
     * <p>{@link #setType} stays a plain setter, because a free-text field calls it on every
     * keystroke, and typing over {@code array} must not throw away its {@code items} on the way.
     */
    public void changeTypeTo(String type) {
        node.remove(REF);
        if (!"array".equals(type)) {
            node.remove(ITEMS);
        }
        setType(type);
    }

    public String getFormat() {
        return JsonNodes.text(node, FORMAT);
    }

    public void setFormat(String format) {
        JsonNodes.setText(node, FORMAT, format);
    }

    public String getDescription() {
        return JsonNodes.text(node, "description");
    }

    public void setDescription(String description) {
        JsonNodes.setText(node, "description", description);
    }

    public List<String> getRequired() {
        List<String> required = new ArrayList<>();
        var existing = node.get("required");
        if (existing instanceof ArrayNode arrayNode) {
            for (var element : arrayNode) {
                required.add(element.asText());
            }
        }
        return required;
    }

    public void setRequired(List<String> required) {
        ArrayNode array = JsonNodes.arrayChild(node, "required");
        array.removeAll();
        for (String name : required) {
            array.add(name);
        }
    }

    public List<String> propertyNames() {
        List<String> names = new ArrayList<>();
        var existing = node.get("properties");
        if (existing instanceof ObjectNode objectNode) {
            Iterator<String> it = objectNode.fieldNames();
            while (it.hasNext()) {
                names.add(it.next());
            }
        }
        return names;
    }

    public Schema getProperty(String name) {
        var properties = node.get("properties");
        if (!(properties instanceof ObjectNode objectNode)) {
            return null;
        }
        var existing = objectNode.get(name);
        return existing instanceof ObjectNode propertyNode ? new Schema(propertyNode) : null;
    }

    public Schema addProperty(String name) {
        ObjectNode properties = JsonNodes.objectChild(node, "properties");
        return new Schema(JsonNodes.objectChild(properties, name));
    }

    public void removeProperty(String name) {
        var properties = node.get("properties");
        if (properties instanceof ObjectNode objectNode) {
            objectNode.remove(name);
        }
    }

    /** The element schema of an array, or {@code null} if there is none. Reads only. */
    public Schema getItems() {
        return node.get(ITEMS) instanceof ObjectNode items ? new Schema(items) : null;
    }

    /** The element schema of an array, adding an empty one if there is none. */
    public Schema createItems() {
        return new Schema(JsonNodes.objectChild(node, ITEMS));
    }

    /**
     * @return the boolean form of {@code additionalProperties}, or {@code null} when it is absent
     *         or in its schema form
     */
    public Boolean getAdditionalPropertiesAllowed() {
        var value = node.get(ADDITIONAL_PROPERTIES);
        return value != null && value.isBoolean() ? value.asBoolean() : null;
    }

    /** Sets the boolean form, replacing a schema form; {@code null} removes the key. */
    public void setAdditionalPropertiesAllowed(Boolean allowed) {
        JsonNodes.setBool(node, ADDITIONAL_PROPERTIES, allowed);
    }

    /** The schema form of {@code additionalProperties}, or {@code null}. Reads only. */
    public Schema getAdditionalPropertiesSchema() {
        return node.get(ADDITIONAL_PROPERTIES) instanceof ObjectNode schema ? new Schema(schema) : null;
    }

    /** The schema form, replacing a boolean form with an empty schema if need be. */
    public Schema createAdditionalPropertiesSchema() {
        return new Schema(JsonNodes.objectChild(node, ADDITIONAL_PROPERTIES));
    }
}
