package no.maddin.oasbuddy.desktop;

import no.maddin.oasbuddy.core.model.HttpMethod;
import javafx.scene.control.Button;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Removals end to end through the app's own window: the document changes, the outline is rebuilt
 * and the editor lands somewhere that still exists rather than on the deleted thing.
 */
class MainAppRemovalTest extends ApplicationTest {

    private MainApp app;

    @Override
    public void start(Stage stage) {
        app = new MainApp();
        app.start(stage);
        app.setConfirmation((question, details) -> true);
        app.getDocument().getPaths().addPath("/pets").addOperation(HttpMethod.GET).setOperationId("listPets");
        app.getDocument().getPaths().getPathItem("/pets").addOperation(HttpMethod.POST);
        app.getDocument().getComponents().getSchemas().addSchema("Pet").setType("object");
        interact(() -> {
            app.refreshOutline();
        });
    }

    @Test
    void removingAnOperationLeavesTheEditorOnItsPath() {
        selectOutline("Paths", "/pets", "POST");

        interact(() -> deleteButton("#delete-operation").fire());

        assertEquals(List.of(HttpMethod.GET),
                List.copyOf(app.getDocument().getPaths().getPathItem("/pets").getOperations().keySet()));
        assertTrue(lookup("#path-operations").tryQuery().isPresent(), "should land on the path editor");
    }

    @Test
    void removingAPathLeavesTheEditorOnThePathList() {
        selectOutline("Paths", "/pets");

        interact(() -> deleteButton("#delete-path").fire());

        assertFalse(app.getDocument().getPaths().pathNames().contains("/pets"));
        assertTrue(lookup("#paths-list").tryQuery().isPresent(), "should land on the path list");
    }

    @Test
    void removingASchemaLeavesTheEditorOnTheSchemaList() {
        selectOutline("Schemas", "Pet");

        interact(() -> deleteButton("#delete-schema").fire());

        assertFalse(app.getDocument().getComponents().getSchemas().names().contains("Pet"));
        assertTrue(lookup("#schemas-list").tryQuery().isPresent(), "should land on the schema list");
    }

    private Button deleteButton(String id) {
        return lookup(id).queryButton();
    }

    @SuppressWarnings("unchecked")
    private void selectOutline(String... labels) {
        TreeView<Object> outline = lookup(".tree-view").query();
        interact(() -> {
            TreeItem<Object> item = outline.getRoot();
            for (String label : labels) {
                item = item.getChildren().stream()
                        .filter(child -> label.equals(String.valueOf(child.getValue())))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("no outline node " + label));
            }
            outline.getSelectionModel().select(item);
        });
    }
}
