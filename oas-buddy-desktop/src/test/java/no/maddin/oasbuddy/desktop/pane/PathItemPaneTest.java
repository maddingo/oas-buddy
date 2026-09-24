package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.HttpMethod;
import no.maddin.oasbuddy.core.model.PathItem;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PathItemPaneTest extends ApplicationTest {

    private final List<HttpMethod> operationRemovals = new ArrayList<>();
    private final List<String> pathRemovals = new ArrayList<>();

    @Override
    public void start(Stage stage) {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        PathItem pets = document.getPaths().addPath("/pets");
        pets.addOperation(HttpMethod.GET).setOperationId("listPets");
        pets.addOperation(HttpMethod.POST).setOperationId("createPet");

        Node pane = PathItemPane.build(document, "/pets", () -> { }, (from, to) -> true,
                () -> pathRemovals.add("/pets"), operationRemovals::add);
        stage.setScene(new Scene(new StackPane(pane), 900, 600));
        stage.show();
    }

    @Test
    void listsTheOperationsThePathAlreadyHas() {
        assertEquals(List.of("GET — listPets", "POST — createPet"), GridPanes.entries(operations()));
    }

    @Test
    void askingToRemoveAnOperationReportsItsMethod() {
        interact(() -> GridPanes.buttonInRowOf(operations(), "POST — createPet").fire());

        assertEquals(List.of(HttpMethod.POST), operationRemovals);
    }

    @Test
    void askingToDeleteThePathReportsIt() {
        Button delete = lookup("#delete-path").queryButton();

        interact(delete::fire);

        assertEquals(List.of("/pets"), pathRemovals);
    }

    private GridPane operations() {
        return lookup("#path-operations").queryAs(GridPane.class);
    }
}
