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

class SchemasPaneTest extends ApplicationTest {

    private final List<String> removalRequests = new ArrayList<>();

    @Override
    public void start(Stage stage) {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getComponents().getSchemas().addSchema("Pet").setType("object");
        document.getComponents().getSchemas().addSchema("NewPet").setType("object");

        Node pane = SchemasPane.build(document, () -> { }, removalRequests::add);
        stage.setScene(new Scene(new StackPane(pane), 900, 600));
        stage.show();
    }

    @Test
    void listsOneRemovableRowPerSchema() {
        assertEquals(List.of("Pet", "NewPet"), GridPanes.entries(list()));
    }

    @Test
    void askingToRemoveASchemaReportsThatSchemaByName() {
        interact(() -> GridPanes.buttonInRowOf(list(), "NewPet").fire());

        assertEquals(List.of("NewPet"), removalRequests);
    }

    @Test
    void removalIsNotAppliedByThePaneItself() {
        interact(() -> GridPanes.buttonInRowOf(list(), "Pet").fire());

        assertTrue(GridPanes.entries(list()).contains("Pet"), "the pane must delegate, not delete");
    }

    private GridPane list() {
        return lookup("#schemas-list").queryAs(GridPane.class);
    }
}
