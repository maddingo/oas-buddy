package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.HttpMethod;
import no.maddin.oasbuddy.core.model.Parameter;
import no.maddin.oasbuddy.core.model.Parameters;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Path-level parameters in the path editor, and the inline/reference switch they share with an
 * operation's own parameters.
 */
class ParametersEditorTest extends ApplicationTest {

    private OasDocument document;
    private final List<String> asked = new ArrayList<>();
    private boolean answer = true;

    @Override
    public void start(Stage stage) {
        document = DocumentReader.read("""
                openapi: 3.0.3
                info:
                  title: Parameters
                  version: 1.0.0
                paths:
                  /pets/{petId}:
                    parameters:
                      - name: petId
                        in: path
                        required: true
                      - $ref: '#/components/parameters/Limit'
                    get:
                      responses:
                        '200':
                          description: ok
                components:
                  parameters:
                    Limit:
                      name: limit
                      in: query
                      schema:
                        type: integer
                    Trace:
                      name: X-Trace
                      in: header
                """, DocumentFormat.YAML);
        stage.setScene(new Scene(new StackPane(PathItemPane.build(document, "/pets/{petId}", () -> { },
                (from, to) -> true, (question, details) -> {
                    asked.add(question);
                    return answer;
                }, () -> { }, method -> { })), 1200, 800));
        stage.show();
    }

    @Test
    void thePathEditorShowsItsParameters() {
        assertAll(
                () -> assertEquals(2, rows().size()),
                () -> assertEquals("petId", textFields(0).getFirst().getText()),
                () -> assertEquals("→ Limit", String.valueOf(sourcePicker(1).getValue())),
                () -> assertEquals("limit in query", resolved(1)));
    }

    @Test
    void showingTheParametersNeverAddsASchema() {
        assertNull(pathParameters().all().getFirst().findSchema());
    }

    @Test
    void replacingAnInlineParameterWithAReferenceAsksFirst() {
        choose(0, "→ Trace");

        assertAll(
                () -> assertEquals(List.of("Replace the inline parameter \"petId\" with a reference to \"Trace\"?"),
                        asked),
                () -> assertEquals("Trace", pathParameters().all().getFirst().getReferencedParameterName()),
                () -> assertEquals(2, pathParameters().all().size(), "replaced in place, not appended"));
    }

    @Test
    void decliningLeavesTheParameterAndThePickerAsTheyWere() {
        answer = false;

        choose(0, "→ Trace");

        assertAll(
                () -> assertFalse(pathParameters().all().getFirst().isReference()),
                () -> assertEquals("petId", pathParameters().all().getFirst().getName()),
                () -> assertEquals("Inline", String.valueOf(sourcePicker(0).getValue())));
    }

    @Test
    void switchingAReferenceToInlineCopiesTheComponentWithoutAsking() {
        choose(1, "Inline");

        Parameter inline = pathParameters().all().get(1);
        assertAll(
                () -> assertEquals(List.of(), asked),
                () -> assertEquals("limit", inline.getName()),
                () -> assertEquals("integer", inline.findSchema().getType()),
                () -> assertEquals("limit", textFields(1).getFirst().getText()));
    }

    @Test
    void addsAReferenceToAReusableParameter() {
        interact(() -> {
            ComboBox<String> components = lookup(".path-parameters .reference-parameter").query();
            components.setValue("Trace");
            lookup(".path-parameters .add-parameter-reference").queryButton().fire();
        });

        assertEquals("Trace", pathParameters().all().get(2).getReferencedParameterName());
    }

    @Test
    void removingTheLastParameterLeavesNoEmptyList() {
        interact(() -> removeButton(1).fire());
        interact(() -> removeButton(0).fire());

        assertFalse(document.getRoot().get("paths").get("/pets/{petId}").has("parameters"));
    }

    @Test
    void theOperationItselfIsUntouched() {
        choose(1, "Inline");

        assertEquals(List.of(), document.getPaths().getPathItem("/pets/{petId}").getOperation(HttpMethod.GET)
                .getParameters().all());
    }

    private Parameters pathParameters() {
        return document.getPaths().getPathItem("/pets/{petId}").getParameters();
    }

    private List<HBox> rows() {
        VBox box = lookup(".path-parameters").queryAs(VBox.class);
        return box.lookupAll(".parameter-row").stream().map(HBox.class::cast).toList();
    }

    private ComboBox<?> sourcePicker(int index) {
        return (ComboBox<?>) rows().get(index).getChildren().stream()
                .filter(node -> node.getStyleClass().contains("parameter-source"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no source picker in row " + index));
    }

    private List<TextField> textFields(int index) {
        return rows().get(index).getChildren().stream()
                .filter(TextField.class::isInstance)
                .map(TextField.class::cast)
                .toList();
    }

    private String resolved(int index) {
        return rows().get(index).getChildren().stream()
                .filter(node -> node.getStyleClass().contains("parameter-resolved"))
                .map(node -> ((Label) node).getText())
                .findFirst()
                .orElseThrow(() -> new AssertionError("no resolved label in row " + index));
    }

    private Button removeButton(int index) {
        return rows().get(index).getChildren().stream()
                .filter(node -> node instanceof Button button && "Remove".equals(button.getText()))
                .map(Button.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no remove button in row " + index));
    }

    @SuppressWarnings("unchecked")
    private void choose(int index, String label) {
        ComboBox<Object> picker = (ComboBox<Object>) sourcePicker(index);
        Object choice = picker.getItems().stream()
                .filter(item -> label.equals(String.valueOf(item)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no choice " + label));
        interact(() -> picker.setValue(choice));
    }
}
