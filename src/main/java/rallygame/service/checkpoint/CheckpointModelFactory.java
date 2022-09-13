package rallygame.service.checkpoint;

import com.jme3.app.Application;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

import rallygame.effects.LoadModelWrapper;
import rallygame.helper.Geo;

public class CheckpointModelFactory {
    
    public static Spatial GetDefaultCheckpointModel(Application app, float scale) {
        return GetDefaultCheckpointModel(app, scale, new ColorRGBA(0, 0, 0, 1));
    }

    public static Spatial GetDefaultCheckpointModel(Application app, float scale, ColorRGBA colour) {
        Vector3f checkpointSize = Vector3f.UNIT_XYZ.mult(scale);
        Spatial baseSpat = new Geometry("checkpoint", new Box(checkpointSize.negate(), checkpointSize));
        Spatial out = LoadModelWrapper.createWithColour(app.getAssetManager(), baseSpat, colour);
        for (Geometry g : Geo.getGeomList(out)) {
            g.getMaterial().getAdditionalRenderState().setWireframe(true);
            g.getMaterial().getAdditionalRenderState().setLineWidth(5);
        }

        return out;
    }
}
