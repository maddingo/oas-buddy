package no.maddin.oasbuddy.desktop.pane;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

/**
 * Asked before anything is removed from the document, so a removal can be confirmed without the
 * caller depending on a modal window: tests pass their own answer instead.
 */
@FunctionalInterface
public interface RemovalConfirmation {

    /**
     * @param question headline, e.g. {@code Remove path "/pets"?}
     * @param details  what the removal takes with it or breaks; may be empty
     * @return whether to go ahead
     */
    boolean confirm(String question, String details);

    /** The confirmation used by the running app. */
    static RemovalConfirmation dialog() {
        return (question, details) -> {
            Alert alert = alert(question, details);
            ButtonType remove = alert.getButtonTypes().get(0);
            return alert.showAndWait().filter(remove::equals).isPresent();
        };
    }

    /** The dialog itself, built separately from showing it so it can be inspected. */
    static Alert alert(String question, String details) {
        ButtonType remove = new ButtonType("Remove", ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", remove, ButtonType.CANCEL);
        alert.setTitle("OAS Buddy");
        alert.setHeaderText(question);

        Label body = new Label(details);
        body.setWrapText(true);
        // reference paths and operation lists are long; let the dialog grow instead of eliding them
        body.setMaxWidth(Double.MAX_VALUE);
        alert.getDialogPane().setContent(body);
        alert.getDialogPane().setPrefWidth(640);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.setResizable(true);
        return alert;
    }
}
