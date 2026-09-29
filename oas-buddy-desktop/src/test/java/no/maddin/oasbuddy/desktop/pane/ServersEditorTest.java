package no.maddin.oasbuddy.desktop.pane;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.HttpMethod;
import no.maddin.oasbuddy.core.model.Operation;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServersEditorTest extends ApplicationTest {

    private final AtomicBoolean confirmAnswer = new AtomicBoolean(true);
    private final AtomicBoolean asked = new AtomicBoolean();
    private OasDocument document;
    private Operation operation;
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        document = OasDocument.newDocument(DocumentFormat.YAML);
        operation = document.getPaths().addPath("/pets").addOperation(HttpMethod.GET);
        show(ServersPane.build(document));
    }

    private void show(Node pane) {
        interact(() -> {
            stage.setScene(new Scene(new StackPane(pane), 1100, 800));
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
    void showingTheServersPaneAddsNothingToTheDocument() {
        assertFalse(document.getRoot().has("servers"));
    }

    @Test
    void aTemplatedServerCanDeclareItsVariables() {
        interact(() -> button("Add server").fire());
        interact(() -> ((TextField) byClass(ServersEditor.URL_CLASS)).setText("https://{region}.example.com"));

        assertTrue(((Label) byClass(ServersEditor.UNDEFINED_NOTE_CLASS)).getText().contains("{region}"));

        interact(() -> button("Declare them").fire());
        interact(() -> ((TextField) byClass(ServersEditor.VARIABLE_ENUM_CLASS)).setText("eu, us"));
        interact(() -> ((TextField) byClass(ServersEditor.VARIABLE_DEFAULT_CLASS)).setText("eu"));

        var variable = document.getServers().all().get(0).getVariables().get("region");
        assertEquals(List.of("eu", "us"), variable.getEnum());
        assertEquals("eu", variable.getDefault());
        assertTrue(document.getServers().all().get(0).undefinedVariables().isEmpty());
    }

    @Test
    void removingTheLastDocumentServerRemovesTheKey() {
        interact(() -> button("Add server").fire());
        interact(() -> button("Remove").fire());

        assertFalse(document.getRoot().has("servers"));
    }

    @Test
    void anOperationCanOverrideTheDocumentServersAndAnEmptyOverrideIsKept() {
        show(OperationPane.build(operation, List::of, TagCatalog.of(document), SecuritySchemeCatalog.of(document),
                ComponentCatalog.of(document), (q, d) -> true, () -> { }));

        interact(() -> ((CheckBox) byClass("servers-override")).setSelected(true));
        assertTrue(operation.getServers().isDeclared());
        assertTrue(operation.getServers().all().isEmpty());
        assertTrue(document.getRoot().get("paths").get("/pets").get("get").get("servers").isArray());

        interact(() -> ((CheckBox) byClass("operation-deprecated")).setSelected(true));
        assertTrue(operation.isDeprecated());
    }

    @Test
    void stoppingAnOverrideWithServersAsksAndDecliningKeepsThem() {
        operation.getServers().add("https://override.example.com");
        show(OperationPane.build(operation, List::of, TagCatalog.of(document), SecuritySchemeCatalog.of(document),
                ComponentCatalog.of(document), (q, d) -> {
                    asked.set(true);
                    return confirmAnswer.get();
                }, () -> { }));

        confirmAnswer.set(false);
        CheckBox box = (CheckBox) byClass("servers-override");
        interact(() -> box.setSelected(false));
        assertTrue(asked.get());
        assertTrue(box.isSelected(), "declining puts the box back");
        assertEquals(1, operation.getServers().all().size());

        confirmAnswer.set(true);
        interact(() -> box.setSelected(false));
        assertFalse(operation.getServers().isDeclared());
    }

    @Test
    void aPathCanOverrideTheDocumentServers() {
        show(PathItemPane.build(document, "/pets", () -> { }, (a, b) -> true, (q, d) -> true, () -> { }, m -> { }));

        interact(() -> ((CheckBox) byClass("servers-override")).setSelected(true));

        assertTrue(document.getPaths().getPathItem("/pets").getServers().isDeclared());
    }
}
