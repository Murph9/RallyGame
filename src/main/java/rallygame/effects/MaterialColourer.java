package rallygame.effects;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;

import rallygame.car.data.SurfaceType;

public class MaterialColourer {
    
    // geometry material name format: <blah blah>[<colour>] [<possibly other square bracket sections>]
    private static Pattern MAT_COLOUR_NAME_REGEX = Pattern.compile("\\[(#.+?)\\]");
    private static Pattern MAT_SURFACE_NAME_REGEX = Pattern.compile("\\[(@\\w+)\\]");

    public static ColorRGBA getColourFromMaterialName(Material m) {
        if (m == null)
            return null;

        String name = m.getName();
        if (name == null)
            return null;

        Matcher mat = MAT_COLOUR_NAME_REGEX.matcher(name);
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

    public static SurfaceType getSurfaceTypeFromMaterialName(Material m) {
        if (m == null)
            return SurfaceType.Normal;

        String name = m.getName();
        if (name == null)
            return SurfaceType.Normal;

        Matcher mat = MAT_SURFACE_NAME_REGEX.matcher(name);
        if (!mat.find())
            return null;

        for (int i = 0; i < mat.groupCount(); i++) {
            var result = SurfaceType.fromString(mat.group(i + 1));
            if (result != null)
                return result;
        }
        return SurfaceType.Normal;
    }
}