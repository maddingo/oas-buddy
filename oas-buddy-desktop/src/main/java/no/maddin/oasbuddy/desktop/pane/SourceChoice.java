package no.maddin.oasbuddy.desktop.pane;

import java.util.List;

/**
 * One entry of a "defined where" picker: inline ({@code name == null}) or a reference to the named
 * component. Shared by operation responses and by operation and path parameters.
 */
record SourceChoice(String name) {

    static final SourceChoice INLINE = new SourceChoice(null);

    /** Inline, then a reference to each component, then {@code current} if it is none of those. */
    static List<SourceChoice> choices(List<String> componentNames, SourceChoice current) {
        List<SourceChoice> choices = new java.util.ArrayList<>();
        choices.add(INLINE);
        componentNames.forEach(name -> choices.add(new SourceChoice(name)));
        if (!choices.contains(current)) {
            // a reference to something not declared: shown as it is, so the form agrees with the file
            choices.add(current);
        }
        return choices;
    }

    boolean isInline() {
        return name == null;
    }

    /** A reference is marked the same way {@link TypeChoice} marks one. */
    @Override
    public String toString() {
        return isInline() ? "Inline" : "→ " + name;
    }
}
