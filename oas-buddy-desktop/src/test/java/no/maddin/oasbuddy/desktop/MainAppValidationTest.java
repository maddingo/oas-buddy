package no.maddin.oasbuddy.desktop;

import no.maddin.oasbuddy.core.model.Constraint;
import no.maddin.oasbuddy.core.model.HttpMethod;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The validation panel shows both severities, prefixed so they read apart, and the
 * "No validation errors." fallback only appears when there is truly nothing to show.
 */
class MainAppValidationTest extends ApplicationTest {

    private MainApp app;

    @Override
    public void start(Stage stage) {
        app = new MainApp();
        app.start(stage);
    }

    @Test
    void aFreshDocumentHasNoValidationMessages() {
        interact(this::validate);

        assertEquals(List.of("No validation errors."), items());
    }

    /** An operation using a tag nothing declares is a warning, not an error (see CLAUDE.md Tags). */
    @Test
    void anUndefinedOperationTagIsShownAsAWarning() {
        app.getDocument().getPaths().addPath("/pets").addOperation(HttpMethod.GET)
                .setTags(List.of("ghost"));

        interact(this::validate);

        assertTrue(items().stream().anyMatch(m -> m.startsWith("Warning: ") && m.contains("undefined tag")),
                () -> "messages: " + items());
        assertTrue(items().stream().noneMatch("No validation errors."::equals),
                "a warning is a message, the fallback text must not also show");
    }

    @Test
    void aRealConstraintProblemIsShownAsAnError() {
        var pet = app.getDocument().getComponents().getSchemas().addSchema("Pet");
        pet.changeTypeTo("string");
        pet.setConstraint(Constraint.MIN_LENGTH, JsonNodeFactory.instance.numberNode(10));
        pet.setConstraint(Constraint.MAX_LENGTH, JsonNodeFactory.instance.numberNode(2));

        interact(this::validate);

        assertTrue(items().stream().anyMatch(m -> m.startsWith("Error: ") && m.contains("minLength")),
                () -> "messages: " + items());
    }

    private void validate() {
        lookup("Validate").queryButton().fire();
    }

    @SuppressWarnings("unchecked")
    private ObservableList<String> items() {
        return ((ListView<String>) lookup(".list-view").query()).getItems();
    }
}
