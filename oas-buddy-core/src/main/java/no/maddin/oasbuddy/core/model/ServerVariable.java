package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

import java.util.ArrayList;
import java.util.List;

/** One entry of a server's {@code variables}: an optional {@code enum}, a {@code default} and a description. */
public final class ServerVariable {

    private final ObjectNode node;

    public ServerVariable(ObjectNode node) {
        this.node = node;
    }

    public List<String> getEnum() {
        List<String> values = new ArrayList<>();
        if (node.get("enum") instanceof ArrayNode array) {
            for (var element : array) {
                values.add(element.asText());
            }
        }
        return values;
    }

    /** An empty list removes the {@code enum} key rather than leaving {@code []}. */
    public void setEnum(List<String> values) {
        if (values.isEmpty()) {
            node.remove("enum");
            return;
        }
        ArrayNode array = JsonNodes.arrayChild(node, "enum");
        array.removeAll();
        values.forEach(array::add);
    }

    public String getDefault() {
        return JsonNodes.text(node, "default");
    }

    public void setDefault(String value) {
        JsonNodes.setText(node, "default", value);
    }

    public String getDescription() {
        return JsonNodes.text(node, "description");
    }

    public void setDescription(String description) {
        JsonNodes.setText(node, "description", description);
    }
}
