package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.Schema;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The property editors of a schema are laid out one row per property; property names differ in
 * length, so the rows only line up if they share a single grid rather than each sizing itself.
 */
class SchemaPaneLayoutTest extends ApplicationTest {

    private final List<String> removalRequests = new ArrayList<>();

    @Override
    public void start(Stage stage) {
        OasDocument document = OasDocument.newDocument(DocumentFormat.YAML);
        Schema schema = document.getComponents().getSchemas().addSchema("Pet");
        schema.setType("object");
        schema.addProperty("id").setType("integer");
        schema.addProperty("name").setType("string");
        schema.addProperty("aMuchLongerPropertyName").setType("boolean");

        Node pane = SchemaPane.build(document, "Pet", removalRequests::add);
        stage.setScene(new Scene(new StackPane(pane), 900, 600));
        stage.show();
    }

    @Test
    void propertyRowsShareTheSameColumnPositions() {
        GridPane properties = lookup("#schema-properties").queryAs(GridPane.class);

        Map<Integer, Set<Double>> startXPerColumn = new LinkedHashMap<>();
        for (Node cell : properties.getChildren()) {
            Integer column = GridPane.getColumnIndex(cell);
            startXPerColumn
                    .computeIfAbsent(column == null ? 0 : column, key -> new LinkedHashSet<>())
                    .add(cell.getLayoutX());
        }

        assertAll(startXPerColumn.entrySet().stream().map(entry -> () ->
                assertEquals(1, entry.getValue().size(),
                        "column " + entry.getKey() + " starts at several x positions: " + entry.getValue())));
    }

    @Test
    void deletingTheSchemaReportsItByName() {
        Button delete = lookup("#delete-schema").queryButton();

        interact(delete::fire);

        assertEquals(List.of("Pet"), removalRequests);
    }
}
