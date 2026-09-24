package no.maddin.oasbuddy.desktop.pane;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;

/**
 * Asked before anything is removed or renamed in the document, so the action can be confirmed
 * without the caller depending on a modal window: tests pass their own answer instead. The same
 * seam serves both, since "ask, show what is at stake, go ahead only on an explicit yes" is
 * identical either way — only the affirmative button's label differs ("Remove" vs. "Rename"),
 * which {@link #dialog(String)} takes as a parameter rather than this interface growing a second
 * method.
 */
@FunctionalInterface
public interface RemovalConfirmation {

    /**
     * @param question headline, e.g. {@code Remove path "/pets"?}
     * @param details  what the action takes with it or breaks; may be empty
     * @return whether to go ahead
     */
    boolean confirm(String question, String details);

    /** The confirmation used by the running app for a removal; its button reads "Remove". */
    static RemovalConfirmation dialog() {
        return dialog("Remove");
    }

    /** The same confirmation, with a different affirmative label, e.g. "Rename" for a rename. */
    static RemovalConfirmation dialog(String actionLabel) {
        return (question, details) -> {
            Alert alert = alert(actionLabel, question, details);
            ButtonType action = alert.getButtonTypes().get(0);
            return alert.showAndWait().filter(action::equals).isPresent();
        };
    }

    /** The dialog itself, built separately from showing it so it can be inspected. */
    static Alert alert(String question, String details) {
        return alert("Remove", question, details);
    }

    /** As {@link #alert(String, String)}, with the affirmative button labelled {@code actionLabel}. */
    static Alert alert(String actionLabel, String question, String details) {
        ButtonType action = new ButtonType(actionLabel, ButtonBar.ButtonData.OK_DONE);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "", action, ButtonType.CANCEL);
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
