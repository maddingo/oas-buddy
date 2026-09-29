package no.maddin.oasbuddy.core.model;

import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.JsonNodes;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Server {

    private final ObjectNode node;

    public Server(ObjectNode node) {
        this.node = node;
    }

    private static final Pattern TEMPLATE = Pattern.compile("\\{([^{}]+)}");

    ObjectNode node() {
        return node;
    }

    public ServerVariables getVariables() {
        return new ServerVariables(node);
    }

    /** The {@code {names}} the URL templates, in order of first appearance. */
    public List<String> templateNames() {
        List<String> names = new ArrayList<>();
        String url = getUrl();
        if (url != null) {
            Matcher matcher = TEMPLATE.matcher(url);
            while (matcher.find()) {
                if (!names.contains(matcher.group(1))) {
                    names.add(matcher.group(1));
                }
            }
        }
        return names;
    }

    /** Template names with no entry in {@code variables}; the spec makes such a URL invalid. */
    public List<String> undefinedVariables() {
        List<String> defined = getVariables().names();
        return templateNames().stream().filter(name -> !defined.contains(name)).toList();
    }

    public String getUrl() {
        return JsonNodes.text(node, "url");
    }

    public void setUrl(String url) {
        JsonNodes.setText(node, "url", url);
    }

    public String getDescription() {
        return JsonNodes.text(node, "description");
    }

    public void setDescription(String description) {
        JsonNodes.setText(node, "description", description);
    }
}
