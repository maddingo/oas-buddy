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
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
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

    /**
     * With a duplicate name, each row must edit its own tag — looking tags up by name would point
     * both rows at the first one.
     */
    @Test
    void eachRowOfADuplicatedNameEditsItsOwnTag() {
        OasDocument duplicated = OasDocument.newDocument(DocumentFormat.YAML);
        var array = duplicated.getRoot().putArray("tags");
        array.addObject().put("name", "pets").put("description", "first");
        array.addObject().put("name", "pets").put("description", "second");
        interact(() -> stage.getScene().setRoot(new StackPane(TagsPane.build(duplicated, () -> { }, name -> { }))));

        List<TextField> descriptions = list().getChildren().stream()
                .filter(cell -> GridPanes.column(cell) == 1 && cell instanceof TextField)
                .map(TextField.class::cast)
                .toList();
        interact(() -> descriptions.get(1).setText("edited"));

        assertEquals("first", array.get(0).get("description").asText(), "the first tag must be left alone");
        assertEquals("edited", array.get(1).get("description").asText());
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
