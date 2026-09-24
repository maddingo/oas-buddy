package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TagsPaneTest extends ApplicationTest {

    private final List<String> removalRequests = new ArrayList<>();
    private OasDocument document;

    @Override
    public void start(Stage stage) {
        document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getTags().add("pets").setDescription("Everything about pets");
        document.getTags().add("store");

        Node pane = TagsPane.build(document, () -> { }, removalRequests::add);
        stage.setScene(new Scene(new StackPane(pane), 900, 600));
        stage.show();
    }

    @Test
    void listsOneRemovableRowPerTag() {
        assertEquals(List.of("pets", "store"), GridPanes.entries(list()));
    }

    @Test
    void aTagsDescriptionIsEditable() {
        TextField description = (TextField) GridPanes.cellInRowOf(list(), "pets", 1);

        interact(() -> description.setText("All things pets"));

        assertEquals("All things pets", document.getTags().get("pets").getDescription());
    }

    @Test
    void askingToRemoveATagReportsThatTagByName() {
        interact(() -> GridPanes.buttonInRowOf(list(), "store").fire());

        assertEquals(List.of("store"), removalRequests);
    }

    @Test
    void removalIsNotAppliedByThePaneItself() {
        interact(() -> GridPanes.buttonInRowOf(list(), "pets").fire());

        assertTrue(GridPanes.entries(list()).contains("pets"), "the pane must delegate, not delete");
    }

    @Test
    void aNewTagIsAddedToTheRootList() {
        TextField nameField = lookup("#new-tag-name").queryAs(TextField.class);
        Button add = lookup("#add-tag").queryButton();

        interact(() -> {
            nameField.setText("users");
            add.fire();
        });

        assertEquals(List.of("pets", "store", "users"), document.getTags().names());
    }

    @Test
    void aBlankNameAddsNothing() {
        Button add = lookup("#add-tag").queryButton();

        interact(() -> {
            lookup("#new-tag-name").queryAs(TextField.class).setText("   ");
            add.fire();
        });

        assertEquals(List.of("pets", "store"), document.getTags().names());
    }

    private GridPane list() {
        return lookup("#tags-list").queryAs(GridPane.class);
    }
}
