package no.maddin.oasbuddy.desktop.pane;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.HttpMethod;
import no.maddin.oasbuddy.core.model.Parameter;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParameterSerializationTest extends ApplicationTest {

    private OasDocument document;
    private Parameter tags;
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        document = DocumentReader.read("""
                openapi: 3.0.3
                info: {title: T, version: '1'}
                paths:
                  /pets:
                    get:
                      parameters:
                        - {name: tags, in: query}
                        - name: filter
                          in: query
                          content:
                            application/json:
                              schema: {type: object}
                      responses:
                        '200': {description: ok}
                """, DocumentFormat.YAML);
        tags = param(0);
        showEditor();
    }

    private Parameter param(int index) {
        return document.getPaths().getPathItem("/pets").getOperation(HttpMethod.GET).getParameters().all().get(index);
    }

    private void showEditor() {
        var parameters = document.getPaths().getPathItem("/pets").getOperation(HttpMethod.GET).getParameters();
        Node editor = ParametersEditor.build(parameters, ComponentCatalog.of(document), (q, d) -> true, "test");
        interact(() -> {
            stage.setScene(new Scene(new StackPane(editor), 1200, 900));
            stage.show();
        });
    }

    @SuppressWarnings("unchecked")
    private <T> ComboBox<T> combo(String styleClass) {
        return (ComboBox<T>) lookup("." + styleClass).queryAll().iterator().next();
    }

    private void expandAll() {
        interact(() -> lookup(".titled-pane").queryAllAs(TitledPane.class).forEach(p -> p.setExpanded(true)));
    }

    @Test
    void onlyTheStylesValidForQueryAreOffered() {
        expandAll();
        ComboBox<ParameterSerialization.StyleChoice> style = combo(ParameterSerialization.STYLE_CLASS);

        assertEquals(List.of("Default (form)", "form", "spaceDelimited", "pipeDelimited", "deepObject"),
                style.getItems().stream().map(Object::toString).toList());
    }

    @Test
    void explodeShowsTheSpecDefaultForTheEffectiveStyleAndFollowsAStyleChange() {
        expandAll();
        ComboBox<ParameterSerialization.ExplodeChoice> explode = combo(ParameterSerialization.EXPLODE_CLASS);
        assertEquals("Default (true)", explode.getItems().get(0).toString());

        ComboBox<ParameterSerialization.StyleChoice> style = combo(ParameterSerialization.STYLE_CLASS);
        interact(() -> style.setValue(style.getItems().stream()
                .filter(c -> "pipeDelimited".equals(c.style())).findFirst().orElseThrow()));

        assertEquals("pipeDelimited", tags.getStyle());
        ComboBox<ParameterSerialization.ExplodeChoice> rebuilt = combo(ParameterSerialization.EXPLODE_CLASS);
        assertEquals("Default (false)", rebuilt.getItems().get(0).toString());
        assertNull(tags.getExplode(), "showing the default writes nothing");
    }

    @Test
    void anExplicitExplodeIsWrittenAndTheDefaultChoiceRemovesIt() {
        expandAll();
        ComboBox<ParameterSerialization.ExplodeChoice> explode = combo(ParameterSerialization.EXPLODE_CLASS);

        interact(() -> explode.setValue(explode.getItems().get(2)));
        assertEquals(Boolean.FALSE, tags.getExplode());

        interact(() -> explode.setValue(explode.getItems().get(0)));
        assertNull(tags.getExplode());
    }

    @Test
    void changingInRevalidatesTheStyleAndHidesTheQueryOnlyFlags() {
        tags.setStyle("deepObject");
        showEditor();
        expandAll();
        // both parameters are query ones, so each shows the box
        assertEquals(2, lookup("." + ParameterSerialization.ALLOW_RESERVED_CLASS).queryAll().size());

        @SuppressWarnings("unchecked")
        ComboBox<String> in = (ComboBox<String>) lookup(".combo-box").queryAll().stream()
                .map(n -> (ComboBox<?>) n)
                .filter(c -> c.getItems().contains("cookie")).findFirst().orElseThrow();
        interact(() -> in.setValue("path"));

        assertNull(tags.getStyle(), "deepObject is not a path style");
        assertEquals(1, lookup("." + ParameterSerialization.ALLOW_RESERVED_CLASS).queryAll().size(),
                "only the parameter that is still a query one keeps the box");
        ComboBox<ParameterSerialization.StyleChoice> style = combo(ParameterSerialization.STYLE_CLASS);
        assertEquals(List.of("Default (simple)", "matrix", "label", "simple"),
                style.getItems().stream().map(Object::toString).toList());
    }

    @Test
    void flagsAndTheExampleAreEditable() {
        expandAll();
        interact(() -> ((CheckBox) lookup("." + ParameterSerialization.ALLOW_RESERVED_CLASS).query()).setSelected(true));
        interact(() -> ((CheckBox) lookup("." + ParameterSerialization.DEPRECATED_CLASS).query()).setSelected(true));
        interact(() -> ((TextField) lookup("." + ParameterSerialization.EXAMPLE_CLASS).query()).setText("a|b"));

        assertTrue(tags.isAllowReserved());
        assertTrue(tags.isDeprecated());
        assertEquals("a|b", tags.getExample().asText());
    }

    @Test
    void anInvalidLoadedStyleIsShownMarkedNotDropped() {
        tags.setStyle("matrix");
        showEditor();

        ComboBox<ParameterSerialization.StyleChoice> style = combo(ParameterSerialization.STYLE_CLASS);

        assertEquals("matrix (not valid for query)", style.getValue().toString());
        assertEquals("matrix", tags.getStyle());
    }

    @Test
    void aParameterUsingContentIsMarkedNotEditableAndItsFileIsUntouched() {
        var before = document.getRoot().deepCopy();
        expandAll();

        List<Label> notes = lookup("." + ParameterSerialization.CONTENT_NOTE_CLASS).queryAllAs(Label.class)
                .stream().toList();
        assertEquals(1, notes.size());
        assertTrue(notes.get(0).getText().contains("application/json"));
        assertTrue(param(1).hasContent());
        assertFalse(param(1).getContent().mediaTypes().isEmpty());
        assertEquals(before, document.getRoot(), "showing the editor changes nothing");
    }

    @Test
    void showingTheEditorAddsNothingToTheDocument() {
        var before = document.getRoot().deepCopy();
        showEditor();
        expandAll();

        assertEquals(before, document.getRoot());
    }
}
