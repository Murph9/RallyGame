package effects;

import java.util.List;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import helper.Geo;
import helper.Log;

public class LoadModelWrapper {
    /**Loads a model with geometry with its given color
     * color == null means use the material color
     */
    public static Node create(AssetManager am, String model, ColorRGBA color) {
        return create(am, am.loadModel(model), color);
    }

    /**Loads a model with geometry with its given color.
     * color == null means use the material color
     */
    public static Node create(AssetManager am, Spatial s, ColorRGBA color) {
        if (s == null)
            return null;
        
        if (s instanceof Geometry) {
            Node n = new Node();
            n.attachChild(setMatColor(am, (Geometry)s, color));
            return n;
        }
        Node n = (Node)s;
        List<Geometry> gList = Geo.getGeomList(n);
        for (Geometry g: gList) {
            g.getParent().attachChild(setMatColor(am, g, color));
        }
        return n;
    }

    private static Geometry setMatColor(AssetManager am, Geometry g, ColorRGBA color) {
        if (color == null) {
            color = MaterialColourer.getColorFromMaterialName(g.getMaterial());
            if (color == null) {
                Log.e("!Material for geom:", g.getName(), "doesn't have a colour set, but was requested!");
                color = new ColorRGBA(0, 0.354f, 1, 0.7f); //orange but transparent, hopefully i will remember this
            }
        }
        
        Material baseMat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        baseMat.setColor("Color", color);
        if (color.a < 1) {
            //needs alpha stuff
            baseMat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Back);
            baseMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
            g.setQueueBucket(Bucket.Transparent);
        } else {
            baseMat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        }
        g.setMaterial(baseMat);
        return g;
    }

    public static void updateColour(Spatial s, ColorRGBA colour, boolean recursive) {
        if (colour == null)
            throw new IllegalArgumentException("Please don't send me a null colour");
        if (recursive)
            throw new IllegalArgumentException("Recursive mode not written yet."); // TODO set colour recursive

        if (s instanceof Geometry) {
            Geometry g = (Geometry)s;
            g.getMaterial().setColor("Color", colour);
        }
        if (s instanceof Node) {
            Node n = (Node) s;
            for (Spatial sp: n.getChildren()) {
                if (sp instanceof Geometry) {
                    Geometry g = (Geometry) sp;
                    g.getMaterial().setColor("Color", colour);
                }
            }
        }
    }
}
