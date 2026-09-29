package no.maddin.oasbuddy.desktop.pane;

import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import no.maddin.oasbuddy.core.model.Parameter;
import no.maddin.oasbuddy.core.model.ParameterStyles;
import no.maddin.oasbuddy.core.model.Schema;
import no.maddin.oasbuddy.core.model.SchemaValues;
import atlantafx.base.theme.Styles;

import java.util.ArrayList;
import java.util.List;

/**
 * How an inline parameter is serialized on the wire: {@code style}, {@code explode},
 * {@code allowReserved}, {@code allowEmptyValue}, {@code deprecated}, and its example(s).
 *
 * <p>Only the styles the parameter's {@code in} allows are offered, and {@code explode} shows what
 * its absence means for the effective style instead of a blank box. The section is rebuilt when the
 * location changes ({@link #refresh}) — a picker has no caret to fight — so both always agree with
 * {@code ParameterStyles}. A style already in the file that the location does not allow is still
 * shown, marked, so the form never disagrees with the file. A parameter using {@code content}
 * instead of {@code schema} is preserved and marked as not yet editable.
 *
 * <p>Nothing here carries an id: several parameters are on screen at once.
 */
final class ParameterSerialization {

    static final String STYLE_CLASS = "parameter-style";
    static final String EXPLODE_CLASS = "parameter-explode";
    static final String ALLOW_RESERVED_CLASS = "parameter-allow-reserved";
    static final String ALLOW_EMPTY_CLASS = "parameter-allow-empty-value";
    static final String DEPRECATED_CLASS = "parameter-deprecated";
    static final String EXAMPLE_CLASS = "parameter-example";
    static final String CONTENT_NOTE_CLASS = "parameter-content-note";

    private final Parameter parameter;
    private final ComponentCatalog catalog;
    private final RemovalConfirmation confirmation;
    private final TitledPane section = new TitledPane();

    ParameterSerialization(Parameter parameter, ComponentCatalog catalog, RemovalConfirmation confirmation) {
        this.parameter = parameter;
        this.catalog = catalog;
        this.confirmation = confirmation;
        section.setText("Serialization");
        section.setExpanded(hasAnythingToShow());
        refresh();
    }

    Node node() {
        return section;
    }

    /** Rebuilds the fields from the parameter; call after its {@code in} changed. */
    void refresh() {
        GridPane grid = FormFields.grid();
        int row = 0;

        if (parameter.hasContent()) {
            Label note = new Label("Its value is defined by content ("
                    + String.join(", ", parameter.getContent().mediaTypes())
                    + ") instead of a schema. That cannot be edited yet; it is left exactly as it was loaded.");
            note.getStyleClass().addAll(Styles.TEXT_MUTED, CONTENT_NOTE_CLASS);
            note.setWrapText(true);
            grid.add(note, 0, row++, 2, 1);
        } else {
            grid.addRow(row++, new Label("Style"), styleBox());
            grid.addRow(row++, new Label("Explode"), explodeBox());
        }
        if (ParameterStyles.allowsReservedAndEmpty(parameter.getIn())) {
            CheckBox reserved = flag(ALLOW_RESERVED_CLASS, parameter.isAllowReserved(), parameter::setAllowReserved);
            grid.addRow(row++, new Label("Allow reserved"), reserved);
            CheckBox empty = flag(ALLOW_EMPTY_CLASS, parameter.isAllowEmptyValue(), parameter::setAllowEmptyValue);
            grid.addRow(row++, new Label("Allow empty value"), empty);
        }
        grid.addRow(row++, new Label("Deprecated"),
                flag(DEPRECATED_CLASS, parameter.isDeprecated(), parameter::setDeprecated));

        TextField example = new TextField(SchemaValues.display(parameter.getExample()));
        example.getStyleClass().add(EXAMPLE_CLASS);
        example.setPromptText("typed as the schema's type");
        example.textProperty().addListener((obs, oldVal, newVal) ->
                parameter.setExample(SchemaValues.parse(newVal, type())));
        grid.addRow(row++, new Label("Example"), example);
        grid.addRow(row, new Label("Examples"),
                ExamplesEditor.build(parameter.getExamples(), this::type, catalog, confirmation, this::refresh));

        section.setContent(new VBox(grid));
    }

    private String type() {
        Schema schema = parameter.findSchema();
        return schema == null ? null : schema.getType();
    }

    private boolean hasAnythingToShow() {
        return parameter.getStyle() != null || parameter.getExplode() != null || parameter.isAllowReserved()
                || parameter.isAllowEmptyValue() || parameter.isDeprecated() || parameter.getExample() != null
                || !parameter.getExamples().names().isEmpty() || parameter.hasContent();
    }

    private static CheckBox flag(String styleClass, boolean selected, java.util.function.Consumer<Boolean> setter) {
        CheckBox box = new CheckBox();
        box.getStyleClass().add(styleClass);
        box.setSelected(selected);
        box.selectedProperty().addListener((obs, was, now) -> setter.accept(now));
        return box;
    }

    /** {@code null} style means "the default for this location", which is what an absent key says. */
    record StyleChoice(String style, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private ComboBox<StyleChoice> styleBox() {
        String in = parameter.getIn();
        String defaultStyle = ParameterStyles.defaultFor(in);
        List<StyleChoice> choices = new ArrayList<>();
        choices.add(new StyleChoice(null, defaultStyle == null ? "Default" : "Default (" + defaultStyle + ")"));
        ParameterStyles.validFor(in).forEach(style -> choices.add(new StyleChoice(style, style)));

        String written = parameter.getStyle();
        StyleChoice current = choices.stream().filter(c -> java.util.Objects.equals(c.style(), written))
                .findFirst().orElse(null);
        if (current == null) {
            // a style the location does not allow, as loaded: shown and marked, not silently dropped
            current = new StyleChoice(written, written + " (not valid for " + in + ")");
            choices.add(current);
        }

        ComboBox<StyleChoice> box = new ComboBox<>();
        box.getStyleClass().add(STYLE_CLASS);
        box.getItems().addAll(choices);
        box.setValue(current);
        box.valueProperty().addListener((obs, was, now) -> {
            if (now != null) {
                parameter.setStyle(now.style());
                refresh();   // the effective explode default follows the style
            }
        });
        return box;
    }

    /** {@code null} explode means "the default for the effective style". */
    record ExplodeChoice(Boolean explode, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private ComboBox<ExplodeChoice> explodeBox() {
        String effectiveStyle = parameter.getStyle() != null ? parameter.getStyle()
                : ParameterStyles.defaultFor(parameter.getIn());
        boolean defaultValue = ParameterStyles.defaultExplode(effectiveStyle);
        List<ExplodeChoice> choices = List.of(
                new ExplodeChoice(null, "Default (" + defaultValue + ")"),
                new ExplodeChoice(Boolean.TRUE, "true"),
                new ExplodeChoice(Boolean.FALSE, "false"));

        ComboBox<ExplodeChoice> box = new ComboBox<>();
        box.getStyleClass().add(EXPLODE_CLASS);
        box.getItems().addAll(choices);
        box.setValue(choices.stream().filter(c -> java.util.Objects.equals(c.explode(), parameter.getExplode()))
                .findFirst().orElse(choices.get(0)));
        box.valueProperty().addListener((obs, was, now) -> {
            if (now != null) {
                parameter.setExplode(now.explode());
            }
        });
        return box;
    }
}
