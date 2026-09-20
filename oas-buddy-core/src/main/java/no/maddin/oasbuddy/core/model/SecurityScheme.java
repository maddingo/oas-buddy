package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

import java.util.List;
import java.util.Map;

/**
 * One entry of {@code components.securitySchemes}.
 *
 * <p>OAS 3.0 defines five types, of which this covers the three whose shape is a handful of flat
 * strings: {@code apiKey}, {@code http} and {@code openIdConnect}. {@code oauth2} nests a
 * {@code flows} object and is deliberately not editable yet; a loaded oauth2 scheme is still
 * readable here and, more importantly, is left exactly as it was found.
 */
public final class SecurityScheme {

    /** The types this editor has a form for, in the order the type picker offers them. */
    public static final List<String> EDITABLE_TYPES = List.of("apiKey", "http", "openIdConnect");

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
