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

import car.data.CarModelData;
import helper.Geo;
import helper.Log;

public class LoadModelWrapper {
    /**Loads a model with geometry with its given color
     * Colour == null means use the material color
     */
    public static Node create(AssetManager am, String model, ColorRGBA colour) {
        return create(am, am.loadModel(model), colour);
    }

    /**Loads a model with geometry with its given color.
     * Colour == null means use the material color
     */
    public static Node create(AssetManager am, Spatial s, ColorRGBA colour) {
        if (s == null)
            return null;

        //TODO this should eventually only set the primary/secondary colours of the model and warn about the other pieces
        
        if (s instanceof Geometry) {
            Node n = new Node();
            n.attachChild(setMatColour(am, (Geometry)s, colour));
            return n;
        }
        Node n = (Node)s;
        List<Geometry> gList = Geo.getGeomList(n);
        for (Geometry g: gList) {
            g.getParent().attachChild(setMatColour(am, g, colour));
        }
        return n;
    }

    private static Geometry setMatColour(AssetManager am, Geometry g, ColorRGBA colour) {
        if (colour == null) {
            colour = MaterialColourer.getColourFromMaterialName(g.getMaterial());
            if (colour == null) {
                Log.e("!Material for geom:", g.getName(), "doesn't have a colour set, but was requested!");
                colour = new ColorRGBA(0, 0.354f, 1, 0.7f); //orange but transparent, hopefully i will remember this
            }
        }
        
        Material baseMat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        baseMat.setColor("Color", colour);
        if (colour.a < 1) {
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

    /** Sets the colour of the primary tagged part */
    public static void setPrimaryColour(Spatial s, ColorRGBA colour) {
        setColour(s, colour, CarModelData.CarPart.PRIMARY_TAG);
    }
    
    /** Sets the colour of the secondary tagged part */
    public static void setSecondaryColour(Spatial s, ColorRGBA colour) {
        setColour(s, colour, CarModelData.CarPart.SECONDARY_TAG);
    }

    private static void setColour(Spatial s, ColorRGBA colour, String tag) {
        if (colour == null)
            throw new IllegalArgumentException("Please don't send me a null colour");

        List<Geometry> geoms = Geo.getGeomsContaining(s, tag);
        if (geoms.isEmpty()) {
            Log.e("No part tagged with " + tag + "found for " + s.getName());
            return;
        }

        for (Geometry g : geoms) {
            g.getMaterial().setColor("Color", colour);
        }
    }
}
