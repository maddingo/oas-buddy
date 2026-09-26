package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;
import no.maddin.oasbuddy.core.document.LazyObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * One entry of {@code components.securitySchemes}.
 *
 * <p>OAS 3.0 defines five types, of which this covers four: the three whose shape is a handful of
 * flat strings ({@code apiKey}, {@code http}, {@code openIdConnect}) and {@code oauth2}, whose
 * nested {@code flows} object is reached through {@link #getFlows()}. A scheme of any other type —
 * {@code mutualTLS}, or a typo — stays readable here and is left exactly as it was found.
 */
public final class SecurityScheme {

    /** The types this editor has a form for, in the order the type picker offers them. */
    public static final List<String> EDITABLE_TYPES = List.of("apiKey", "http", "oauth2", "openIdConnect");

    /** Where an {@code apiKey} scheme carries its key, per OAS 3.0. */
    public static final List<String> API_KEY_LOCATIONS = List.of("query", "header", "cookie");

    /**
     * Each editable type's own fields. Changing type strips the fields of the other types in this
     * map and nothing else, so anything the editor does not understand — an oauth2 {@code flows},
     * an {@code x-} extension — survives untouched.
     */
    private static final Map<String, List<String>> FIELDS_BY_TYPE = Map.of(
            "apiKey", List.of("name", "in"),
            "http", List.of("scheme", "bearerFormat"),
            "oauth2", List.of("flows"),
            "openIdConnect", List.of("openIdConnectUrl"));

    private final ObjectNode node;

    public SecurityScheme(ObjectNode node) {
        this.node = node;
    }

    public String getType() {
        return JsonNodes.text(node, "type");
    }

    /**
     * Sets the type and drops the fields belonging to the other editable types.
     *
     * <p>Leaving them would be worse than losing them: a stale {@code name} on an {@code http}
     * scheme is invisible in the form, because that field is not rendered for http, yet it would
     * still be written to the file.
     */
    public void setType(String type) {
        JsonNodes.setText(node, "type", type);
        FIELDS_BY_TYPE.forEach((candidate, fields) -> {
            if (!candidate.equals(type)) {
                fields.forEach(node::remove);
            }
        });
    }

    /** Whether this editor has a form for this scheme's type. */
    public boolean isEditable() {
        return EDITABLE_TYPES.contains(getType());
    }

    /**
     * The oauth2 flows, resolved lazily: the {@code flows} object is added only once a flow is
     * actually declared, so reading an oauth2 scheme never changes it.
     */
    public OAuthFlows getFlows() {
        return new OAuthFlows(LazyObjectNode.of(node, "flows"));
    }

    /**
     * The scopes a requirement on this scheme may ask for: the union of its flows' scopes, in
     * document order, or empty for a type that has none.
     *
     * <p>The union rather than one flow's scopes, because a requirement names the scheme and not a
     * flow, so a scope any flow defines is legitimate to require. A flow whose value is not an
     * object ({@code implicit: ~}) defines no scopes and is skipped.
     */
    public List<String> declaredScopes() {
        if (!"oauth2".equals(getType())) {
            return List.of();
        }
        OAuthFlows flows = getFlows();
        List<String> scopes = new ArrayList<>();
        for (String flowName : flows.names()) {
            OAuthFlow flow = flows.getFlow(flowName);
            if (flow == null) {
                continue;
            }
            for (String scope : flow.scopeNames()) {
                if (!scopes.contains(scope)) {
                    scopes.add(scope);
                }
            }
        }
        return List.copyOf(scopes);
    }

    public String getDescription() {
        return JsonNodes.text(node, "description");
    }

    public void setDescription(String description) {
        JsonNodes.setText(node, "description", description);
    }

    public String getName() {
        return JsonNodes.text(node, "name");
    }

    public void setName(String name) {
        JsonNodes.setText(node, "name", name);
    }

    public String getIn() {
        return JsonNodes.text(node, "in");
    }

    public void setIn(String in) {
        JsonNodes.setText(node, "in", in);
    }

    public String getScheme() {
        return JsonNodes.text(node, "scheme");
    }

    public void setScheme(String scheme) {
        JsonNodes.setText(node, "scheme", scheme);
    }

    public String getBearerFormat() {
        return JsonNodes.text(node, "bearerFormat");
    }

    public void setBearerFormat(String bearerFormat) {
        JsonNodes.setText(node, "bearerFormat", bearerFormat);
    }

    public String getOpenIdConnectUrl() {
        return JsonNodes.text(node, "openIdConnectUrl");
    }

    public void setOpenIdConnectUrl(String openIdConnectUrl) {
        JsonNodes.setText(node, "openIdConnectUrl", openIdConnectUrl);
    }
}
