package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.desktop.AppIcon;
import no.maddin.oasbuddy.desktop.AppInfo;
import atlantafx.base.theme.Styles;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextFlow;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Says what the running build is, who made it and where to read about it.
 *
 * <p>Split the way {@link RemovalConfirmation} is: {@link #content} builds the body and
 * {@link #show} wraps it in a window, so the content can be inspected without driving a modal
 * dialog. Opening a link is likewise delegated to a collaborator, because the app opens links
 * through {@code HostServices}, which does not exist outside a launched application.
 *
 * <p>Everything shown comes from {@link AppInfo}, i.e. from the POM. A value the build never
 * resolved arrives empty, and an empty value means the line or link is left out entirely rather
 * than rendered blank — an unfiltered classpath should look sparse, not broken.
 */
public final class AboutDialog {

    private static final double ICON_SIZE = 72;

    private AboutDialog() {
    }

    /** Shows the dialog modally over {@code owner} and returns once it is dismissed. */
    public static void show(Window owner, AppInfo info, Consumer<String> browse) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About " + (info.name().isEmpty() ? "OAS Buddy" : info.name()));
        // The content carries its own heading and icon, so the stock ones would only repeat it.
        alert.setHeaderText(null);
        alert.setGraphic(null);
        alert.getDialogPane().setContent(content(info, browse));
        alert.initOwner(owner);
        alert.showAndWait();
    }

    /** The dialog body, built separately from showing it so it can be inspected. */
    public static Node content(AppInfo info, Consumer<String> browse) {
        StackPane icon = new StackPane(AppIcon.vector(ICON_SIZE));
        icon.setId("about-icon");
        icon.setMinSize(ICON_SIZE, ICON_SIZE);
        icon.setPrefSize(ICON_SIZE, ICON_SIZE);
        icon.setMaxSize(ICON_SIZE, ICON_SIZE);
        icon.setAlignment(Pos.TOP_CENTER);

        VBox details = new VBox(6);
        add(details, label(info.name(), "about-name", Styles.TITLE_3));
        add(details, label(info.description(), "about-description", Styles.TEXT_MUTED));
        add(details, label(info.version(), "about-version", Styles.TEXT_MUTED, Styles.TEXT_SMALL));
        add(details, label(info.copyright(), "about-copyright", Styles.TEXT_SMALL));
        add(details, licence(info, browse));
        add(details, credits(info, browse));
        add(details, links(info, browse));

        HBox body = new HBox(18, icon, details);
        body.setAlignment(Pos.TOP_LEFT);
        return FormFields.root(body);
    }

    private static Node licence(AppInfo info, Consumer<String> browse) {
        if (info.licenseName().isEmpty()) {
            return null;
        }
        Label prefix = new Label("Licensed under");
        prefix.getStyleClass().add(Styles.TEXT_SMALL);
        Node licence = info.licenseUrl().isEmpty()
                ? label(info.licenseName(), "about-license", Styles.TEXT_SMALL)
                : link(info.licenseName(), "about-link-license", info.licenseUrl(), browse);
        return new TextFlow(prefix, new Label(" "), licence);
    }

    /**
     * The people named in the POM, then a link to the full contributor graph — the POM list credits
     * who should be credited by name, the link is the part that never goes stale.
     */
    private static Node credits(AppInfo info, Consumer<String> browse) {
        List<Node> parts = new ArrayList<>();
        if (!info.developers().isEmpty()) {
            Label prefix = new Label("Created by ");
            prefix.getStyleClass().add(Styles.TEXT_SMALL);
            parts.add(prefix);
            for (int index = 0; index < info.developers().size(); index++) {
                AppInfo.Person person = info.developers().get(index);
                if (index > 0) {
                    parts.add(new Label(", "));
                }
                String id = "about-developer-" + index;
                parts.add(person.url().isEmpty()
                        ? label(person.name(), id, Styles.TEXT_SMALL)
                        : link(person.name(), id, person.url(), browse));
            }
        }
        if (!info.contributorsUrl().isEmpty()) {
            if (!parts.isEmpty()) {
                parts.add(new Label(" · "));
            }
            parts.add(link("All contributors", "about-link-contributors", info.contributorsUrl(), browse));
        }
        return parts.isEmpty() ? null : new TextFlow(parts.toArray(Node[]::new));
    }

    private static Node links(AppInfo info, Consumer<String> browse) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        if (!info.url().isEmpty()) {
            row.getChildren().add(link("GitHub repository", "about-link-repository", info.url(), browse));
        }
        if (!info.readmeUrl().isEmpty()) {
            row.getChildren().add(link("README", "about-link-readme", info.readmeUrl(), browse));
        }
        return row.getChildren().isEmpty() ? null : row;
    }

    private static Hyperlink link(String text, String id, String url, Consumer<String> browse) {
        Hyperlink hyperlink = new Hyperlink(text);
        hyperlink.setId(id);
        hyperlink.setOnAction(e -> browse.accept(url));
        return hyperlink;
    }

    /** Null for an empty value, so {@link #add} can drop the whole line. */
    private static Label label(String text, String id, String... styleClasses) {
        if (text.isEmpty()) {
            return null;
        }
        Label label = new Label(text);
        label.setId(id);
        label.getStyleClass().addAll(styleClasses);
        label.setWrapText(true);
        return label;
    }

    private static void add(VBox box, Node node) {
        if (node != null) {
            box.getChildren().add(node);
        }
    }
}
