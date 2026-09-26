package no.maddin.oasbuddy.desktop;

import no.maddin.oasbuddy.core.model.HttpMethod;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.GridPane;
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
        app.getDocument().getComponents().getSecuritySchemes().addScheme("ApiKeyAuth").setType("apiKey");
        app.getDocument().getComponents().getSecuritySchemes().addScheme("OAuth2Auth").setType("oauth2");
        app.getDocument().getTags().add("pets");
        app.getDocument().getComponents().getResponses().addResponse("NotFound").setDescription("Not here");
        app.getDocument().getComponents().getParameters().addParameter("Limit", "limit", "query");
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

    @Test
    void removingATagLeavesTheEditorOnTheTagList() {
        selectOutline("Tags");

        interact(() -> removeButtonInRowOf(lookup("#tags-list").queryAs(GridPane.class), "pets").fire());

        assertFalse(app.getDocument().getTags().names().contains("pets"));
        assertTrue(lookup("#tags-list").tryQuery().isPresent(), "should still be on the tag list");
    }

    /**
     * {@code pane.GridPanes} is package-private to the pane tests, so this outline-level test finds
     * the row's own remove button the same way, without depending on that helper.
     */
    private static Button removeButtonInRowOf(GridPane grid, String entry) {
        int targetRow = grid.getChildren().stream()
                .filter(cell -> cell instanceof Label label && entry.equals(label.getText()))
                .mapToInt(cell -> GridPane.getRowIndex(cell) == null ? 0 : GridPane.getRowIndex(cell))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no row for " + entry));
        return grid.getChildren().stream()
                .filter(cell -> cell instanceof Button
                        && (GridPane.getRowIndex(cell) == null ? 0 : GridPane.getRowIndex(cell)) == targetRow)
                .map(Button.class::cast)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no button for " + entry));
    }

    @Test
    void theOutlineListsEverySecurityScheme() {
        assertEquals(List.of("ApiKeyAuth", "OAuth2Auth"), outlineChildren("Security Schemes"));
    }

    @Test
    void theOutlineOffersTheDocumentSecuritySection() {
        TreeView<Object> outline = lookup(".tree-view").query();

        assertTrue(outline.getRoot().getChildren().stream()
                        .anyMatch(child -> "Security".equals(String.valueOf(child.getValue()))),
                "no document security node in the outline");
    }

    @Test
    void removingASecuritySchemeLeavesTheEditorOnTheSchemeList() {
        selectOutline("Security Schemes", "ApiKeyAuth");

        interact(() -> deleteButton("#delete-security-scheme").fire());

        assertFalse(app.getDocument().getComponents().getSecuritySchemes().names().contains("ApiKeyAuth"));
        assertTrue(lookup("#security-schemes-list").tryQuery().isPresent(),
                "should land on the security scheme list");
    }

    @Test
    void componentResponsesAppearInTheOutline() {
        assertEquals(List.of("NotFound"), outlineChildren("Responses"));
    }

    @Test
    void removingAResponseLeavesTheEditorOnTheResponseList() {
        selectOutline("Responses", "NotFound");

        interact(() -> deleteButton("#delete-response").fire());

        assertFalse(app.getDocument().getComponents().getResponses().names().contains("NotFound"));
        assertTrue(lookup("#responses-list").tryQuery().isPresent(), "should land on the response list");
    }

    @Test
    void componentParametersAppearInTheOutline() {
        assertEquals(List.of("Limit"), outlineChildren("Parameters"));
    }

    @Test
    void removingAParameterLeavesTheEditorOnTheParameterList() {
        selectOutline("Parameters", "Limit");

        interact(() -> deleteButton("#delete-parameter").fire());

        assertFalse(app.getDocument().getComponents().getParameters().names().contains("Limit"));
        assertTrue(lookup("#parameters-list").tryQuery().isPresent(), "should land on the parameter list");
    }

    @SuppressWarnings("unchecked")
    private List<String> outlineChildren(String label) {
        TreeView<Object> outline = lookup(".tree-view").query();
        return outline.getRoot().getChildren().stream()
                .filter(child -> label.equals(String.valueOf(child.getValue())))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no outline node " + label))
                .getChildren().stream()
                .map(child -> String.valueOf(child.getValue()))
                .toList();
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
