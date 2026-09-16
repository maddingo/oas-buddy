package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.document.OasDocument;
import no.maddin.oasbuddy.core.model.Server;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public final class ServersPane {

    private ServersPane() {
    }

    public static Node build(OasDocument document) {
        VBox root = new VBox(8);
        root.setPadding(new Insets(12));

        VBox rows = new VBox(6);
        refresh(document, rows);

        Button addButton = new Button("Add server");
        addButton.setOnAction(e -> {
            document.addServer("https://");
            refresh(document, rows);
        });

        root.getChildren().addAll(rows, addButton);
        return root;
    }

    private static void refresh(OasDocument document, VBox rows) {
        rows.getChildren().clear();
        var servers = document.getServers();
        for (int i = 0; i < servers.size(); i++) {
            Server server = servers.get(i);

            TextField urlField = new TextField(server.getUrl() == null ? "" : server.getUrl());
            urlField.setPromptText("URL");
            urlField.textProperty().addListener((obs, oldVal, newVal) -> server.setUrl(newVal));

            TextField descriptionField = new TextField(server.getDescription() == null ? "" : server.getDescription());
            descriptionField.setPromptText("Description");
            descriptionField.textProperty().addListener((obs, oldVal, newVal) -> server.setDescription(newVal));

            int index = i;
            Button removeButton = new Button("Remove");
            removeButton.setOnAction(e -> {
                document.removeServer(index);
                refresh(document, rows);
            });

            rows.getChildren().add(new HBox(6,
                    new Label("URL"), urlField, new Label("Description"), descriptionField, removeButton));
        }
    }
}
