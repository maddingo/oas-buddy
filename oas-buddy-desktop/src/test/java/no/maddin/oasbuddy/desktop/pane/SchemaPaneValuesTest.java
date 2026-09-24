package no.maddin.oasbuddy.desktop.pane;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.Schema;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.service.query.NodeQuery;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** enum, default, example, title and the flags, on the schema and on its properties. */
class SchemaPaneValuesTest extends ApplicationTest {

    private static final int TYPE_COLUMN = 1;
    private static final int DETAILS_COLUMN = 4;

    private OasDocument document;
    private StackPane holder;

    @Override
    public void start(Stage stage) {
        document = OasDocument.newDocument(DocumentFormat.YAML);
        var schemas = document.getComponents().getSchemas();
        Schema size = schemas.addSchema("Size");
        size.changeTypeTo("integer");
        Schema pet = schemas.addSchema("Pet");
        pet.changeTypeTo("object");
        pet.addProperty("age").changeTypeTo("integer");
        pet.addProperty("owner").referTo("Size");

        holder = new StackPane();
        stage.setScene(new Scene(holder, 1100, 900));
        stage.show();
        show("Size");
    }

    private void show(String schemaName) {
        Node pane = SchemaPane.build(document, schemaName, (from, to) -> true, name -> { });
        interact(() -> holder.getChildren().setAll(pane));
    }

    @Test
    void aDefaultIsWrittenAsTheSchemasType() {
        interact(() -> values().lookup("." + SchemaDetails.DEFAULT).queryAs(TextField.class).setText("42"));

        JsonNode value = schema("Size").getDefault();
        assertTrue(value.isInt(), () -> "written as " + value.getNodeType());
    }

    @Test
    void anExampleThatIsNotOfTheTypeIsKeptLiterallyForValidationToReport() {
        interact(() -> values().lookup("." + SchemaDetails.EXAMPLE).queryAs(TextField.class).setText("many"));

        assertEquals("many", schema("Size").getExample().asText());
    }

    @Test
    void clearingADefaultRemovesTheKey() {
        TextField field = values().lookup("." + SchemaDetails.DEFAULT).queryAs(TextField.class);
        interact(() -> field.setText("42"));
        interact(() -> field.setText(""));

        assertFalse(raw("Size").has("default"));
    }

    @Test
    void theTitleIsInTheMainForm() {
        TextField title = (TextField) fieldAfterLabel("Title");
        interact(() -> title.setText("Shoe size"));

        assertEquals("Shoe size", schema("Size").getTitle());
    }

    @Test
    void enumValuesAreEnteredTypedAndInOrder() {
        addEnumValues("1", "2", "3");

        assertEquals("[1,2,3]", raw("Size").get("enum").toString());
    }

    @Test
    void enumValuesCanBeReordered() {
        addEnumValues("1", "2", "3");

        interact(() -> enumList().getSelectionModel().select(2));
        interact(() -> enumButton("up").fire());

        assertAll(
                () -> assertEquals("[1,3,2]", raw("Size").get("enum").toString()),
                () -> assertEquals(1, enumList().getSelectionModel().getSelectedIndex(),
                        "the moved value stays selected, so it can be moved again"));
    }

    @Test
    void enumValuesCanBeRemovedOneByOne() {
        addEnumValues("1", "2", "3");

        interact(() -> enumList().getSelectionModel().select(0));
        interact(() -> enumButton("remove").fire());

        assertEquals("[2,3]", raw("Size").get("enum").toString());
    }

    @Test
    void clearingTheEnumRemovesTheKey() {
        addEnumValues("1", "2");

        interact(() -> enumButton("clear").fire());

        assertFalse(raw("Size").has("enum"));
    }

    @Test
    void aFlagIsWrittenOnlyWhileTicked() {
        CheckBox deprecated = flag(values(), "Deprecated");

        interact(() -> deprecated.setSelected(true));
        assertTrue(raw("Size").get("deprecated").asBoolean());

        interact(() -> deprecated.setSelected(false));
        assertFalse(raw("Size").has("deprecated"));
    }

    @Test
    void openingASchemaWritesNoFlags() {
        show("Pet");

        assertEquals(List.of("type", "properties"), fieldNames(raw("Pet")));
    }

    @Test
    void aPropertysDetailsOpenBelowItsRow() {
        show("Pet");
        openDetails("age");

        Node details = detailsOf("age");
        interact(() -> from(details).lookup("." + SchemaDetails.DEFAULT).queryAs(TextField.class).setText("3"));
        interact(() -> flag(from(details), "Read only").setSelected(true));

        Schema age = schema("Pet").getProperty("age");
        assertAll(
                () -> assertTrue(age.getDefault().isInt()),
                () -> assertTrue(age.isReadOnly()));
    }

    @Test
    void aPropertysDetailsIncludeItsTitleAndDescription() {
        show("Pet");
        openDetails("age");

        List<String> labels = from(detailsOf("age")).lookup(".label").queryAll().stream()
                .map(node -> ((Label) node).getText()).toList();

        assertTrue(labels.containsAll(List.of("Title", "Description")), () -> "labels: " + labels);
    }

    @Test
    void openDetailsStayOpenWhenThePropertysTypeChanges() {
        show("Pet");
        openDetails("age");

        @SuppressWarnings("unchecked")
        ComboBox<TypeChoice> type = (ComboBox<TypeChoice>) cell("age", TYPE_COLUMN);
        interact(() -> type.setValue(TypeChoice.type("number")));

        assertTrue(detailsOf("age") != null, "the details folded on a type change");
    }

    @Test
    void aReferencePropertyHasNoDetails() {
        show("Pet");

        assertNull(cell("owner", DETAILS_COLUMN));
    }

    private void addEnumValues(String... texts) {
        TextField input = values().lookup("." + SchemaDetails.ENUM_INPUT).queryAs(TextField.class);
        for (String text : texts) {
            interact(() -> {
                input.setText(text);
                enumButton("add").fire();
            });
        }
    }

    private NodeQuery values() {
        Node values = lookup("#schema-values").query();
        return from(values);
    }

    @SuppressWarnings("unchecked")
    private ListView<JsonNode> enumList() {
        return values().lookup("." + SchemaDetails.ENUM).queryAs(ListView.class);
    }

    private Button enumButton(String name) {
        return values().lookup(".schema-enum-" + name).queryButton();
    }

    private static CheckBox flag(NodeQuery scope, String text) {
        return scope.lookup(".check-box").queryAll().stream()
                .map(CheckBox.class::cast)
                .filter(box -> text.equals(box.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no flag " + text));
    }

    private void openDetails(String property) {
        ToggleButton toggle = (ToggleButton) cell(property, DETAILS_COLUMN);
        interact(toggle::fire);
    }

    /** The node spanning the row right below a property's own row, if its details are open. */
    private Node detailsOf(String property) {
        GridPane grid = properties();
        int propertyRow = GridPanes.row(GridPanes.cellInRowOf(grid, property, 0));
        return grid.getChildren().stream()
                .filter(cell -> GridPanes.row(cell) == propertyRow + 1 && GridPanes.column(cell) == 1)
                .findFirst()
                .orElse(null);
    }

    private Node cell(String property, int column) {
        return GridPanes.cellInRowOf(properties(), property, column);
    }

    private GridPane properties() {
        return lookup("#schema-properties").queryAs(GridPane.class);
    }

    /** The control next to a first-column label of the schema's main form. */
    private Node fieldAfterLabel(String text) {
        Label label = lookup(".label").queryAll().stream()
                .map(Label.class::cast)
                .filter(candidate -> text.equals(candidate.getText()))
                .findFirst()
                .orElseThrow();
        GridPane grid = (GridPane) label.getParent();
        return grid.getChildren().stream()
                .filter(cell -> GridPanes.row(cell) == GridPanes.row(label) && GridPanes.column(cell) == 1)
                .findFirst()
                .orElseThrow();
    }

    private Schema schema(String name) {
        return document.getComponents().getSchemas().getSchema(name);
    }

    private ObjectNode raw(String name) {
        return (ObjectNode) document.getRoot().get("components").get("schemas").get(name);
    }

    private static List<String> fieldNames(JsonNode node) {
        List<String> names = new ArrayList<>();
        node.fieldNames().forEachRemaining(names::add);
        return names;
    }
}
