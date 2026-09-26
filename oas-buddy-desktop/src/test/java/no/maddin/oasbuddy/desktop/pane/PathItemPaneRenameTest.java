package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.HttpMethod;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The rename control on a path's own editor header. */
class PathItemPaneRenameTest extends ApplicationTest {

    private final List<String[]> renameRequests = new ArrayList<>();
    private OasDocument document;

    @Override
    public void start(Stage stage) {
        document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getPaths().addPath("/pets").addOperation(HttpMethod.GET).setOperationId("listPets");
        document.getPaths().addPath("/owners");

        Node pane = PathItemPane.build(document, "/pets", () -> { }, this::onRenamePath,
                (question, details) -> true, () -> { }, method -> { });
        stage.setScene(new Scene(new StackPane(pane), 900, 600));
        stage.show();
    }

    private boolean onRenamePath(String from, String to) {
        renameRequests.add(new String[] {from, to});
        return document.getPaths().renamePath(from, to);
    }

    @Test
    void showsTheCurrentPathName() {
        assertEquals("/pets", nameField().getText());
    }

    @Test
    void committingANewNameReportsTheRequest() {
        commit("/animals");

        assertEquals(1, renameRequests.size());
        assertEquals("/pets", renameRequests.get(0)[0]);
        assertEquals("/animals", renameRequests.get(0)[1]);
        assertEquals("/animals", nameField().getText());
    }

    @Test
    void collidingWithAnExistingPathIsRefusedWithoutReportingIt() {
        commit("/owners");

        assertTrue(renameRequests.isEmpty(), "a collision is caught before the callback is asked");
        assertEquals("/pets", nameField().getText(), "the field reverts to the old name");
    }

    @Test
    void aBlankNameIsRefused() {
        commit("   ");

        assertTrue(renameRequests.isEmpty());
        assertEquals("/pets", nameField().getText());
    }

    @Test
    void committingTheSameNameIsANoOpAndAsksNothing() {
        commit("/pets");

        assertTrue(renameRequests.isEmpty());
        assertEquals("/pets", nameField().getText());
    }

    private void commit(String newName) {
        interact(() -> {
            TextField field = nameField();
            field.setText(newName);
            field.fireEvent(new ActionEvent());
        });
    }

    private TextField nameField() {
        return lookup("#path-name").queryAs(TextField.class);
    }
}
