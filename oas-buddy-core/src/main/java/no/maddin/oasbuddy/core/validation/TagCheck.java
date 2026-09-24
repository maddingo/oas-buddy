package no.maddin.oasbuddy.core.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.model.HttpMethod;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks the one tag problem swagger-parser leaves alone: an operation naming a tag the root
 * {@code tags} array never declares.
 *
 * <p>A duplicate name in the root list is <strong>not</strong> re-checked here — swagger-parser
 * already reports {@code attribute tags.<name> is repeated} for both YAML and JSON (verified), so
 * doing it again would just show the same problem twice under two different messages. That is the
 * mirror image of why {@code ConstraintCheck} exists at all: that one fills a gap swagger-parser
 * leaves, this one must not paper over ground it already covers.
 *
 * <p>An operation naming an undefined tag is not rewritten on load — see {@code core.model.Tags} —
 * so this is where it gets reported instead, at {@link ValidationSeverity#WARNING}: OAS does not
 * actually require a tag an operation uses to be declared at the root, unlike a duplicate root name,
 * which is a real spec violation and stays an error (reported by swagger-parser itself, not here).
 */
final class TagCheck {

    private TagCheck() {
    }

    static List<ValidationMessage> check(JsonNode root) {
        List<ValidationMessage> messages = new ArrayList<>();
        Set<String> defined = definedNames(root);
        if (root.path("paths") instanceof ObjectNode paths) {
            for (Map.Entry<String, JsonNode> pathEntry : paths.properties()) {
                checkPathItem(pathEntry.getKey(), pathEntry.getValue(), defined, messages);
            }
        }
        return messages;
    }

    private static Set<String> definedNames(JsonNode root) {
        Set<String> defined = new HashSet<>();
        if (root.path("tags") instanceof ArrayNode tags) {
            for (JsonNode tag : tags) {
                String name = tag.path("name").asText(null);
                if (name != null) {
                    defined.add(name);
                }
            }
        }
        return defined;
    }

    private static void checkPathItem(String path, JsonNode pathItem, Set<String> defined,
                                      List<ValidationMessage> messages) {
        if (!(pathItem instanceof ObjectNode pathItemNode)) {
            return;
        }
        for (HttpMethod method : HttpMethod.values()) {
            if (pathItemNode.get(method.fieldName()) instanceof ObjectNode operation
                    && operation.get("tags") instanceof ArrayNode tags) {
                for (JsonNode tagNode : tags) {
                    String tag = tagNode.asText(null);
                    if (tag != null && !defined.contains(tag)) {
                        messages.add(warning(method.name() + " " + path
                                + " uses undefined tag \"" + tag + "\"."));
                    }
                }
            }
        }
    }

    private static ValidationMessage warning(String message) {
        return new ValidationMessage(ValidationSeverity.WARNING, message);
    }
}
