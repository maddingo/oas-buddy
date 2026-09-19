package no.maddin.oasbuddy.desktop;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Renders the icon through the same path the running app uses.
 *
 * <p>Extends {@link ApplicationTest} purely to get a booted JavaFX toolkit — snapshotting needs the
 * application thread. The colour assertions are the point: an SVG renderer that quietly ignores
 * stroked paths still produces a plausible-looking badge with the circles on it, so "the image is
 * not blank" is not enough to prove the icon actually drew.
 */
class AppIconRenderTest extends ApplicationTest {

    private static final Color BADGE = Color.web("#0F243F");
    private static final Color MARK = Color.web("#2DD4BF");
    private static final Color NODES = Color.web("#7DD3FC");

    @Override
    public void start(Stage stage) {
        // no UI needed; the toolkit is what matters
    }

    @Test
    void rendersAtAnySizeBecauseItIsVector() {
        for (int size : new int[]{16, 32, 256, 512, 1024}) {
            Image image = image(size);
            assertEquals(size, (int) image.getWidth(), "width at " + size);
            assertEquals(size, (int) image.getHeight(), "height at " + size);
        }
    }

    @Test
    void drawsTheBraceAndTreeStrokesNotJustTheBadge() {
        Image image = image(512);

        assertTrue(contains(image, MARK), "the teal brace stroke is missing");
        assertTrue(contains(image, NODES), "the sky-blue tree strokes are missing");
        assertTrue(contains(image, BADGE), "the navy badge is missing");
    }

    /**
     * Regression guard for a silent failure that no amount of pixel-checking would catch: the GTK
     * glass backend does not forward a snapshot-produced {@link javafx.scene.image.WritableImage}
     * to the window manager, so a stage whose icons are raw snapshots keeps the default Java icon
     * (X11 {@code _NET_WM_ICON} is never set) even though {@code getIcons()} looks correct.
     * {@link AppIcon#image} therefore re-decodes from PNG; "simplifying" that away would break the
     * window icon while every other assertion here still passed.
     */
    @Test
    void imagesAreDecodedNotRawSnapshots() {
        Image image = image(64);
        assertFalse(image instanceof javafx.scene.image.WritableImage,
                "icon must be re-decoded from PNG, or the window manager ignores it");
    }

    @Test
    void badgeCornersAreRounded() {
        Image image = image(512);
        // A rounded badge leaves the extreme corner transparent; a plain square would not.
        assertTrue(image.getPixelReader().getColor(1, 1).getOpacity() < 0.1, "top-left corner is opaque");
        assertFalse(image.getPixelReader().getColor(256, 256).getOpacity() < 0.1, "centre is transparent");
    }

    private Image image(int size) {
        Image[] holder = new Image[1];
        interact(() -> holder[0] = AppIcon.image(size));
        return holder[0];
    }

    private static boolean contains(Image image, Color wanted) {
        PixelReader reader = image.getPixelReader();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (close(reader.getColor(x, y), wanted)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Anti-aliasing means an exact match is too strict; this only needs to identify the colour. */
    private static boolean close(Color actual, Color wanted) {
        return actual.getOpacity() > 0.9
                && Math.abs(actual.getRed() - wanted.getRed()) < 0.04
                && Math.abs(actual.getGreen() - wanted.getGreen()) < 0.04
                && Math.abs(actual.getBlue() - wanted.getBlue()) < 0.04;
    }
}
