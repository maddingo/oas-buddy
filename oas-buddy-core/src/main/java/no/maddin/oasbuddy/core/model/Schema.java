package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
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
    private static final String ENUM = "enum";
    private static final String DEFAULT = "default";
    private static final String EXAMPLE = "example";

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

    public String getTitle() {
        return JsonNodes.text(node, "title");
    }

    public void setTitle(String title) {
        JsonNodes.setText(node, "title", title);
    }

    /** The allowed values, in order, as typed values (see {@link SchemaValues}). Empty if none. */
    public List<JsonNode> getEnum() {
        List<JsonNode> values = new ArrayList<>();
        if (node.get(ENUM) instanceof ArrayNode arrayNode) {
            arrayNode.forEach(values::add);
        }
        return values;
    }

    /** Replaces the allowed values; an empty list removes {@code enum} rather than leaving {@code []}. */
    public void setEnum(List<JsonNode> values) {
        if (values.isEmpty()) {
            node.remove(ENUM);
            return;
        }
        ArrayNode array = JsonNodes.arrayChild(node, ENUM);
        array.removeAll();
        values.forEach(array::add);
    }

    /**
     * @return the value, or {@code null} if absent. A JSON {@code null} is a legitimate default and
     *         comes back as a {@code NullNode}, not as {@code null}.
     */
    public JsonNode getDefault() {
        return node.get(DEFAULT);
    }

    /** {@code null} removes the key. */
    public void setDefault(JsonNode value) {
        setValue(DEFAULT, value);
    }

    public JsonNode getExample() {
        return node.get(EXAMPLE);
    }

    /** {@code null} removes the key. */
    public void setExample(JsonNode value) {
        setValue(EXAMPLE, value);
    }

    public boolean isNullable() {
        return flag("nullable");
    }

    public void setNullable(boolean nullable) {
        setFlag("nullable", nullable);
    }

    public boolean isReadOnly() {
        return flag("readOnly");
    }

    public void setReadOnly(boolean readOnly) {
        setFlag("readOnly", readOnly);
    }

    public boolean isWriteOnly() {
        return flag("writeOnly");
    }

    public void setWriteOnly(boolean writeOnly) {
        setFlag("writeOnly", writeOnly);
    }

    public boolean isDeprecated() {
        return flag("deprecated");
    }

    public void setDeprecated(boolean deprecated) {
        setFlag("deprecated", deprecated);
    }

    /** @return the constraint's value as written, or {@code null} if absent */
    public JsonNode getConstraint(Constraint constraint) {
        return node.get(constraint.key());
    }

    /** {@code null} removes the key. */
    public void setConstraint(Constraint constraint, JsonNode value) {
        setValue(constraint.key(), value);
    }

    /**
     * The constraints present on this schema that do not apply to its type. Changing the type
     * hides a constraint rather than deleting it, so an integer that becomes a number and back
     * keeps its bounds; this is what lets the editor say what it is not showing.
     */
    public List<Constraint> constraintsNotApplying() {
        String type = getType();
        List<Constraint> present = new ArrayList<>();
        for (Constraint constraint : Constraint.values()) {
            if (node.has(constraint.key()) && !constraint.appliesTo(type)) {
                present.add(constraint);
            }
        }
        return present;
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

    /**
     * Renames a property, keeping its position and body, and renames it in {@code required} too
     * (in place, at the same index — a JSON array supports that natively, unlike an object field).
     *
     * @return {@code false}, changing nothing, if {@code from} does not exist or {@code to} is
     *         already taken (a collision is refused, never silently overwritten)
     */
    public boolean renameProperty(String from, String to) {
        var properties = node.get("properties");
        if (!(properties instanceof ObjectNode objectNode) || !JsonNodes.renameField(objectNode, from, to)) {
            return false;
        }
        if (node.get("required") instanceof ArrayNode required) {
            for (int i = 0; i < required.size(); i++) {
                if (from.equals(required.get(i).asText())) {
                    required.set(i, TextNode.valueOf(to));
                }
            }
        }
        return true;
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

    private void setValue(String field, JsonNode value) {
        if (value == null) {
            node.remove(field);
        } else {
            node.set(field, value);
        }
    }

    private boolean flag(String field) {
        return Boolean.TRUE.equals(JsonNodes.bool(node, field));
    }

    /**
     * Every flag defaults to {@code false}, so only {@code true} is written: an untouched schema
     * must not gain a {@code readOnly: false} it never had.
     */
    private void setFlag(String field, boolean value) {
        JsonNodes.setBool(node, field, value ? Boolean.TRUE : null);
    }
}
