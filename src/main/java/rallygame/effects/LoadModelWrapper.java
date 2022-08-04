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
import rallygame.car.data.SurfaceType;
import rallygame.helper.Geo;
import rallygame.helper.Log;

public class LoadModelWrapper {
    
    private static float REPEATING_SIZE = 15;

    /** Loads a model with all the same colour */
    public static Node createWithColour(AssetManager am, Spatial s, ColorRGBA colour) {
        if (s == null)
            return null;

        if (s instanceof Geometry) {
            Node n = new Node();
            n.attachChild(setMatColour(am, (Geometry) s, colour));
            return n;
        }
        Node n = (Node) s;
        List<Geometry> gList = Geo.getGeomList(n);
        for (Geometry g : gList) {
            g.getParent().attachChild(setMatColour(am, g, colour));
        }
        return n;
    }
    
    /** Loads a model with geometry with its own colours */
    public static Node create(AssetManager am, String model) {
        return create(am, am.loadModel(model), null, null);
    }

    /** Loads a model with geometry with a primary colour */
    public static Node create(AssetManager am, String model, ColorRGBA primaryColor) {
        return create(am, am.loadModel(model), primaryColor, null);
    }

    /** Loads a model with geometry with a primary colour and secondary colour */
    public static Node create(AssetManager am, Spatial s, ColorRGBA primaryColor, ColorRGBA secondaryColor) {
        if (s == null)
            return null;

        if (s instanceof Geometry) {
            Node n = new Node();
            n.attachChild(setMat(am, (Geometry) s));
            return n;
        }
        Node n = (Node) s;
        List<Geometry> gList = Geo.getGeomList(n);
        for (Geometry g : gList) {
            g.getParent().attachChild(setMat(am, g));
        }

        setPrimaryColour(n, primaryColor);
        setSecondaryColour(n, secondaryColor);
        return n;
    }

    private static Geometry setMatColour(AssetManager am, Geometry g, ColorRGBA color) {
        return setMatColour(am, g, color, SurfaceType.Normal);
    }

    private static Geometry setMatColour(AssetManager am, Geometry g, ColorRGBA color, SurfaceType type) {
        if (color == null) {
            throw new IllegalArgumentException("Please don't send me a null colour");
        }

        Material baseMat = new Material(am, "MatDefs/Base.j3md");
        baseMat.setColor("Color", color);
        baseMat.setFloat("RepeatingPatternSize", REPEATING_SIZE);
        var pattern = getMaterialPatternFromType(type);
        if (pattern != null)
            baseMat.setBoolean(pattern, true);

        Material mat = g.getMaterial();
        if (mat != null) // keep the name if given
            baseMat.setName(mat.getName());
        if (color.a < 1) {
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

    private static String getMaterialPatternFromType(SurfaceType type) {
        if (type == null)
            return null;
        
        switch (type) {
            case Dirt:
                return "Checker";
            case Grass:
                return "Picnic";
            case Ice:
                return "DiagStriped";
            case None:
                return "Xd";
            case Normal:
            default:
                return null;
        }
    }

    private static Geometry setMat(AssetManager am, Geometry g) {
        Material mat = g.getMaterial();
        ColorRGBA colour = MaterialColourer.getColourFromMaterialName(mat);
        if (colour == null) {
            Log.e("Material " + mat.getName() + " for geom: " + g.getName() + " doesn't have a colour set");
            colour = new ColorRGBA(ColorRGBA.Magenta);
        }

        SurfaceType type = MaterialColourer.getSurfaceTypeFromMaterialName(mat);
        return setMatColour(am, g, colour, type);
    }

    /** Sets the colour of the primary tagged part */
    public static void setPrimaryColour(Spatial s, ColorRGBA colour) {
        if (colour != null)
            setColour(s, colour, CarModelData.CarPart.PRIMARY_TAG);
    }

    /** Sets the colour of the secondary tagged part */
    public static void setSecondaryColour(Spatial s, ColorRGBA colour) {
        if (colour != null)
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
