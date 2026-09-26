package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.ApiResponse;
import no.maddin.oasbuddy.core.model.HttpMethod;
import no.maddin.oasbuddy.core.model.Responses;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
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
 * An operation's responses switching between inline and a reference to a component response. The
 * switch is explicit, and replacing an inline definition asks first — but only when there is
 * something to lose.
 */
class OperationPaneResponsesTest extends ApplicationTest {

    private OasDocument document;
    private StackPane holder;
    private final List<String> asked = new ArrayList<>();
    private boolean answer = true;

    @Override
    public void start(Stage stage) {
        document = DocumentReader.read("""
                openapi: 3.0.3
                info:
                  title: Reusing
                  version: 1.0.0
                paths:
                  /pets:
                    get:
                      responses:
                        '200':
                          description: The pets
                        '404':
                          $ref: '#/components/responses/NotFound'
                        '500': {}
                        '503':
                          $ref: '#/components/responses/Gone'
                components:
                  responses:
                    NotFound:
                      description: Not here
                      content:
                        application/json:
                          schema:
                            type: string
                    Error:
                      description: Broken
                """, DocumentFormat.YAML);
        holder = new StackPane();
        stage.setScene(new Scene(holder, 1200, 800));
        stage.show();
        holder.getChildren().setAll(OperationPane.build(
                document.getPaths().getPathItem("/pets").getOperation(HttpMethod.GET), List::of,
                TagCatalog.of(document), SecuritySchemeCatalog.of(document), ComponentCatalog.of(document),
                (question, details) -> {
                    asked.add(question);
                    return answer;
                }, () -> { }));
    }

    @Test
    void aReferencedResponseShowsWhereItPointsAndNoInlineFields() {
        assertAll(
                () -> assertEquals("→ NotFound", String.valueOf(sourcePicker("404").getValue())),
                () -> assertTrue(textFields("404").isEmpty(), "a reference has no fields of its own to edit"),
                () -> assertEquals("Inline", String.valueOf(sourcePicker("200").getValue())),
                () -> assertEquals(List.of("The pets"),
                        textFields("200").stream().map(TextField::getText).toList()));
    }

    @Test
    void theSourcePickerOffersInlineAndEveryComponentResponse() {
        assertEquals(List.of("Inline", "→ NotFound", "→ Error"),
                sourcePicker("200").getItems().stream().map(String::valueOf).toList());
    }

    @Test
    void aReferenceToAnUndeclaredResponseIsStillShown() {
        assertEquals("→ Gone", String.valueOf(sourcePicker("503").getValue()));
    }

    @Test
    void referringAnEmptyInlineResponseReplacesItWithoutAsking() {
        choose("500", "→ Error");

        assertAll(
                () -> assertEquals(List.of(), asked),
                () -> assertEquals("Error", responses().getResponse("500").getReferencedResponseName()));
    }

    @Test
    void referringAPopulatedInlineResponseAsksFirst() {
        choose("200", "→ NotFound");

        assertAll(
                () -> assertEquals(List.of("Replace the inline 200 response with a reference to \"NotFound\"?"), asked),
                () -> assertEquals("NotFound", responses().getResponse("200").getReferencedResponseName()),
                () -> assertEquals(List.of("200", "404", "500", "503"), responses().statusCodes(),
                        "the status code keeps its place"));
    }

    @Test
    void decliningLeavesTheDocumentAndThePickerAsTheyWere() {
        answer = false;

        choose("200", "→ NotFound");

        ApiResponse response = responses().getResponse("200");
        assertAll(
                () -> assertFalse(response.isReference()),
                () -> assertEquals("The pets", response.getDescription()),
                () -> assertEquals("Inline", String.valueOf(sourcePicker("200").getValue())));
    }

    @Test
    void switchingAReferenceToInlineStartsFromACopyOfTheComponent() {
        choose("404", "Inline");

        ApiResponse inline = responses().getResponse("404");
        assertAll(
                () -> assertEquals(List.of(), asked, "nothing is lost, so nothing is asked"),
                () -> assertFalse(inline.isReference()),
                () -> assertEquals("Not here", inline.getDescription()),
                () -> assertEquals("string", inline.findSchema("application/json").getType()),
                () -> assertEquals(List.of("Not here"),
                        textFields("404").stream().map(TextField::getText).toList()));
    }

    @Test
    void showingTheOperationNeverAddsContentToAResponse() {
        assertTrue(responses().getResponse("500").isEmpty());
        assertNull(responses().getResponse("200").findSchema("application/json"));
    }

    private Responses responses() {
        return document.getPaths().getPathItem("/pets").getOperation(HttpMethod.GET).getResponses();
    }

    @SuppressWarnings("unchecked")
    private void choose(String statusCode, String label) {
        ComboBox<Object> picker = (ComboBox<Object>) sourcePicker(statusCode);
        Object choice = picker.getItems().stream()
                .filter(item -> label.equals(String.valueOf(item)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no choice " + label));
        interact(() -> picker.setValue(choice));
    }

    private ComboBox<?> sourcePicker(String statusCode) {
        return row(statusCode).getChildren().stream()
                .filter(node -> node.getStyleClass().contains("response-source"))
                .map(node -> (ComboBox<?>) node)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no source picker for " + statusCode));
    }

    private List<TextField> textFields(String statusCode) {
        return row(statusCode).getChildren().stream()
                .filter(TextField.class::isInstance)
                .map(TextField.class::cast)
                .toList();
    }

    private HBox row(String statusCode) {
        return lookup(".operation-response").queryAllAs(HBox.class).stream()
                .filter(row -> {
                    Node first = row.getChildren().getFirst();
                    return first instanceof Label label && statusCode.equals(label.getText());
                })
                .findFirst()
                .orElseThrow(() -> new AssertionError("no row for " + statusCode));
    }
}
