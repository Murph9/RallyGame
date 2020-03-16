package rallygame.effects;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;

public class MaterialColourer {
    
    // geometry material name format: <blah blah>[<colour>] [<possibly other square bracket sections>]
    private static Pattern GEO_NAME_REGEX = Pattern.compile("\\[(#.+?)\\]");

    public static ColorRGBA getColourFromMaterialName(Material m) {
        if (m == null)
            return null;

        String name = m.getName();
        if (name == null)
            return null;

        Matcher mat = GEO_NAME_REGEX.matcher(name);
        if (!mat.find())
            return null;

        // check every matching group
        for (int i = 0; i < mat.groupCount(); i++) {
            ColorRGBA col = parseAsHex(mat.group(i + 1));
            if (col != null)
                return col;
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