package no.maddin.oasbuddy.desktop;

import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Exercises the open->edit path within the app's own window (outline selection, form editing)
 * against the in-memory document. File Open/Save aren't covered here since they go through the
 * native OS FileChooser dialog, which TestFX cannot drive; that path is covered indirectly by
 * oas-buddy-core's DocumentReader/DocumentWriter tests.
 */
class MainAppSmokeTest extends ApplicationTest {

    private MainApp app;
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        app = new MainApp();
        app.start(stage);
    }

    @Test
    void theWindowCarriesTheAppIcon() {
        assertFalse(stage.getIcons().isEmpty(), "MainApp never set the window icon");
    }

    @Test
    void editingInfoTitleUpdatesTheDocument() {
        TreeView<Object> outline = lookup(".tree-view").query();
        interact(() -> outline.getSelectionModel().select(outline.getRoot().getChildren().get(0)));

        TextField titleField = lookup(".text-field").nth(0).query();
        interact(() -> {
            titleField.clear();
            titleField.setText("Updated Title");
        });

        assertEquals("Updated Title", app.getDocument().getInfo().getTitle());
    }
}
