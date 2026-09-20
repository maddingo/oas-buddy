package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.core.model.OAuthFlow;
import no.maddin.oasbuddy.core.model.OAuthFlowUrl;
import no.maddin.oasbuddy.core.model.OAuthFlows;
import no.maddin.oasbuddy.core.model.SecurityScheme;
import atlantafx.base.theme.Styles;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

/**
 * The {@code flows} section of an oauth2 security scheme: the four flows OAS 3.0 defines, each
 * switched on and off independently, each showing only the URLs it actually carries and its own
 * scope map.
 *
 * <p>Which URLs a flow has comes from {@link OAuthFlows#urlsOf(String)} rather than from a switch
 * here, so the spec is stated once, in core.
 *
 * <p>Turning a flow off discards its URLs and scopes, so a flow that holds anything is confirmed
 * first; an empty one goes without a dialog, since there is nothing to lose. Declining leaves both
 * the document and the switch as they were — the form must never disagree with the document.
 */
public final class OAuthFlowsPane {

    private static final Map<OAuthFlowUrl, String> URL_LABELS = Map.of(
            OAuthFlowUrl.AUTHORIZATION_URL, "Authorization URL",
            OAuthFlowUrl.TOKEN_URL, "Token URL",
            OAuthFlowUrl.REFRESH_URL, "Refresh URL");

    private OAuthFlowsPane() {
    }

    /**
     * @param confirmation asked before a flow that holds something is turned off
     * @param onChanged    run after the document's structure changes, to rebuild this section;
     *                     invoked on the JavaFX thread from inside a control's listener
     */
    public static Node build(SecurityScheme scheme, RemovalConfirmation confirmation, Runnable onChanged) {
        OAuthFlows flows = scheme.getFlows();

        VBox section = new VBox(14);
        section.setId("oauth-flows");
        for (String flowName : OAuthFlows.FLOW_NAMES) {
            section.getChildren().add(flowBlock(flows, flowName, confirmation, onChanged));
        }
        return section;
    }

    private static Node flowBlock(OAuthFlows flows, String flowName,
                                  RemovalConfirmation confirmation, Runnable onChanged) {
        OAuthFlow flow = flows.getFlow(flowName);

        CheckBox enabled = new CheckBox(flowName);
        enabled.setId("flow-" + flowName + "-enabled");
        enabled.getStyleClass().add(Styles.TEXT_BOLD);
        enabled.setSelected(flow != null);
        // Reverting a declined switch would otherwise re-enter this listener.
        boolean[] reverting = {false};
        enabled.selectedProperty().addListener((obs, was, now) -> {
            if (reverting[0]) {
                return;
            }
            if (now) {
                flows.addFlow(flowName);
                onChanged.run();
                return;
            }
            OAuthFlow existing = flows.getFlow(flowName);
            if (existing != null && !existing.isEmpty()
                    && !confirmation.confirm("Turn off the " + flowName + " flow?", describe(existing))) {
                reverting[0] = true;
                enabled.setSelected(true);
                reverting[0] = false;
                return;
            }
            flows.removeFlow(flowName);
            onChanged.run();
        });

        VBox block = new VBox(8, enabled);
        if (flow == null) {
            return block;
        }

        block.getChildren().addAll(urlGrid(flowName, flow), scopes(flowName, flow, onChanged));
        block.setPadding(new Insets(0, 0, 0, 0));
        return block;
    }

    private static Node urlGrid(String flowName, OAuthFlow flow) {
        GridPane grid = FormFields.grid();
        grid.setId("flow-" + flowName + "-urls");
        grid.setPadding(new Insets(0, 0, 0, 24));

        int row = 0;
        for (OAuthFlowUrl url : OAuthFlows.urlsOf(flowName)) {
            TextField field = FormFields.textRow(grid, row++, URL_LABELS.get(url),
                    () -> flow.getUrl(url), value -> flow.setUrl(url, value));
            field.setId("flow-" + flowName + "-" + url.field());
        }
        return grid;
    }

    private static Node scopes(String flowName, OAuthFlow flow, Runnable onChanged) {
        GridPane grid = FormFields.grid();
        grid.setId("flow-" + flowName + "-scopes");
        grid.getColumnConstraints().addAll(
                FormFields.column(HPos.LEFT, Priority.NEVER),
                FormFields.column(HPos.LEFT, Priority.ALWAYS),
                FormFields.column(HPos.RIGHT, Priority.NEVER));

        List<String> names = flow.scopeNames();
        if (names.isEmpty()) {
            Label empty = new Label("No scopes yet.");
            empty.getStyleClass().add(Styles.TEXT_MUTED);
            grid.add(empty, 0, 0, 3, 1);
        } else {
            grid.addRow(0, FormFields.columnHeading("Scope"), FormFields.columnHeading("Description"));
            int row = 1;
            for (int index = 0; index < names.size(); index++) {
                grid.addRow(row++, scopeRow(flowName, flow, names.get(index), index, onChanged));
            }
        }

        TextField newScope = new TextField();
        newScope.setId("flow-" + flowName + "-new-scope");
        newScope.setPromptText("read:pets");
        Button add = new Button("Add scope");
        add.setId("flow-" + flowName + "-add-scope");
        add.getStyleClass().add(Styles.ACCENT);
        add.setOnAction(e -> {
            String name = newScope.getText();
            if (name != null && !name.isBlank()) {
                flow.setScope(name.strip(), "");
                onChanged.run();
            }
        });

        VBox box = new VBox(8, FormFields.columnHeading("Scopes"), grid, new HBox(8, newScope, add));
        box.setPadding(new Insets(0, 0, 0, 24));
        box.setAlignment(Pos.TOP_LEFT);
        return box;
    }

    private static Node[] scopeRow(String flowName, OAuthFlow flow, String scope, int index, Runnable onChanged) {
        TextField name = new TextField(scope);
        name.setId("flow-" + flowName + "-scope-name-" + index);
        // Renamed on commit rather than per keystroke: a rename rebuilds the scope map, and doing
        // that on every character would rebuild it once per letter and fight the caret.
        name.focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) {
                renameTo(flow, scope, name.getText(), onChanged);
            }
        });
        name.setOnAction(e -> renameTo(flow, scope, name.getText(), onChanged));

        TextField description = new TextField(nullToEmpty(flow.getScopeDescription(scope)));
        description.setId("flow-" + flowName + "-scope-description-" + index);
        description.setMaxWidth(Double.MAX_VALUE);
        description.textProperty().addListener((obs, was, now) -> flow.setScope(scope, now));

        Button remove = new Button("Remove");
        remove.setId("flow-" + flowName + "-remove-scope-" + index);
        remove.getStyleClass().addAll(Styles.DANGER, Styles.BUTTON_OUTLINED);
        remove.setOnAction(e -> {
            flow.removeScope(scope);
            onChanged.run();
        });

        return new Node[]{name, description, remove};
    }

    private static void renameTo(OAuthFlow flow, String from, String to, Runnable onChanged) {
        if (to == null || to.isBlank() || to.strip().equals(from)) {
            return;
        }
        flow.renameScope(from, to.strip());
        onChanged.run();
    }

    /** The dialog's body: what turning the flow off would discard. */
    private static String describe(OAuthFlow flow) {
        StringBuilder details = new StringBuilder("This discards:");
        for (OAuthFlowUrl url : OAuthFlowUrl.values()) {
            if (flow.getUrl(url) != null) {
                details.append("\n  ").append(URL_LABELS.get(url)).append(": ").append(flow.getUrl(url));
            }
        }
        List<String> scopes = flow.scopeNames();
        if (!scopes.isEmpty()) {
            details.append("\n  scopes: ").append(String.join(", ", scopes));
        }
        return details.toString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
