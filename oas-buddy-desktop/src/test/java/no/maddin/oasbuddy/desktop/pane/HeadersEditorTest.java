package no.maddin.oasbuddy.desktop.pane;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.ApiResponse;
import no.maddin.oasbuddy.core.model.HttpMethod;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeadersEditorTest extends ApplicationTest {

    private final AtomicBoolean confirmAnswer = new AtomicBoolean(true);
    private final AtomicBoolean asked = new AtomicBoolean();
    private OasDocument document;
    private ApiResponse ok;
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getComponents().getHeaders().addHeader("Rate").setDescription("Calls left");
        ok = document.getPaths().addPath("/pets").addOperation(HttpMethod.GET).getResponses().addResponse("200");
        ok.setDescription("ok");
        showOperation();
    }

    private void showOperation() {
        var operation = document.getPaths().getPathItem("/pets").getOperation(HttpMethod.GET);
        show(OperationPane.build(operation, List::of, TagCatalog.of(document), SecuritySchemeCatalog.of(document),
                ComponentCatalog.of(document), (q, d) -> {
                    asked.set(true);
                    return confirmAnswer.get();
                }, () -> { }));
    }

    private void show(Node pane) {
        interact(() -> {
            stage.setScene(new Scene(new StackPane(pane), 1200, 900));
            stage.show();
        });
    }

    private Node byClass(String styleClass) {
        return lookup("." + styleClass).query();
    }

    private Button button(String text) {
        return lookup((Node n) -> n instanceof Button b && text.equals(b.getText())).queryButton();
    }

    @Test
    void showingAResponseAddsNoHeadersKey() {
        assertFalse(document.getRoot().get("paths").get("/pets").get("get").get("responses").get("200").has("headers"));
    }

    @Test
    void aResponseCanDeclareAHeaderWithADescriptionASchemaAndRequired() {
        interact(() -> ((TextField) byClass(HeadersEditor.NEW_NAME_CLASS)).setText("X-Total"));
        interact(() -> button("Add header").fire());

        var row = lookup("." + HeadersEditor.ROW_CLASS).query();
        interact(() -> {
            var fields = row.lookupAll(".text-field").stream().map(TextField.class::cast).toList();
            fields.get(1).setText("Total items");   // description
            fields.get(2).setText("integer");       // type
            ((CheckBox) row.lookup(".check-box")).setSelected(true);
        });

        var header = ok.getHeaders().get("X-Total");
        assertEquals("Total items", header.getDescription());
        assertEquals("integer", header.getSchema().getType());
        assertEquals(Boolean.TRUE, header.isRequired());
    }

    @Test
    void aResponseCanReferToAReusableHeaderAndSwitchingToInlineCopiesIt() {
        @SuppressWarnings("unchecked")
        ComboBox<String> components = (ComboBox<String>) byClass(HeadersEditor.REFERENCE_CLASS);
        interact(() -> components.setValue("Rate"));
        interact(() -> button("Add reference").fire());

        assertEquals("Rate", ok.getHeaders().get("Rate").getReferencedHeaderName());

        @SuppressWarnings("unchecked")
        ComboBox<SourceChoice> picker = (ComboBox<SourceChoice>) byClass(HeadersEditor.SOURCE_CLASS);
        interact(() -> picker.setValue(SourceChoice.INLINE));

        assertFalse(ok.getHeaders().get("Rate").isReference());
        assertEquals("Calls left", ok.getHeaders().get("Rate").getDescription());
        assertFalse(asked.get(), "ref -> inline loses nothing, so it never asks");
    }

    @Test
    void replacingAnInlineHeaderWithAReferenceAsksAndDecliningKeepsIt() {
        ok.getHeaders().add("X-Rate").setDescription("mine");
        showOperation();
        confirmAnswer.set(false);

        @SuppressWarnings("unchecked")
        ComboBox<SourceChoice> picker = (ComboBox<SourceChoice>) byClass(HeadersEditor.SOURCE_CLASS);
        interact(() -> picker.setValue(new SourceChoice("Rate")));

        assertTrue(asked.get());
        assertEquals(SourceChoice.INLINE, picker.getValue(), "declining reverts the picker");
        assertEquals("mine", ok.getHeaders().get("X-Rate").getDescription());
    }

    @Test
    void renamingAHeaderKeepsItsPosition() {
        ok.getHeaders().add("A");
        ok.getHeaders().add("B");
        ok.getHeaders().add("C");
        showOperation();

        TextField second = lookup("." + HeadersEditor.NAME_CLASS).queryAllAs(TextField.class).stream()
                .filter(f -> "B".equals(f.getText())).findFirst().orElseThrow();
        interact(() -> {
            second.setText("Z");
            second.fireEvent(new javafx.event.ActionEvent());
        });

        assertEquals(List.of("A", "Z", "C"), ok.getHeaders().names());
    }

    @Test
    void aHeaderThatIsNotAnObjectIsShownAndCanBeRemovedButNotEdited() {
        ((com.fasterxml.jackson.databind.node.ObjectNode) document.getRoot().get("paths").get("/pets").get("get")
                .get("responses").get("200")).putObject("headers").put("X-Odd", "~");
        showOperation();

        assertTrue(lookup((Node n) -> n instanceof Label l && "(not an object)".equals(l.getText()))
                .tryQuery().isPresent());

        Node row = lookup("." + HeadersEditor.ROW_CLASS).query();
        interact(() -> ((Button) row.lookup(".button")).fire());
        assertFalse(document.getRoot().get("paths").get("/pets").get("get").get("responses").get("200")
                .has("headers"));
    }

    @Test
    void theComponentResponseEditorHasTheSameSection() {
        document.getComponents().getResponses().addResponse("Limited").setDescription("limited");
        show(ResponsePane.build(document, "Limited", List::of, (q, d) -> true, name -> { }));

        interact(() -> ((TextField) byClass(HeadersEditor.NEW_NAME_CLASS)).setText("Retry-After"));
        interact(() -> button("Add header").fire());

        assertEquals(List.of("Retry-After"),
                document.getComponents().getResponses().getResponse("Limited").getHeaders().names());
    }

    @Test
    void theComponentHeaderEditorEditsItsFields() {
        show(HeaderPane.build(document, "Rate", name -> { }));

        interact(() -> ((TextField) lookup("#header-type").query()).setText("integer"));

        assertEquals("integer", document.getComponents().getHeaders().getHeader("Rate").getSchema().getType());
    }
}
