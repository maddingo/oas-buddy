package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** One flow of an oauth2 scheme: its URLs and its scope map. */
public final class OAuthFlow {

    private static final String SCOPES_FIELD = "scopes";

    private final ObjectNode node;

    public OAuthFlow(ObjectNode node) {
        this.node = node;
    }

    public String getUrl(OAuthFlowUrl url) {
        return JsonNodes.text(node, url.field());
    }

    public void setUrl(OAuthFlowUrl url, String value) {
        JsonNodes.setText(node, url.field(), value);
    }

    /** The declared scopes, in document order. */
    public List<String> scopeNames() {
        List<String> names = new ArrayList<>();
        if (node.get(SCOPES_FIELD) instanceof ObjectNode scopes) {
            Iterator<String> it = scopes.fieldNames();
            while (it.hasNext()) {
                names.add(it.next());
            }
        }
        return names;
    }

    public String getScopeDescription(String scope) {
        return node.get(SCOPES_FIELD) instanceof ObjectNode scopes ? JsonNodes.text(scopes, scope) : null;
    }

    /** Adds the scope, or updates its description in place if it is already declared. */
    public void setScope(String scope, String description) {
        JsonNodes.objectChild(node, SCOPES_FIELD).put(scope, description == null ? "" : description);
    }

    public void removeScope(String scope) {
        if (node.get(SCOPES_FIELD) instanceof ObjectNode scopes) {
            scopes.remove(scope);
        }
    }

    /**
     * Renames a scope without moving it or losing its description. A no-op if {@code from} is not
     * declared or {@code to} already is; see {@link JsonNodes#renameField} for why this needs its
     * own helper rather than Jackson's remove-then-add.
     */
    public void renameScope(String from, String to) {
        if (node.get(SCOPES_FIELD) instanceof ObjectNode scopes) {
            JsonNodes.renameField(scopes, from, to);
        }
    }

    /** Whether the flow holds anything, i.e. whether discarding it would lose something. */
    public boolean isEmpty() {
        return node.isEmpty() || (scopeNames().isEmpty() && noUrls());
    }

    private boolean noUrls() {
        for (OAuthFlowUrl url : OAuthFlowUrl.values()) {
            if (getUrl(url) != null) {
                return false;
            }
        }
        return true;
    }
}
