package rallygame.effects;

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

import rallygame.car.data.CarModelData;
import rallygame.helper.Geo;
import rallygame.helper.Log;

public class LoadModelWrapper {
    
    /**Loads a model with geometry with its own colours */
    public static Node create(AssetManager am, String model) {
        return create(am, am.loadModel(model), null);
    }

    /**Loads a model with geometry with a primaryColor, a null colour means it needs its own colour*/
    public static Node create(AssetManager am, Spatial s, ColorRGBA primaryColor) {
        if (s == null)
            return null;

        if (s instanceof Geometry) {
            Node n = new Node();
            n.attachChild(setMatColour(am, (Geometry) s, primaryColor));
            return n;
        }
        Node n = (Node) s;
        List<Geometry> gList = Geo.getGeomList(n);
        for (Geometry g : gList) {
            g.getParent().attachChild(setMatColour(am, g, primaryColor));
        }
        return n;
    }

    private static Geometry setMatColour(AssetManager am, Geometry g, ColorRGBA primaryColor) {
        Material mat = g.getMaterial();
        if (primaryColor == null) {
            primaryColor = MaterialColourer.getColourFromMaterialName(mat);
            if (primaryColor == null) {
                throw new IllegalArgumentException("Material " + mat.getName() + " for geom: " + g.getName()
                        + " doesn't have a colour set and i wasn't given one");
            }
        }

        Material baseMat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        baseMat.setColor("Color", primaryColor);
        if (mat != null) // keep the name if given
            baseMat.setName(mat.getName());
        if (primaryColor.a < 1) {
            // needs alpha stuff
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

        List<Geometry> geoms = Geo.getGeomsWithMaterialNameContaining(s, tag);
        if (geoms.isEmpty()) {
            Log.e("No part tagged with " + tag + "found for " + s.getName());
            return;
        }

        for (Geometry g : geoms) {
            g.getMaterial().setColor("Color", colour);
        }
    }
}
