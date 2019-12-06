package effects;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;

public class MaterialColourer {
    
    // geometry format required: <blah blah>[<colour>]
    private static Pattern GEO_NAME_REGEX = Pattern.compile(".*\\[(.+)\\].*");

    public static ColorRGBA getColorFromMaterialName(Material m) {
        if (m == null)
            return null;

        String name = m.getName();
        if (name == null)
            return null;

        Matcher mat = GEO_NAME_REGEX.matcher(name);
        if (!mat.find())
            return null;
        String colour = mat.group(1);

        if (colour.startsWith("#")) {
            return parseAsHex(colour);
        }
        return null;
    }

    private static ColorRGBA parseAsHex(String hex) {
        try {
            java.awt.Color r = java.awt.Color.decode(hex); // = me being lazy, because it does hex conversion
            return new ColorRGBA().fromIntARGB(r.getRGB());
        } catch (Exception e) {
            return null;
        }
    }
}