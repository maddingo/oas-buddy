package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import atlantafx.base.theme.Styles;
import javafx.scene.Node;
import javafx.scene.control.Label;

/**
 * The document-wide default security: what an operation requires unless it says otherwise.
 *
 * <p>Removing the last requirement takes the {@code security} key out rather than leaving an empty
 * array: at document level the two mean the same thing, and the empty array is noise in the file.
 * For an operation they differ — an empty array is "explicitly public" — which is why that
 * distinction lives in {@link OperationPane} and not here.
 */
public final class SecurityPane {

    private SecurityPane() {
    }

    public static Node build(OasDocument document, Runnable onChanged) {
        Label explanation = new Label("Applies to every operation that does not override it. "
                + "Satisfying any one requirement is enough.");
        explanation.getStyleClass().add(Styles.TEXT_MUTED);
        explanation.setWrapText(true);

        Node requirements = SecurityRequirementsPane.build(
                document.getSecurity(), SecuritySchemeCatalog.of(document), () -> {
                    if (document.getSecurity().requirements().isEmpty()) {
                        document.getSecurity().undeclare();
                    }
                    onChanged.run();
                });

        return FormFields.root(FormFields.heading("Security"), explanation, requirements);
    }
}
