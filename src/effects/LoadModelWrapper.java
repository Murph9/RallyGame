package effects;

import java.awt.Color;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import helper.H;
import helper.Log;

public class LoadModelWrapper {
    public static Node create(AssetManager am, String model, ColorRGBA color) {
		return create(am, am.loadModel(model), color);
	}

    public static Node create(AssetManager am, Spatial s, ColorRGBA color) {
		if (s == null)
			return null;
		
		if (s instanceof Geometry) {
			Node n = new Node();
			n.attachChild(_do(am, (Geometry)s, color));
			return n;
		}
		Node n = (Node)s;
		List<Geometry> gList = H.getGeomList(n);
		for (Geometry g: gList) {
			g.getParent().attachChild(_do(am, g, color));
		}
		return n;
    }

    private static Geometry _do(AssetManager am, Geometry g, ColorRGBA color) {
		ColorRGBA defColour = getColorFromMaterialName(g.getMaterial());
		if (defColour != null)
			color = defColour;
		else
			Log.e("!Material in geom:", g.getName(), "doesn't have a colour set! Using the default given: " + color);
		
		Material baseMat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		baseMat.setColor("Color", color);
		baseMat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		g.setMaterial(baseMat);
		
		return g;
	}

	// geometry format required: <blah blah>[<colour>]
	private static Pattern GEO_NAME_REGEX = Pattern.compile(".*\\[(.+)\\].*");

	public static ColorRGBA getColorFromMaterialName(Material m) {
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
			Color r = Color.decode(hex); // = me being lazy
			return new ColorRGBA().fromIntARGB(r.getRGB()); // probably
		} catch (Exception e) {
			return null;
		}
	}
}