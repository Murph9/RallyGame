package duel;

import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.GuiGlobals;
import com.simsilica.lemur.Insets3f;
import com.simsilica.lemur.component.QuadBackgroundComponent;
import com.simsilica.lemur.component.TbtQuadBackgroundComponent;
import com.simsilica.lemur.style.Attributes;
import com.simsilica.lemur.style.Styles;

public class DuelUiStyle {
    // https://hub.jmonkeyengine.org/t/many-little-lemur-questions/40244/14

    private static final String STYLE_NAME = "duel_style";
    private static ColorRGBA dialogBackground = new ColorRGBA(0.8f, 0.8f, 0.8f, 1);
    private static ColorRGBA altBackground = new ColorRGBA(0.5f, 0.5f, 0.5f, 0.85f);

    private static ColorRGBA color_1 = new ColorRGBA(0.031f, 0.969f, 0.996f, 1);
    private static ColorRGBA color_2 = new ColorRGBA(0.035f, 0.984f, 0.827f, 1);
    private static ColorRGBA color_3 = new ColorRGBA(0.996f, 0.325f, 0.733f, 1);
    private static ColorRGBA color_4 = new ColorRGBA(0.961f, 0.827f, 0.0f, 1);

    private static float TEXT_TO_BUFFER_RATIO = 0.2f;
    private static int TEXT_SIZE = 20;
    private static Insets3f INSETS_BASE_2 = new Insets3f(TEXT_SIZE * TEXT_TO_BUFFER_RATIO,
            TEXT_SIZE * TEXT_TO_BUFFER_RATIO * 2, TEXT_SIZE * TEXT_TO_BUFFER_RATIO, TEXT_SIZE * TEXT_TO_BUFFER_RATIO * 2);
    private static Insets3f INSETS_BASE = new Insets3f(TEXT_SIZE * TEXT_TO_BUFFER_RATIO / 2,
            TEXT_SIZE * TEXT_TO_BUFFER_RATIO, TEXT_SIZE * TEXT_TO_BUFFER_RATIO / 2, TEXT_SIZE * TEXT_TO_BUFFER_RATIO);

    public static void load(AssetManager assetManager) {
        Styles styles = GuiGlobals.getInstance().getStyles();
        styles.setDefault(GuiGlobals.getInstance().loadFont("assets/nasalization.fnt"));
        //to fix the font, use http://kvazars.com/littera/

        Attributes attrs;

        QuadBackgroundComponent bg = new QuadBackgroundComponent(altBackground);

        TbtQuadBackgroundComponent flat = TbtQuadBackgroundComponent.create("assets/image/solid-white.png", 1, 1, 1,
                2, 2, 1f, false);
        flat.setColor(dialogBackground);

        TbtQuadBackgroundComponent double_gradient = TbtQuadBackgroundComponent
                .create("assets/image/double-gradient-128.png", 1, 1, 1, 126, 126, 1f, false);
        double_gradient.setColor(color_4);

        attrs = styles.getSelector(STYLE_NAME);
        attrs.set("fontSize", TEXT_SIZE);

        // label
        attrs = styles.getSelector("label", STYLE_NAME);
        attrs.set("insets", INSETS_BASE);
        attrs.set("color", color_3);

        // title
        attrs = styles.getSelector("title", STYLE_NAME);
        attrs.set("fontSize", 32);
        attrs.set("color", color_1);
        attrs.set("highlightColor", color_3);
        attrs.set("shadowColor", altBackground);
        attrs.set("shadowOffset", new Vector3f(1, -1, -1));
        attrs.set("background", double_gradient.clone());
        attrs.set("insets", INSETS_BASE);

        // button
        attrs = styles.getSelector("button", STYLE_NAME);
        attrs.set("color", color_1);
        attrs.set("background", flat.clone());
        ((TbtQuadBackgroundComponent) attrs.get("background")).setColor(altBackground);
        attrs.set("insets", INSETS_BASE);

        // textField
        attrs = styles.getSelector("textField", STYLE_NAME);
        attrs.set("background", flat.clone());
        ((TbtQuadBackgroundComponent) attrs.get("background")).setColor(altBackground);
        attrs.set("insets", INSETS_BASE);

        // container
        attrs = styles.getSelector("container", STYLE_NAME);
        attrs.set("background", flat.clone());
        attrs.set("insets", INSETS_BASE_2);

        // slider
        attrs = styles.getSelector("slider", STYLE_NAME);
        attrs.set("insets", new Insets3f(2, 2, 2, 2));
        attrs.set("background", bg.clone());
        ((QuadBackgroundComponent) attrs.get("background")).setColor(altBackground);

        attrs = styles.getSelector("slider", "button", STYLE_NAME);
        attrs.set("background", bg.clone());
        ((QuadBackgroundComponent) attrs.get("background")).setColor(altBackground);
        attrs.set("insets", new Insets3f(0, 0, 0, 0));

        attrs = styles.getSelector("slider", "slider.thumb.button", STYLE_NAME);
        attrs.set("text", "[]");
        attrs.set("color", color_2);

        attrs = styles.getSelector("slider", "slider.left.button", STYLE_NAME);
        attrs.set("text", "-");
        attrs.set("background", bg.clone());
        ((QuadBackgroundComponent) attrs.get("background")).setColor(altBackground);
        ((QuadBackgroundComponent) attrs.get("background")).setMargin(5, 0);
        attrs.set("color", color_2);

        attrs = styles.getSelector("slider", "slider.right.button", STYLE_NAME);
        attrs.set("text", "+");
        attrs.set("background", bg.clone());
        ((QuadBackgroundComponent) attrs.get("background")).setColor(altBackground);
        ((QuadBackgroundComponent) attrs.get("background")).setMargin(4, 0);
        attrs.set("color", color_2);

        // Tabbed Panel
        attrs = styles.getSelector("tabbedPanel", STYLE_NAME);
        attrs.set("activationColor", color_2);

        attrs = styles.getSelector("tabbedPanel.container", STYLE_NAME);
        attrs.set("background", null);

        attrs = styles.getSelector("tab.button", STYLE_NAME);
        attrs.set("background", flat.clone());
        ((TbtQuadBackgroundComponent) attrs.get("background")).setColor(altBackground);
        ((TbtQuadBackgroundComponent) attrs.get("background")).setMargin(12, 6);
        attrs.set("color", color_2);
        attrs.set("insets", new Insets3f(12, 6, 0, 6));

        // Set this as the default style
        GuiGlobals.getInstance().getStyles().setDefaultStyle(STYLE_NAME);
    }

}