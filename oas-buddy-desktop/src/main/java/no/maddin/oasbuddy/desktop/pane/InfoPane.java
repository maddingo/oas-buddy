package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.model.Info;
import javafx.scene.Node;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

public final class InfoPane {

    private InfoPane() {
    }

    public static Node build(Info info) {
        GridPane general = FormFields.grid();
        int row = 0;
        FormFields.textRow(general, row++, "Title", info::getTitle, info::setTitle);
        FormFields.textRow(general, row++, "Version", info::getVersion, info::setVersion);
        FormFields.textAreaRow(general, row++, "Description", info::getDescription, info::setDescription);
        FormFields.textRow(general, row, "Terms of service", info::getTermsOfService, info::setTermsOfService);

        GridPane contact = FormFields.grid();
        FormFields.textRow(contact, 0, "Name", () -> info.getContact().getName(), v -> info.getContact().setName(v));
        FormFields.textRow(contact, 1, "Url", () -> info.getContact().getUrl(), v -> info.getContact().setUrl(v));
        FormFields.textRow(contact, 2, "Email", () -> info.getContact().getEmail(), v -> info.getContact().setEmail(v));

        GridPane license = FormFields.grid();
        FormFields.textRow(license, 0, "Name", () -> info.getLicense().getName(), v -> info.getLicense().setName(v));
        FormFields.textRow(license, 1, "Url", () -> info.getLicense().getUrl(), v -> info.getLicense().setUrl(v));

        return FormFields.root(
                new VBox(10, FormFields.heading("General"), general),
                new TitledPane("Contact", contact),
                new TitledPane("License", license));
    }
}
