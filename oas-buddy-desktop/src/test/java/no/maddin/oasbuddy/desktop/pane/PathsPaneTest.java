package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PathsPaneTest extends ApplicationTest {

    private final List<String> removalRequests = new ArrayList<>();

    @Override
    public void start(Stage stage) {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getPaths().addPath("/pets");
        document.getPaths().addPath("/pets/{petId}");

        Node pane = PathsPane.build(document, () -> { }, removalRequests::add);
        stage.setScene(new Scene(new StackPane(pane), 900, 600));
        stage.show();
    }

    @Test
    void listsOneRemovableRowPerPath() {
        assertEquals(List.of("/pets", "/pets/{petId}"), GridPanes.entries(list()));
    }

    @Test
    void askingToRemoveAPathReportsThatPath() {
        interact(() -> GridPanes.buttonInRowOf(list(), "/pets/{petId}").fire());

        assertEquals(List.of("/pets/{petId}"), removalRequests);
    }

    @Test
    void removalIsNotAppliedByThePaneItself() {
        interact(() -> GridPanes.buttonInRowOf(list(), "/pets").fire());

        assertTrue(GridPanes.entries(list()).contains("/pets"), "the pane must delegate, not delete");
    }

    private GridPane list() {
        return lookup("#paths-list").queryAs(GridPane.class);
    }
}
