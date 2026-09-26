package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.HttpMethod;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OperationPaneTest extends ApplicationTest {

    private final AtomicInteger removalRequests = new AtomicInteger();
    private OasDocument document;

    @Override
    public void start(Stage stage) {
        document = OasDocument.newDocument(DocumentFormat.YAML);
        document.getComponents().getParameters().addParameter("Limit", "limit", "query");
        var operation = document.getPaths().addPath("/pets").addOperation(HttpMethod.GET);
        operation.setOperationId("listPets");

        Node pane = OperationPane.build(operation, List::of, TagCatalog.of(document),
                SecuritySchemeCatalog.of(document), ComponentCatalog.of(document), (question, details) -> true,
                removalRequests::incrementAndGet);
        stage.setScene(new Scene(new StackPane(pane), 900, 700));
        stage.show();
    }

    @Test
    void anOperationCanReferToAReusableParameter() {
        interact(() -> {
            ComboBox<String> components = lookup(".operation-parameters .reference-parameter").query();
            components.setValue("Limit");
            lookup(".operation-parameters .add-parameter-reference").queryButton().fire();
        });

        assertEquals("Limit", document.getPaths().getPathItem("/pets").getOperation(HttpMethod.GET)
                .getParameters().all().getFirst().getReferencedParameterName());
    }

    @Test
    void askingToDeleteTheOperationReportsIt() {
        Button delete = lookup("#delete-operation").queryButton();

        interact(delete::fire);

        assertEquals(1, removalRequests.get());
    }
}
