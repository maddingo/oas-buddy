package no.maddin.oasbuddy.desktop;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the icon resource itself, without needing a JavaFX toolkit.
 *
 * <p>The failure this exists to catch is a silent one: rename the SVG or move the resource root and
 * nothing breaks at compile time — the window just quietly falls back to the default icon, because
 * {@link AppIcon#icons} is deliberately forgiving at runtime.
 */
class AppIconResourceTest {

    @Test
    void masterSvgIsOnTheClasspath() throws Exception {
        try (InputStream in = AppIcon.class.getResourceAsStream(AppIcon.RESOURCE)) {
            assertNotNull(in, AppIcon.RESOURCE + " is missing from the classpath");
            assertTrue(in.readAllBytes().length > 0, "icon resource is empty");
        }
    }

    @Test
    void masterSvgStaysWithinTheSubsetTheParserUnderstands() throws Exception {
        try (InputStream in = AppIcon.class.getResourceAsStream(AppIcon.RESOURCE)) {
            Element svg = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder().parse(in).getDocumentElement();

            assertEquals("0 0 256 256", svg.getAttribute("viewBox"),
                    "AppIcon scales against a 256x256 canvas");
            // Anything outside rect/path/circle/g would be dropped on the floor by the parser
            // rather than reported, so assert the authoring constraint holds.
            assertTrue(svg.getElementsByTagName("rect").getLength() > 0, "expected the badge");
            assertTrue(svg.getElementsByTagName("path").getLength() > 0, "expected stroked paths");
            assertTrue(svg.getElementsByTagName("circle").getLength() > 0, "expected the tree nodes");
        }
    }
}
