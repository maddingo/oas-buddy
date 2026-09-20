package no.maddin.oasbuddy.desktop.pane;

import no.maddin.oasbuddy.desktop.AppInfo;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The About dialog's content, inspected without showing the dialog itself — the same split
 * {@link RemovalConfirmation} uses, so nothing here has to drive a modal window.
 *
 * <p>Links are handed to a collaborator rather than opened, because the running app opens them
 * through {@code HostServices}, which does not exist in a test.
 */
class AboutDialogTest extends ApplicationTest {

    private static final AppInfo COMPLETE = new AppInfo(
            "OAS Buddy", "1.2.0", "An OpenAPI Specification editor.",
            "https://github.com/maddingo/oas-buddy",
            "https://github.com/maddingo/oas-buddy#readme",
            "https://github.com/maddingo/oas-buddy/graphs/contributors",
            "Copyright © 2026 OAS Buddy contributors",
            "Apache License, Version 2.0", "https://www.apache.org/licenses/LICENSE-2.0.txt",
            List.of(new AppInfo.Person("Martin Goldhahn", "https://github.com/maddingo"),
                    new AppInfo.Person("A Nameless Helper", "")));

    /** What an unfiltered classpath leaves AppInfo with: a fallback version and nothing else. */
    private static final AppInfo UNRESOLVED = new AppInfo(
            "", AppInfo.UNKNOWN_VERSION, "", "", "", "", "", "", "", List.of());

    private final List<String> opened = new ArrayList<>();
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setScene(new Scene(new StackPane(), 600, 400));
        stage.show();
    }

    private void showContent(AppInfo info) {
        interact(() -> ((StackPane) stage.getScene().getRoot())
                .getChildren().setAll(AboutDialog.content(info, opened::add)));
    }

    @Test
    void theNameVersionAndCopyrightComeStraightFromAppInfo() {
        showContent(COMPLETE);

        assertAll(
                () -> assertEquals("OAS Buddy", text("#about-name")),
                () -> assertEquals("1.2.0", text("#about-version")),
                () -> assertEquals("An OpenAPI Specification editor.", text("#about-description")),
                () -> assertEquals("Copyright © 2026 OAS Buddy contributors", text("#about-copyright")),
                () -> assertTrue(lookup("#about-icon").tryQuery().isPresent(), "no app icon in the dialog"));
    }

    @Test
    void everyLinkHandsItsUrlToTheBrowser() {
        showContent(COMPLETE);

        interact(() -> {
            link("#about-link-repository").fire();
            link("#about-link-readme").fire();
            link("#about-link-license").fire();
            link("#about-link-contributors").fire();
        });

        assertEquals(List.of(
                "https://github.com/maddingo/oas-buddy",
                "https://github.com/maddingo/oas-buddy#readme",
                "https://www.apache.org/licenses/LICENSE-2.0.txt",
                "https://github.com/maddingo/oas-buddy/graphs/contributors"), opened);
    }

    @Test
    void theLicenceIsNamedByTheLinkThatOpensIt() {
        showContent(COMPLETE);

        assertEquals("Apache License, Version 2.0", link("#about-link-license").getText());
    }

    @Test
    void aDeveloperWithAUrlIsALinkAndOneWithoutIsPlainText() {
        showContent(COMPLETE);

        interact(() -> link("#about-developer-0").fire());

        assertAll(
                () -> assertEquals("Martin Goldhahn", link("#about-developer-0").getText()),
                () -> assertEquals(List.of("https://github.com/maddingo"), opened),
                () -> assertEquals("A Nameless Helper", text("#about-developer-1")),
                () -> assertTrue(node("#about-developer-1") instanceof Label,
                        "a developer without a url should not look clickable"));
    }

    /**
     * End to end: POM -> resource filtering -> AppInfo -> the label the user reads. The two halves
     * are covered separately above and in {@code AppInfoTest}; this one exists because the seam
     * between them is exactly where a hardcoded version string would hide.
     */
    @Test
    void theVersionOnScreenIsTheOneThisBuildWasGiven() {
        showContent(AppInfo.load());

        assertEquals(System.getProperty("project.version"), text("#about-version"));
    }

    @Test
    void whatTheBuildNeverResolvedIsLeftOutRatherThanShownEmpty() {
        showContent(UNRESOLVED);

        assertAll(
                () -> assertEquals(AppInfo.UNKNOWN_VERSION, text("#about-version")),
                () -> assertTrue(lookup("#about-copyright").tryQuery().isEmpty(), "empty copyright shown"),
                () -> assertTrue(lookup("#about-description").tryQuery().isEmpty(), "empty description shown"),
                () -> assertTrue(lookup("#about-link-repository").tryQuery().isEmpty(), "dead repository link"),
                () -> assertTrue(lookup("#about-link-license").tryQuery().isEmpty(), "dead license link"),
                () -> assertTrue(lookup("#about-developer-0").tryQuery().isEmpty(), "credited nobody as somebody"));
    }

    private Node node(String id) {
        return lookup(id).query();
    }

    private Hyperlink link(String id) {
        return lookup(id).queryAs(Hyperlink.class);
    }

    private String text(String id) {
        return ((Label) node(id)).getText();
    }
}
