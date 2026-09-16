package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.model.Info;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;

public final class InfoPane {

    private InfoPane() {
    }

    public static Node build(Info info) {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));

        int row = 0;
        FormFields.textRow(grid, row++, "Title", info::getTitle, info::setTitle);
        FormFields.textRow(grid, row++, "Version", info::getVersion, info::setVersion);
        FormFields.textAreaRow(grid, row++, "Description", info::getDescription, info::setDescription);
        FormFields.textRow(grid, row++, "Terms of service", info::getTermsOfService, info::setTermsOfService);
        FormFields.textRow(grid, row++, "Contact name", () -> info.getContact().getName(), v -> info.getContact().setName(v));
        FormFields.textRow(grid, row++, "Contact url", () -> info.getContact().getUrl(), v -> info.getContact().setUrl(v));
        FormFields.textRow(grid, row++, "Contact email", () -> info.getContact().getEmail(), v -> info.getContact().setEmail(v));
        FormFields.textRow(grid, row++, "License name", () -> info.getLicense().getName(), v -> info.getLicense().setName(v));
        FormFields.textRow(grid, row, "License url", () -> info.getLicense().getUrl(), v -> info.getLicense().setUrl(v));

        return grid;
    }
}
