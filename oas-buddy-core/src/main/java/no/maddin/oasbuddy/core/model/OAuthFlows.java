package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;
import no.maddin.oasbuddy.core.document.LazyObjectNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The {@code flows} object of an oauth2 security scheme: up to four named flows, each with its own
 * URLs and scopes.
 */
public final class OAuthFlows {

    /** The flows OAS 3.0 defines, in the order the spec lists them. */
    public static final List<String> FLOW_NAMES =
            List.of("implicit", "password", "clientCredentials", "authorizationCode");

    /**
     * Which URLs each flow carries. Kept here rather than in the editor so the form renders what
     * the spec says instead of restating it: {@code implicit} has no token URL, and
     * {@code clientCredentials} has no authorization URL.
     */
    private static final Map<String, List<OAuthFlowUrl>> URLS_BY_FLOW = Map.of(
            "implicit", List.of(OAuthFlowUrl.AUTHORIZATION_URL, OAuthFlowUrl.REFRESH_URL),
            "password", List.of(OAuthFlowUrl.TOKEN_URL, OAuthFlowUrl.REFRESH_URL),
            "clientCredentials", List.of(OAuthFlowUrl.TOKEN_URL, OAuthFlowUrl.REFRESH_URL),
            "authorizationCode", List.of(OAuthFlowUrl.AUTHORIZATION_URL, OAuthFlowUrl.TOKEN_URL,
                    OAuthFlowUrl.REFRESH_URL));

    private final LazyObjectNode node;

    public OAuthFlows(LazyObjectNode node) {
        this.node = node;
    }

    /** The URLs the named flow defines, or empty for a name the spec does not know. */
    public static List<OAuthFlowUrl> urlsOf(String flowName) {
        return URLS_BY_FLOW.getOrDefault(flowName, List.of());
    }

    /** The flows actually present, in document order. */
    public List<String> names() {
        ObjectNode existing = node.peek();
        if (existing == null) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        Iterator<String> it = existing.fieldNames();
        while (it.hasNext()) {
            names.add(it.next());
        }
        return names;
    }

    public OAuthFlow getFlow(String flowName) {
        ObjectNode existing = node.peek();
        return existing != null && existing.get(flowName) instanceof ObjectNode flowNode
                ? new OAuthFlow(flowNode)
                : null;
    }

    public OAuthFlow addFlow(String flowName) {
        return new OAuthFlow(JsonNodes.objectChild(node.create(), flowName));
    }

    public void removeFlow(String flowName) {
        ObjectNode existing = node.peek();
        if (existing != null) {
            existing.remove(flowName);
        }
    }

    /** Whether any flow is declared, i.e. whether discarding the object would lose something. */
    public boolean isEmpty() {
        return names().isEmpty();
    }
}
