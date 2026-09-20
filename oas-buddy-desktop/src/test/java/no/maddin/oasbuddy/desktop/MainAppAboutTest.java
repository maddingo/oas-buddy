package no.maddin.oasbuddy.desktop;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The Help menu's wiring. Actually showing the dialog is not exercised here — it is modal, and
 * TestFX cannot dismiss it — so the menu item is checked against an injected action, the same way
 * {@link MainAppRemovalTest} injects its confirmation. The dialog's own contents are covered by
 * {@code AboutDialogTest}.
 */
class MainAppAboutTest extends ApplicationTest {

    private MainApp app;

    @Override
    public void start(Stage stage) {
        app = new MainApp();
        app.start(stage);
    }

    @Test
    void theHelpMenuOffersAboutAndItOpensTheDialog() {
        List<String> opened = new ArrayList<>();
        app.setAboutAction(() -> opened.add("about"));

        MenuItem about = menuItem("Help", "About OAS Buddy");
        interact(about::fire);

        assertEquals(List.of("about"), opened);
    }

    @Test
    void openingALinkIsANoOpWhenThereIsNoHostBrowser() {
        // MainApp is constructed directly rather than launched, so JavaFX never gave it
        // HostServices - exactly the situation a packaged app hits when the desktop has no handler.
        assertDoesNotThrow(() -> interact(() -> app.browse("https://github.com/maddingo/oas-buddy")));
    }

    private MenuItem menuItem(String menuName, String itemName) {
        MenuBar menuBar = lookup(".menu-bar").queryAs(MenuBar.class);
        Menu menu = menuBar.getMenus().stream()
                .filter(candidate -> menuName.equals(candidate.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no " + menuName + " menu"));
        return menu.getItems().stream()
                .filter(item -> itemName.equals(item.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no " + itemName + " item in " + menuName));
    }
}
