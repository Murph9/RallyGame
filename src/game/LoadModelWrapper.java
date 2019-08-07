package game;

import java.util.List;

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
    public static Spatial create(AssetManager am, String model, ColorRGBA color) {
		return create(am, am.loadModel(model), color);
	}

    public static Spatial create(AssetManager am, Spatial s, ColorRGBA color) {
		if (s == null)
			return s;
		
		if (s instanceof Geometry) {
			Node n = new Node();
			n.attachChild(_do(am, (Geometry)s, color));
			return s;
		}
		Node n = (Node)s;
		List<Geometry> gList = H.getGeomList(n);
		for (Geometry g: gList) {
			g.getParent().attachChild(_do(am, g, color));
		}
		return n;
    }

    private static Geometry _do(AssetManager am, Geometry g, ColorRGBA color) {
		ColorRGBA defColour = H.getColorFromMaterialName(g.getMaterial());
		if (defColour != null)
			color = defColour;
		else
			Log.e("!!Material in geom:", g.getName(), "doesn't have a colour set! Using the default given: " + color);
		
		Material baseMat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		baseMat.setColor("Color", color);
		baseMat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		g.setMaterial(baseMat);
		
		return g;
	}
}