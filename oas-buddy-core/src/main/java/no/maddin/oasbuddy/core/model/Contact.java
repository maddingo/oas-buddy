package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

public final class Contact {

    private final ObjectNode node;

    public Contact(ObjectNode node) {
        this.node = node;
    }

    public String getName() {
        return JsonNodes.text(node, "name");
    }

    public void setName(String name) {
        JsonNodes.setText(node, "name", name);
    }

    public String getUrl() {
        return JsonNodes.text(node, "url");
    }

    public void setUrl(String url) {
        JsonNodes.setText(node, "url", url);
    }

    public String getEmail() {
        return JsonNodes.text(node, "email");
    }

    public void setEmail(String email) {
        JsonNodes.setText(node, "email", email);
    }
}
