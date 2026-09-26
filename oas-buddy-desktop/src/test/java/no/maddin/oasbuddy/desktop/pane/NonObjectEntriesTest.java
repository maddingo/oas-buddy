package no.maddin.oasbuddy.desktop.pane;

import com.fasterxml.jackson.databind.JsonNode;
import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.DocumentReader;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.HttpMethod;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A hand-edited file can put something other than an object under a component name or a media
 * type ({@code NotFound: ~}). The outline lists every key, so each editor must show such an entry
 * without crashing, and without rewriting it: it is left exactly as loaded.
 */
class NonObjectEntriesTest extends ApplicationTest {

    private OasDocument document;
    private JsonNode asLoaded;
    private StackPane holder;

    @Override
    public void start(Stage stage) {
        document = DocumentReader.read("""
                openapi: 3.0.3
                info:
                  title: Odd
                  version: 1.0.0
                paths:
                  /odd: ~
                  /pets:
                    get:
                      responses:
                        '200':
                          description: ok
                          content:
                            application/json: ~
                        '500': ~
                components:
                  schemas:
                    Odd: ~
                  responses:
                    Odd: just text
                  parameters:
                    Odd: [1, 2]
                  examples:
                    Odd: 42
                  securitySchemes:
                    Odd: ~
                """, DocumentFormat.YAML);
        asLoaded = document.getRoot().deepCopy();
        holder = new StackPane();
        stage.setScene(new Scene(new ScrollPane(holder), 1200, 800));
        stage.show();
    }

    @Test
    void theSchemaEditorExplainsInsteadOfCrashing() {
        assertExplains(() -> SchemaPane.build(document, "Odd", (from, to) -> false, name -> { }));
    }

    @Test
    void theResponseEditorExplainsInsteadOfCrashing() {
        assertExplains(() -> ResponsePane.build(document, "Odd", List::of, (question, details) -> true, name -> { }));
    }

    @Test
    void theParameterEditorExplainsInsteadOfCrashing() {
        assertExplains(() -> ParameterPane.build(document, "Odd", name -> { }));
    }

    @Test
    void theExampleEditorExplainsInsteadOfCrashing() {
        assertExplains(() -> ExamplePane.build(document, "Odd", name -> { }));
    }

    @Test
    void theSecuritySchemeEditorExplainsInsteadOfCrashing() {
        assertExplains(() -> SecuritySchemePane.build(document, "Odd", (question, details) -> true, name -> { }));
    }

    @Test
    void thePathEditorExplainsInsteadOfCrashing() {
        assertExplains(() -> PathItemPane.build(document, "/odd", () -> { }, (from, to) -> false,
                (question, details) -> true, () -> { }, method -> { }), "\"/odd\" is not a");
    }

    @Test
    void theListPanesStillListTheEntry() {
        show(() -> new javafx.scene.layout.VBox(
                ResponsesPane.build(document, () -> { }, name -> { }),
                ParametersPane.build(document, () -> { }, name -> { }),
                ExamplesPane.build(document, () -> { }, name -> { }),
                SecuritySchemesPane.build(document, () -> { }, name -> { })));

        assertAll(
                () -> assertEquals(List.of("Odd"), GridPanes.entries(lookup("#responses-list").queryAs(GridPane.class))),
                () -> assertEquals(List.of("Odd"), GridPanes.entries(lookup("#parameters-list").queryAs(GridPane.class))),
                () -> assertEquals(List.of("Odd"), GridPanes.entries(lookup("#examples-list").queryAs(GridPane.class))),
                () -> assertEquals(List.of("Odd"),
                        GridPanes.entries(lookup("#security-schemes-list").queryAs(GridPane.class))),
                () -> assertEquals(asLoaded, document.getRoot()));
    }

    @Test
    void theOperationEditorShowsANonObjectResponseAndMediaTypeWithoutCrashing() {
        show(() -> OperationPane.build(
                document.getPaths().getPathItem("/pets").getOperation(HttpMethod.GET), List::of,
                TagCatalog.of(document), SecuritySchemeCatalog.of(document), ComponentCatalog.of(document),
                (question, details) -> true, () -> { }));

        assertAll(
                () -> assertTrue(lookup("Not a response object; left exactly as it was loaded.").tryQuery().isPresent()),
                () -> assertTrue(lookup("Not a media type object; left exactly as it was loaded.").tryQuery().isPresent()),
                () -> assertEquals(asLoaded, document.getRoot()));
    }

    private void assertExplains(Supplier<Node> pane) {
        assertExplains(pane, "\"Odd\" is not a");
    }

    private void assertExplains(Supplier<Node> pane, String expectedStart) {
        show(pane);

        Label note = lookup("#not-editable").queryAs(Label.class);
        assertAll(
                () -> assertTrue(note.getText().startsWith(expectedStart), note.getText()),
                () -> assertEquals(asLoaded, document.getRoot(), "showing the entry must not change the document"));
    }

    private void show(Supplier<Node> pane) {
        interact(() -> holder.getChildren().setAll(pane.get()));
    }
}
