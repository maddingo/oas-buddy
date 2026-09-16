package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.Server;
import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class ServersPane {

    private ServersPane() {
    }

    public static Node build(OasDocument document) {
        VBox rows = new VBox(8);
        refresh(document, rows);

        Button addButton = new Button("Add server");
        addButton.getStyleClass().add(Styles.ACCENT);
        addButton.setOnAction(e -> {
            document.addServer("https://");
            refresh(document, rows);
        });

        return FormFields.root(FormFields.heading("Servers"), rows, addButton);
    }

    private static void refresh(OasDocument document, VBox rows) {
        rows.getChildren().clear();
        var servers = document.getServers();
        for (int i = 0; i < servers.size(); i++) {
            Server server = servers.get(i);

            TextField urlField = new TextField(server.getUrl() == null ? "" : server.getUrl());
            urlField.setPromptText("URL");
            urlField.textProperty().addListener((obs, oldVal, newVal) -> server.setUrl(newVal));
            HBox.setHgrow(urlField, Priority.SOMETIMES);

            TextField descriptionField = new TextField(server.getDescription() == null ? "" : server.getDescription());
            descriptionField.setPromptText("Description");
            descriptionField.textProperty().addListener((obs, oldVal, newVal) -> server.setDescription(newVal));
            HBox.setHgrow(descriptionField, Priority.ALWAYS);

            int index = i;
            Button removeButton = new Button("Remove");
            removeButton.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
            removeButton.setOnAction(e -> {
                document.removeServer(index);
                refresh(document, rows);
            });

            HBox row = new HBox(8, new Label("URL"), urlField, new Label("Description"), descriptionField, removeButton);
            row.setAlignment(Pos.CENTER_LEFT);
            rows.getChildren().add(row);
        }
    }
}
