package no.maddin.oasbuddy.desktop;

import no.maddin.oasbuddy.core.model.HttpMethod;
import javafx.event.ActionEvent;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Renames end to end through the app's own window: the document changes, the outline picks up the
 * new name and the editor stays on the thing that was just renamed rather than losing the
 * selection.
 */
class MainAppRenameTest extends ApplicationTest {

    private MainApp app;
    private final List<String> askedToRenameSchema = new ArrayList<>();

    @Override
    public void start(Stage stage) {
        app = new MainApp();
        app.start(stage);
        app.setConfirmation((question, details) -> true);
        app.setRenameConfirmation((question, details) -> {
            askedToRenameSchema.add(question + " | " + details);
            return true;
        });
        app.getDocument().getPaths().addPath("/pets").addOperation(HttpMethod.GET).setOperationId("listPets");
        app.getDocument().getComponents().getSchemas().addSchema("Pet").setType("object");
        app.getDocument().getComponents().getSchemas().addSchema("Owner").setType("object");
        interact(() -> app.refreshOutline());
    }

    @Test
    void renamingASchemaWithNoReferencesUpdatesTheOutlineAndKeepsTheSelectionWithoutAsking() {
        selectOutline("Schemas", "Pet");

        commit(nameField("#schema-name"), "Animal");

        assertTrue(askedToRenameSchema.isEmpty(), "nothing references Pet, so there is nothing to ask about");
        assertEquals(List.of("Animal", "Owner"), app.getDocument().getComponents().getSchemas().names());
        assertEquals(List.of("Schemas", "Animal"), selectedPath());
    }

    @Test
    void renamingASchemaWithAReferenceAsksAndRewritesTheRef() {
        app.getDocument().getPaths().getPathItem("/pets").getOperation(HttpMethod.GET)
                .getResponses().addResponse("200").getSchema("application/json")
                .setRef("#/components/schemas/Pet");
        interact(() -> app.refreshOutline());
        selectOutline("Schemas", "Pet");

        commit(nameField("#schema-name"), "Animal");

        assertEquals(List.of("Rename schema \"Pet\" to \"Animal\"? | 1 reference will be updated:\n"
                        + "  paths → /pets → get → responses → 200 → content → application/json → schema"),
                askedToRenameSchema);
        assertEquals("#/components/schemas/Animal",
                app.getDocument().getPaths().getPathItem("/pets").getOperation(HttpMethod.GET)
                        .getResponses().getResponse("200").getSchema("application/json").getRef());
    }

    @Test
    void renamingAPathUpdatesTheOutlineAndKeepsTheSelection() {
        selectOutline("Paths", "/pets");

        commit(nameField("#path-name"), "/animals");

        assertEquals(List.of("/animals"), app.getDocument().getPaths().pathNames());
        assertEquals(List.of("Paths", "/animals"), selectedPath());
    }

    @Test
    void collidingSchemaNameLeavesTheOutlineUnchanged() {
        selectOutline("Schemas", "Pet");

        commit(nameField("#schema-name"), "Owner");

        assertTrue(askedToRenameSchema.isEmpty(), "a collision is refused before any confirmation");
        assertEquals(List.of("Pet", "Owner"), app.getDocument().getComponents().getSchemas().names());
    }

    private void commit(TextField field, String newName) {
        interact(() -> {
            field.setText(newName);
            field.fireEvent(new ActionEvent());
        });
    }

    private TextField nameField(String id) {
        return lookup(id).queryAs(TextField.class);
    }

    @SuppressWarnings("unchecked")
    private List<String> selectedPath() {
        TreeItem<Object> selected = ((TreeView<Object>) lookup(".tree-view").query())
                .getSelectionModel().getSelectedItem();
        List<String> labels = new ArrayList<>();
        for (TreeItem<Object> item = selected; item != null && item.getParent() != null; item = item.getParent()) {
            labels.add(0, String.valueOf(item.getValue()));
        }
        return labels;
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
