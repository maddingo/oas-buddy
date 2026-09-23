package no.maddin.oasbuddy.desktop.pane;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.maddin.oasbuddy.core.document.DocumentFormat;
import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.Constraint;
import no.maddin.oasbuddy.core.model.Schema;
import atlantafx.base.theme.Styles;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;
import org.testfx.service.query.NodeQuery;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The constraints section: one group per type, numbers written as numbers, nothing silently lost. */
class SchemaPaneConstraintsTest extends ApplicationTest {

    private static final int TYPE_COLUMN = 1;
    private static final int DETAILS_COLUMN = 4;

    private OasDocument document;
    private StackPane holder;

    @Override
    public void start(Stage stage) {
        document = OasDocument.newDocument(DocumentFormat.YAML);
        var schemas = document.getComponents().getSchemas();
        schemas.addSchema("Code").changeTypeTo("string");
        Schema pet = schemas.addSchema("Pet");
        pet.changeTypeTo("object");
        pet.addProperty("age").changeTypeTo("integer");

        holder = new StackPane();
        stage.setScene(new Scene(holder, 1100, 1000));
        stage.show();
        show("Code");
    }

    private void show(String schemaName) {
        Node pane = SchemaPane.build(document, schemaName, name -> { });
        interact(() -> holder.getChildren().setAll(pane));
    }

    @Test
    void aStringSchemaOffersOnlyStringConstraints() {
        assertEquals(List.of("Min length", "Max length", "Pattern"), labels(constraints()));
    }

    @Test
    void eachTypeOffersItsOwnGroup() {
        interact(() -> schemaType().setValue("array"));
        assertEquals(List.of("Min items", "Max items", "Unique items"), labels(constraints()));

        interact(() -> schemaType().setValue("number"));
        assertEquals(List.of("Minimum", "Exclusive minimum", "Maximum", "Exclusive maximum", "Multiple of"),
                labels(constraints()));

        interact(() -> schemaType().setValue("boolean"));
        assertEquals(List.of(), labels(constraints()));
    }

    @Test
    void aLengthIsWrittenAsANumber() {
        interact(() -> field(constraints(), Constraint.MAX_LENGTH).setText("8"));

        JsonNode value = code().getConstraint(Constraint.MAX_LENGTH);
        assertTrue(value.isInt(), () -> "written as " + value.getNodeType());
    }

    @Test
    void textThatIsNoNumberIsMarkedAndNotWritten() {
        TextField maxLength = field(constraints(), Constraint.MAX_LENGTH);
        interact(() -> maxLength.setText("8"));
        interact(() -> maxLength.setText("8x"));

        assertAll(
                () -> assertTrue(maxLength.getPseudoClassStates().contains(Styles.STATE_DANGER)),
                () -> assertEquals(8, code().getConstraint(Constraint.MAX_LENGTH).asInt(),
                        "the last valid value stays"));
    }

    @Test
    void aContradictoryRangeIsWrittenForValidationToReport() {
        interact(() -> field(constraints(), Constraint.MIN_LENGTH).setText("10"));
        interact(() -> field(constraints(), Constraint.MAX_LENGTH).setText("2"));

        assertAll(
                () -> assertEquals(10, code().getConstraint(Constraint.MIN_LENGTH).asInt()),
                () -> assertEquals(2, code().getConstraint(Constraint.MAX_LENGTH).asInt()));
    }

    @Test
    void clearingAFieldRemovesTheKey() {
        TextField pattern = field(constraints(), Constraint.PATTERN);
        interact(() -> pattern.setText("^[A-Z]+$"));
        interact(() -> pattern.setText(""));

        assertFalse(raw("Code").has("pattern"));
    }

    @Test
    void aBooleanConstraintIsWrittenOnlyWhileTicked() {
        interact(() -> schemaType().setValue("array"));
        CheckBox unique = constraints().lookup("." + SchemaConstraints.styleClass(Constraint.UNIQUE_ITEMS))
                .queryAs(CheckBox.class);

        interact(() -> unique.setSelected(true));
        assertTrue(raw("Code").get("uniqueItems").asBoolean());

        interact(() -> unique.setSelected(false));
        assertFalse(raw("Code").has("uniqueItems"));
    }

    @Test
    void switchingTypeHidesConstraintsButKeepsThemAndSaysSo() {
        interact(() -> field(constraints(), Constraint.MAX_LENGTH).setText("8"));

        interact(() -> schemaType().setValue("integer"));

        assertAll(
                () -> assertEquals(8, code().getConstraint(Constraint.MAX_LENGTH).asInt()),
                () -> assertFalse(labels(constraints()).contains("Max length")),
                () -> assertTrue(hiddenNote(constraints()).contains("maxLength"),
                        () -> "note: " + hiddenNote(constraints())));
    }

    @Test
    void switchingBackShowsTheKeptConstraintAgain() {
        interact(() -> field(constraints(), Constraint.MAX_LENGTH).setText("8"));
        interact(() -> schemaType().setValue("integer"));

        interact(() -> schemaType().setValue("string"));

        assertEquals("8", field(constraints(), Constraint.MAX_LENGTH).getText());
    }

    @Test
    void aPropertysConstraintsFollowItsType() {
        show("Pet");
        openDetails("age");
        interact(() -> field(propertyDetails("age"), Constraint.MINIMUM).setText("0"));

        @SuppressWarnings("unchecked")
        ComboBox<TypeChoice> type = (ComboBox<TypeChoice>) cell("age", TYPE_COLUMN);
        interact(() -> type.setValue(TypeChoice.type("string")));

        List<String> shown = labels(from(propertyDetails("age")).lookup(".schema-constraints"));
        assertAll(
                () -> assertEquals(List.of("Min length", "Max length", "Pattern"), shown),
                () -> assertEquals(0, document.getComponents().getSchemas().getSchema("Pet").getProperty("age")
                        .getConstraint(Constraint.MINIMUM).asInt()));
    }

    private NodeQuery constraints() {
        Node node = lookup("#schema-constraints").query();
        return from(node);
    }

    private static TextField field(NodeQuery scope, Constraint constraint) {
        return scope.lookup("." + SchemaConstraints.styleClass(constraint)).queryAs(TextField.class);
    }

    private static List<String> labels(NodeQuery scope) {
        return scope.lookup(".label").queryAll().stream()
                .map(Label.class::cast)
                .filter(label -> label.getParent() instanceof GridPane && GridPanes.column(label) == 0)
                .map(Label::getText)
                .toList();
    }

    private static String hiddenNote(NodeQuery scope) {
        return scope.lookup("." + SchemaConstraints.HIDDEN_NOTE).queryAs(Label.class).getText();
    }

    private NodeQuery propertyDetails(String property) {
        GridPane grid = lookup("#schema-properties").queryAs(GridPane.class);
        int propertyRow = GridPanes.row(GridPanes.cellInRowOf(grid, property, 0));
        Node details = grid.getChildren().stream()
                .filter(cell -> GridPanes.row(cell) == propertyRow + 1 && GridPanes.column(cell) == 1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("details of " + property + " are not open"));
        return from(details);
    }

    private void openDetails(String property) {
        ToggleButton toggle = (ToggleButton) cell(property, DETAILS_COLUMN);
        interact(toggle::fire);
    }

    private Node cell(String property, int column) {
        return GridPanes.cellInRowOf(lookup("#schema-properties").queryAs(GridPane.class), property, column);
    }

    @SuppressWarnings("unchecked")
    private ComboBox<String> schemaType() {
        return lookup("#schema-type").queryAs(ComboBox.class);
    }

    private Schema code() {
        return document.getComponents().getSchemas().getSchema("Code");
    }

    private ObjectNode raw(String name) {
        return (ObjectNode) document.getRoot().get("components").get("schemas").get(name);
    }
}
