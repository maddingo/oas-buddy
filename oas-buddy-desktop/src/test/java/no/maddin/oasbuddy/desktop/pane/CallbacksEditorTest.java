package no.maddin.oasbuddy.desktop.pane;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.HttpMethod;
import no.maddin.oasbuddy.core.model.Operation;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CallbacksEditorTest extends ApplicationTest {

    private static final String URL = "{$request.body#/callbackUrl}";

    private final AtomicBoolean confirmAnswer = new AtomicBoolean(true);
    private final List<String> asked = new ArrayList<>();
    private OasDocument document;
    private Operation subscribe;
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getComponents().getCallbacks().addCallback("Shared").add(URL).addOperation(HttpMethod.POST);
        subscribe = document.getPaths().addPath("/subscribe").addOperation(HttpMethod.POST);
        subscribe.getResponses().addResponse("200").setDescription("ok");
        showOperation();
    }

    private void showOperation() {
        show(OperationPane.build(subscribe, List::of, TagCatalog.of(document), SecuritySchemeCatalog.of(document),
                ComponentCatalog.of(document), confirmation(), () -> { }));
    }

    private RemovalConfirmation confirmation() {
        return (q, d) -> {
            asked.add(q);
            return confirmAnswer.get();
        };
    }

    private void show(Node pane) {
        interact(() -> {
            stage.setScene(new Scene(new StackPane(pane), 1200, 1000));
            stage.show();
        });
    }

    private Node byClass(String styleClass) {
        return lookup("." + styleClass).query();
    }

    private Button button(String text) {
        return lookup((Node n) -> n instanceof Button b && text.equals(b.getText())).queryButton();
    }

    private void addCallback(String name) {
        interact(() -> ((TextField) byClass(CallbacksEditor.NEW_NAME_CLASS)).setText(name));
        interact(() -> ((Button) byClass(CallbacksEditor.ADD_CLASS)).fire());
    }

    private void addExpression(String expression) {
        interact(() -> ((TextField) byClass(CallbackExpressions.NEW_NAME_CLASS)).setText(expression));
        interact(() -> ((Button) byClass(CallbackExpressions.ADD_CLASS)).fire());
    }

    @Test
    void showingAnOperationAddsNoCallbacksKey() {
        assertFalse(document.getRoot().get("paths").get("/subscribe").get("post").has("callbacks"));
    }

    @Test
    void aCallbackHoldsSeveralExpressionsEachWithAFullPathItem() {
        addCallback("onEvent");
        addExpression(URL);
        addExpression("{$request.body#/otherUrl}");

        var callback = subscribe.getCallbacks().get("onEvent");
        assertEquals(List.of(URL, "{$request.body#/otherUrl}"), callback.expressions());

        interact(() -> ((Button) byClass(PathItemPane.ADD_OPERATION_CLASS)).fire());
        assertEquals(1, callback.get(URL).getOperations().size()
                + callback.get("{$request.body#/otherUrl}").getOperations().size());
    }

    @Test
    void theNestedPathItemEditsItsFieldsAndOperationsWithTheSameEditors() {
        subscribe.getCallbacks().add("onEvent").add(URL);
        showOperation();

        // summary is the first text field of the embedded path item's grid
        TextField summary = lookup(".callback-expression .text-field").queryAllAs(TextField.class).stream()
                .filter(f -> f.getParent() instanceof javafx.scene.layout.GridPane).findFirst().orElseThrow();
        interact(() -> summary.setText("Event delivery"));
        assertEquals("Event delivery", subscribe.getCallbacks().get("onEvent").get(URL).getSummary());

        interact(() -> button("+ POST").fire());
        var operations = subscribe.getCallbacks().get("onEvent").get(URL).getOperations();
        assertTrue(operations.containsKey(HttpMethod.POST));

        // the operation inside is the full operation editor: it can take its own operation id and responses
        TitledPane nested = (TitledPane) byClass(PathItemPane.OPERATION_CLASS);
        interact(() -> nested.setExpanded(true));
        TextField operationId = nested.getContent().lookupAll(".text-field").stream()
                .map(TextField.class::cast).filter(f -> f.getParent() instanceof javafx.scene.layout.GridPane)
                .findFirst().orElseThrow();
        interact(() -> operationId.setText("onEvent"));
        assertEquals("onEvent", operations.get(HttpMethod.POST).getOperationId());
    }

    @Test
    void removingAnExpressionWithOperationsAsksAndDecliningKeepsIt() {
        subscribe.getCallbacks().add("onEvent").add(URL).addOperation(HttpMethod.POST);
        showOperation();
        confirmAnswer.set(false);

        interact(() -> ((Button) byClass(CallbackExpressions.REMOVE_CLASS)).fire());

        assertEquals(List.of("Remove expression \"" + URL + "\"?"), asked);
        assertEquals(List.of(URL), subscribe.getCallbacks().get("onEvent").expressions());

        confirmAnswer.set(true);
        interact(() -> ((Button) byClass(CallbackExpressions.REMOVE_CLASS)).fire());
        assertTrue(subscribe.getCallbacks().get("onEvent").expressions().isEmpty());
    }

    @Test
    void removingAnEmptyExpressionDoesNotAsk() {
        subscribe.getCallbacks().add("onEvent").add(URL);
        showOperation();

        interact(() -> ((Button) byClass(CallbackExpressions.REMOVE_CLASS)).fire());

        assertTrue(asked.isEmpty());
        assertTrue(subscribe.getCallbacks().get("onEvent").expressions().isEmpty());
    }

    @Test
    void anExpressionRenameKeepsItsPosition() {
        var callback = subscribe.getCallbacks().add("onEvent");
        callback.add("a");
        callback.add("b");
        callback.add("c");
        showOperation();

        TextField second = lookup("." + CallbackExpressions.NAME_CLASS).queryAllAs(TextField.class).stream()
                .filter(f -> "b".equals(f.getText())).findFirst().orElseThrow();
        interact(() -> {
            second.setText("z");
            second.fireEvent(new javafx.event.ActionEvent());
        });

        assertEquals(List.of("a", "z", "c"), callback.expressions());
    }

    @Test
    void aCallbackCanReferToAReusableOneAndSwitchingToInlineCopiesIt() {
        @SuppressWarnings("unchecked")
        ComboBox<String> components = (ComboBox<String>) byClass(CallbacksEditor.REFERENCE_CLASS);
        interact(() -> components.setValue("Shared"));
        interact(() -> button("Add reference").fire());

        assertEquals("Shared", subscribe.getCallbacks().get("Shared").getReferencedCallbackName());

        @SuppressWarnings("unchecked")
        ComboBox<SourceChoice> picker = (ComboBox<SourceChoice>) byClass(CallbacksEditor.SOURCE_CLASS);
        interact(() -> picker.setValue(SourceChoice.INLINE));

        assertFalse(subscribe.getCallbacks().get("Shared").isReference());
        assertEquals(List.of(URL), subscribe.getCallbacks().get("Shared").expressions());
        assertTrue(asked.isEmpty(), "ref -> inline loses nothing, so it never asks");
    }

    @Test
    void replacingAnInlineCallbackWithAReferenceAsksAndDecliningKeepsIt() {
        subscribe.getCallbacks().add("mine").add(URL);
        showOperation();
        confirmAnswer.set(false);

        @SuppressWarnings("unchecked")
        ComboBox<SourceChoice> picker = (ComboBox<SourceChoice>) byClass(CallbacksEditor.SOURCE_CLASS);
        interact(() -> picker.setValue(new SourceChoice("Shared")));

        assertEquals(1, asked.size());
        assertEquals(SourceChoice.INLINE, picker.getValue(), "declining reverts the picker");
        assertEquals(List.of(URL), subscribe.getCallbacks().get("mine").expressions());
    }

    @Test
    void aCallbackAndAnExpressionThatAreNotObjectsAreShownAndLeftAlone() throws Exception {
        document = no.maddin.oasbuddy.core.document.DocumentReader.read("""
                openapi: 3.0.3
                paths:
                  /subscribe:
                    post:
                      responses: {}
                      callbacks:
                        odd: ~
                        oddExpr:
                          'x': ~
                """, DocumentFormat.YAML);
        subscribe = document.getPaths().getPathItem("/subscribe").getOperation(HttpMethod.POST);
        var before = document.getRoot().deepCopy();
        showOperation();

        assertEquals(2, lookup((Node n) -> n instanceof Label l && "(not an object)".equals(l.getText()))
                .queryAll().size());
        assertEquals(before, document.getRoot(), "showing them must not change the document");
    }

    @Test
    void theComponentCallbackEditorHasTheSameExpressions() {
        show(CallbackPane.build(document, "Shared", List::of, confirmation(), name -> { }));

        addExpression("{$request.body#/second}");

        assertEquals(List.of(URL, "{$request.body#/second}"),
                document.getComponents().getCallbacks().getCallback("Shared").expressions());
    }

    @Test
    void aCallbackNestedInsideACallbackIsEditable() {
        subscribe.getCallbacks().add("outer").add(URL).addOperation(HttpMethod.POST);
        showOperation();

        TitledPane nested = (TitledPane) byClass(PathItemPane.OPERATION_CLASS);
        interact(() -> nested.setExpanded(true));
        TextField newName = nested.getContent().lookupAll("." + CallbacksEditor.NEW_NAME_CLASS).stream()
                .map(TextField.class::cast).findFirst().orElseThrow();
        Button add = (Button) nested.getContent().lookup("." + CallbacksEditor.ADD_CLASS);
        interact(() -> newName.setText("inner"));
        interact(add::fire);

        assertEquals(List.of("inner"), subscribe.getCallbacks().get("outer").get(URL)
                .getOperation(HttpMethod.POST).getCallbacks().names());
    }
}
