package no.maddin.oasbuddy.desktop;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the one value in the Linux desktop entry that duplicates a Java name.
 *
 * <p>JavaFX derives a window's {@code WM_CLASS} from the {@link javafx.application.Application}
 * subclass, and {@code StartupWMClass} in the desktop entry has to repeat it for a shell to match
 * the running window to the launcher. Nothing connects the two: renaming or moving {@link MainApp}
 * compiles, packages and starts exactly as before, and the only symptom is a taskbar entry that
 * falls back to a generic icon and window title.
 *
 * <p>The entry is packaging input rather than a classpath resource, so it is read from the source
 * tree — see {@code jpackage.resourceDir} in the POM.
 */
class LinuxDesktopEntryTest {

    private static final String RELATIVE_PATH = "src/main/jpackage/linux/oas-buddy.desktop";

    @Test
    void startupWmClassNamesTheApplicationClass() throws IOException {
        List<String> values = Files.readAllLines(desktopEntry()).stream()
                .filter(line -> line.startsWith("StartupWMClass="))
                .map(line -> line.substring("StartupWMClass=".length()))
                .toList();

        assertEquals(1, values.size(), "expected exactly one StartupWMClass in " + RELATIVE_PATH);
        assertEquals(MainApp.class.getName(), values.getFirst(),
                "StartupWMClass must match the WM_CLASS JavaFX derives from the Application subclass");
    }

    /**
     * Surefire runs with the module directory as the working directory; an IDE may use the repo
     * root instead, so both are tried before giving up.
     */
    private static Path desktopEntry() {
        Path fromModule = Path.of(RELATIVE_PATH);
        Path candidate = Files.exists(fromModule) ? fromModule : Path.of("oas-buddy-desktop", RELATIVE_PATH);
        assertTrue(Files.exists(candidate), "cannot find " + RELATIVE_PATH + " from " + Path.of("").toAbsolutePath());
        return candidate;
    }
}
