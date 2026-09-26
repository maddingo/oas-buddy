package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.ApiResponse;
import no.maddin.oasbuddy.core.model.Content;
import no.maddin.oasbuddy.core.model.HttpMethod;
import no.maddin.oasbuddy.core.model.Operation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
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
 * Media types beyond application/json on a request body and a response, each with its own schema
 * and examples, driven through the operation editor that hosts them.
 */
class ContentEditorTest extends ApplicationTest {

    private OasDocument document;
    private final List<String> asked = new ArrayList<>();

    @Override
    public void start(Stage stage) {
        document = DocumentReader.read("""
                openapi: 3.0.3
                info:
                  title: Content
                  version: 1.0.0
                paths:
                  /pets:
                    post:
                      requestBody:
                        content:
                          application/json:
                            schema:
                              $ref: '#/components/schemas/Pet'
                          application/xml:
                            schema:
                              $ref: '#/components/schemas/Pet'
                      responses:
                        '200':
                          description: ok
                          content:
                            text/plain:
                              schema:
                                type: integer
                              examples:
                                one:
                                  summary: One
                                  value: 1
                                blank: {}
                        '204':
                          description: No content
                components:
                  schemas:
                    Pet:
                      type: object
                  examples:
                    Two:
                      summary: Two
                      value: 2
                """, DocumentFormat.YAML);
        Node pane = OperationPane.build(operation(), () -> document.getComponents().getSchemas().names(),
                TagCatalog.of(document), SecuritySchemeCatalog.of(document), ComponentCatalog.of(document),
                (question, details) -> {
                    asked.add(question);
                    return true;
                }, () -> { });
        stage.setScene(new Scene(new ScrollPane(pane), 1400, 900));
        stage.show();
    }

    @Test
    void theRequestBodyShowsEveryMediaTypeWithItsOwnSchema() {
        assertAll(
                () -> assertEquals(List.of("application/json", "application/xml"), mediaTypeNames(requestBody())),
                () -> assertEquals("→ Pet", String.valueOf(schemaPicker(requestBody(), "application/xml").getValue())));
    }

    @Test
    void aResponseMediaTypeCanHaveAPlainType() {
        assertEquals("integer", String.valueOf(schemaPicker(responseContent(), "text/plain").getValue()));
    }

    @Test
    void addsAMediaTypeAtTheEnd() {
        interact(() -> {
            ComboBox<?> newType = (ComboBox<?>) requestBody().lookup(".new-media-type");
            newType.getEditor().setText("application/x-www-form-urlencoded");
            ((Button) requestBody().lookup(".add-media-type")).fire();
        });

        assertEquals(List.of("application/json", "application/xml", "application/x-www-form-urlencoded"),
                body().mediaTypes());
    }

    @Test
    void removesOneMediaTypeAndLeavesTheOthers() {
        interact(() -> removeButton(section(requestBody(), "application/json")).fire());

        assertEquals(List.of("application/xml"), body().mediaTypes());
    }

    @Test
    void removingTheLastMediaTypeOfAResponseLeavesNoContentKey() {
        interact(() -> removeButton(section(responseContent(), "text/plain")).fire());

        assertFalse(document.getRoot().get("paths").get("/pets").get("post").get("responses").get("200")
                .has("content"));
    }

    @Test
    void renamingAMediaTypeKeepsItsPlaceAndSchema() {
        interact(() -> {
            TextField name = nameField(section(requestBody(), "application/json"));
            name.setText("application/vnd.pet+json");
            name.getOnAction().handle(null);
        });

        assertAll(
                () -> assertEquals(List.of("application/vnd.pet+json", "application/xml"), body().mediaTypes()),
                () -> assertEquals("Pet", body().get("application/vnd.pet+json").findSchema().getReferencedSchemaName()));
    }

    @Test
    void choosingAPlainTypeReplacesAReference() {
        interact(() -> choose(schemaPicker(requestBody(), "application/xml"), "string"));

        assertAll(
                () -> assertEquals("string", body().get("application/xml").findSchema().getType()),
                () -> assertNull(body().get("application/xml").findSchema().getRef()));
    }

    @Test
    void theSingleExampleIsTypedAsTheSchemasType() {
        interact(() -> ((TextField) section(responseContent(), "text/plain").lookup(".media-type-example"))
                .setText("42"));

        assertTrue(okContent().get("text/plain").getExample().isInt());
    }

    @Test
    void namedExamplesAreShownWithTheirSource() {
        assertEquals(List.of("Inline", "Inline"), exampleRows().stream()
                .map(row -> String.valueOf(((ComboBox<?>) row.lookup(".example-source")).getValue())).toList());
    }

    @Test
    void anExampleValueIsTypedAsTheSchemasType() {
        interact(() -> ((TextArea) exampleRow("one").lookup(".example-value")).setText("7"));

        assertEquals(7, okContent().get("text/plain").getExamples().get("one").getValue().asInt());
        assertTrue(okContent().get("text/plain").getExamples().get("one").getValue().isInt());
    }

    @Test
    void referringAPopulatedExampleAsksFirst() {
        interact(() -> choose((ComboBox<?>) exampleRow("one").lookup(".example-source"), "→ Two"));

        assertAll(
                () -> assertEquals(List.of("Replace the inline example \"one\" with a reference to \"Two\"?"), asked),
                () -> assertEquals("Two", okContent().get("text/plain").getExamples().get("one")
                        .getReferencedExampleName()),
                () -> assertEquals(List.of("one", "blank"), okContent().get("text/plain").getExamples().names()));
    }

    @Test
    void referringAnEmptyExampleDoesNotAsk() {
        interact(() -> choose((ComboBox<?>) exampleRow("blank").lookup(".example-source"), "→ Two"));

        assertEquals(List.of(), asked);
    }

    @Test
    void addsAReferenceToAReusableExampleNamedAfterIt() {
        interact(() -> {
            Node section = section(responseContent(), "text/plain");
            ((ComboBox<String>) section.lookup(".reference-example")).setValue("Two");
            ((Button) section.lookup(".add-example-reference")).fire();
        });

        assertEquals("Two", okContent().get("text/plain").getExamples().get("Two").getReferencedExampleName());
    }

    @Test
    void showingAResponseWithoutContentAddsNone() {
        ApiResponse noContent = operation().getResponses().getResponse("204");

        assertEquals(List.of(), noContent.getContent().mediaTypes());
    }

    private Operation operation() {
        return document.getPaths().getPathItem("/pets").getOperation(HttpMethod.POST);
    }

    private Content body() {
        return operation().getRequestBody().getContent();
    }

    private Content okContent() {
        return operation().getResponses().getResponse("200").getContent();
    }

    private VBox requestBody() {
        return lookup(".request-body-content").queryAs(VBox.class);
    }

    /** The first response's content editor: the one under status 200. */
    private VBox responseContent() {
        return lookup(".content-editor").queryAllAs(VBox.class).stream()
                .filter(box -> !box.getStyleClass().contains("request-body-content"))
                .findFirst()
                .orElseThrow();
    }

    private static List<String> mediaTypeNames(VBox content) {
        return content.lookupAll(".media-type-name").stream().map(node -> ((TextField) node).getText()).toList();
    }

    private static Node section(VBox content, String mediaType) {
        return content.lookupAll(".media-type").stream()
                .filter(section -> mediaType.equals(nameField(section).getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no section for " + mediaType));
    }

    private static TextField nameField(Node section) {
        return (TextField) section.lookup(".media-type-name");
    }

    private static ComboBox<?> schemaPicker(VBox content, String mediaType) {
        return (ComboBox<?>) section(content, mediaType).lookup(".media-type-schema");
    }

    private static Button removeButton(Node section) {
        return (Button) ((HBox) nameField(section).getParent()).getChildren().getLast();
    }

    private List<HBox> exampleRows() {
        return section(responseContent(), "text/plain").lookupAll(".example-row").stream()
                .map(HBox.class::cast).toList();
    }

    private HBox exampleRow(String name) {
        return exampleRows().stream()
                .filter(row -> name.equals(((javafx.scene.control.Label) row.getChildren().getFirst()).getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no example row " + name));
    }

    @SuppressWarnings("unchecked")
    private static void choose(ComboBox<?> picker, String label) {
        ComboBox<Object> box = (ComboBox<Object>) picker;
        box.setValue(box.getItems().stream()
                .filter(item -> label.equals(String.valueOf(item)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no choice " + label)));
    }
}
