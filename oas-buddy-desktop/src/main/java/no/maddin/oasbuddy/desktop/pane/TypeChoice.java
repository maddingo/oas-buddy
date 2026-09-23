package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.model.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * One entry of a schema type picker: either a JSON type or a reference to a component schema.
 * Keeping both in one picker lets a property be pointed at another schema in a single choice.
 */
record TypeChoice(String type, String schemaName) {

    static TypeChoice type(String type) {
        return new TypeChoice(type, null);
    }

    static TypeChoice ref(String schemaName) {
        return new TypeChoice(null, schemaName);
    }

    /** The given types followed by a reference to each of the given schemas. */
    static List<TypeChoice> choices(List<String> types, List<String> schemaNames) {
        List<TypeChoice> choices = new ArrayList<>();
        types.forEach(type -> choices.add(type(type)));
        schemaNames.forEach(name -> choices.add(ref(name)));
        return choices;
    }

    /** What the schema is now, or {@code null} if it is neither a typed schema nor a component reference. */
    static TypeChoice of(Schema schema) {
        String schemaName = schema.getReferencedSchemaName();
        if (schemaName != null) {
            return ref(schemaName);
        }
        return schema.getType() == null ? null : type(schema.getType());
    }

    boolean isReference() {
        return schemaName != null;
    }

    boolean isArray() {
        return "array".equals(type);
    }

    void applyTo(Schema schema) {
        if (isReference()) {
            schema.referTo(schemaName);
        } else {
            schema.changeTypeTo(type);
        }
    }

    /** How the picker shows it; a reference is marked so a schema named like a type stays distinguishable. */
    @Override
    public String toString() {
        return isReference() ? "→ " + schemaName : type;
    }
}
