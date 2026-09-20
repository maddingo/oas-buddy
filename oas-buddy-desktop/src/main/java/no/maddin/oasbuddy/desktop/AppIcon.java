package no.maddin.oasbuddy.desktop;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.transform.Scale;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.imageio.ImageIO;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * The application icon, rendered as live vector rather than loaded from a bitmap.
 *
 * <p>JavaFX cannot load SVG ({@link Image} handles PNG/JPEG/GIF/BMP only), so the usual approach is
 * to ship a pile of pre-rendered PNGs. Instead this parses the master SVG — the same file the
 * README and the installers use — into a JavaFX scene graph and snapshots it at whatever size is
 * asked for. That keeps the icon crisp at any DPI and, more importantly, leaves exactly one copy
 * of the geometry in the project: copying the path data into Java constants would have created a
 * second source that silently drifts from the first.
 *
 * <p>The parser deliberately understands only the handful of SVG constructs the icon actually uses
 * ({@code rect}, {@code path}, {@code circle} and {@code g} for attribute inheritance). It is not a
 * general SVG implementation and is not meant to become one — see {@code docs/branding/README.md}
 * for the authoring constraint that keeps the master file within this subset.
 */
public final class AppIcon {

    /** The master icon, copied into the classpath from {@code docs/branding/} by the generator. */
    static final String RESOURCE = "/no/maddin/oasbuddy/desktop/oas-buddy-icon.svg";

    /** The master SVG's {@code viewBox} is square with this edge length. */
    private static final double CANVAS = 256;

    private AppIcon() {
    }

    /**
     * Builds the icon as a scene graph in the master SVG's own 256x256 coordinate space.
     *
     * @throws IllegalStateException if the resource is missing or cannot be parsed
     */
    public static Group vector() {
        try (InputStream in = AppIcon.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("icon resource not on the classpath: " + RESOURCE);
            }
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Element svg = factory.newDocumentBuilder().parse(in).getDocumentElement();

            Group group = new Group();
            appendChildren(svg, new Style(), group.getChildren());
            return group;
        } catch (Exception e) {
            throw new IllegalStateException("could not build the app icon from " + RESOURCE, e);
        }
    }

    /**
     * The icon as live vector, sized to the given edge length in pixels.
     *
     * <p>For use inside the scene graph — an About dialog, a toolbar — where none of the rasterising
     * {@link #image(int)} does is needed, and where staying vector means the icon is resolved by the
     * same renderer that draws the rest of the window.
     */
    public static Group vector(double edgeLength) {
        Group scaled = vector();
        double scale = edgeLength / CANVAS;
        scaled.getTransforms().add(new Scale(scale, scale));
        // Wrapped, because a node's own transforms do not count towards its layout bounds: without
        // the wrapper this would still measure 256 to a layout parent and blow the dialog open.
        return new Group(scaled);
    }

    /**
     * Renders the icon at the given edge length in pixels.
     *
     * <p>Must be called on the JavaFX application thread.
     *
     * <p>The result is deliberately re-decoded from PNG bytes rather than handed back as the raw
     * snapshot. A {@link WritableImage} produced by {@code snapshot()} works fine everywhere inside
     * the scene graph, but the GTK glass backend does <em>not</em> forward one to the window
     * manager: put snapshots in {@code Stage.getIcons()} and the X11 {@code _NET_WM_ICON} property
     * is never set, so the window and taskbar silently keep the default Java icon. Round-tripping
     * through PNG yields a decoded image, which does propagate. Verified by comparing the two
     * side by side with {@code xprop _NET_WM_ICON} on an otherwise identical stage.
     */
    public static Image image(int size) {
        return decode(toPng(snapshot(size), size));
    }

    private static WritableImage snapshot(int size) {
        Group group = vector();
        // A snapshot needs the node in a scene for its transforms to resolve predictably.
        Scene scene = new Scene(group);
        scene.setFill(Color.TRANSPARENT);

        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        double scale = size / CANVAS;
        params.setTransform(new Scale(scale, scale));

        return group.snapshot(params, new WritableImage(size, size));
    }

    private static byte[] toPng(WritableImage rendered, int size) {
        // Deliberately hand-rolled rather than via SwingFXUtils, which lives in the javafx.swing
        // module the desktop app does not otherwise depend on. java.desktop is part of the JDK.
        BufferedImage buffered = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        PixelReader pixels = rendered.getPixelReader();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                buffered.setRGB(x, y, pixels.getArgb(x, y));
            }
        }
        try {
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(buffered, "png", png);
            return png.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("could not encode the app icon at " + size + "px", e);
        }
    }

    private static Image decode(byte[] png) {
        return new Image(new ByteArrayInputStream(png));
    }

    /**
     * Renders the icon at each of the given sizes, for {@code Stage.getIcons()}.
     *
     * <p>A missing or broken icon must never stop the editor from starting, so this returns
     * whatever it managed to render — possibly nothing — instead of propagating a failure.
     */
    public static List<Image> icons(int... sizes) {
        List<Image> images = new ArrayList<>(sizes.length);
        try {
            for (int size : sizes) {
                images.add(image(size));
            }
        } catch (RuntimeException e) {
            System.getLogger(AppIcon.class.getName())
                    .log(System.Logger.Level.WARNING, "falling back to the default window icon", e);
        }
        return images;
    }

    private static void appendChildren(Element parent, Style inherited, List<javafx.scene.Node> out) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (!(children.item(i) instanceof Element child)) {
                continue;
            }
            Style style = inherited.mergedWith(child);
            switch (child.getLocalName() == null ? child.getTagName() : child.getLocalName()) {
                case "g" -> appendChildren(child, style, out);
                case "rect" -> out.add(style.applyTo(rect(child)));
                case "circle" -> out.add(style.applyTo(circle(child)));
                case "path" -> out.add(style.applyTo(path(child)));
                default -> {
                    // <title> and anything else the icon doesn't draw with
                }
            }
        }
    }

    private static Shape rect(Element e) {
        Rectangle rect = new Rectangle(num(e, "x", 0), num(e, "y", 0), num(e, "width", 0), num(e, "height", 0));
        // SVG rx/ry are radii; the JavaFX equivalents are diameters.
        double rx = num(e, "rx", 0);
        rect.setArcWidth(rx * 2);
        rect.setArcHeight(num(e, "ry", rx) * 2);
        return rect;
    }

    private static Shape circle(Element e) {
        return new Circle(num(e, "cx", 0), num(e, "cy", 0), num(e, "r", 0));
    }

    private static Shape path(Element e) {
        SVGPath svgPath = new SVGPath();
        svgPath.setContent(e.getAttribute("d"));
        return svgPath;
    }

    private static double num(Element e, String name, double fallback) {
        String value = e.getAttribute(name);
        return value.isEmpty() ? fallback : Double.parseDouble(value.trim());
    }

    /** The presentation attributes the icon uses, resolved down the {@code <g>} hierarchy. */
    private record Style(String fill, String stroke, String strokeWidth, String cap, String join) {

        Style() {
            this(null, null, null, null, null);
        }

        Style mergedWith(Element e) {
            return new Style(
                    override(fill, e, "fill"),
                    override(stroke, e, "stroke"),
                    override(strokeWidth, e, "stroke-width"),
                    override(cap, e, "stroke-linecap"),
                    override(join, e, "stroke-linejoin"));
        }

        private static String override(String current, Element e, String attribute) {
            String value = e.getAttribute(attribute);
            return value.isEmpty() ? current : value.trim();
        }

        Shape applyTo(Shape shape) {
            // An unset fill defaults to black in SVG, which is never what this icon wants.
            shape.setFill(paint(fill));
            shape.setStroke(paint(stroke));
            if (strokeWidth != null) {
                shape.setStrokeWidth(Double.parseDouble(strokeWidth));
            }
            if ("round".equals(cap)) {
                shape.setStrokeLineCap(StrokeLineCap.ROUND);
            }
            if ("round".equals(join)) {
                shape.setStrokeLineJoin(StrokeLineJoin.ROUND);
            }
            return shape;
        }

        private static Color paint(String value) {
            return value == null || "none".equals(value) ? null : Color.web(value);
        }
    }
}
