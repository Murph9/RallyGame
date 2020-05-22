package rallygame.world.path;

import java.util.function.BiFunction;

import com.jme3.math.Spline;
import com.jme3.math.Vector3f;
import rallygame.helper.Duo;

public class CatmullRomRoad {

    public static BiFunction<Vector3f, Vector3f, Duo<Vector3f, Vector3f>> Flat(float width) {
        final float w = width / 2f;
        return (Vector3f mid, Vector3f normal) -> {
            return new Duo<>(mid.add(normal.mult(w)), mid.add(normal.mult(-w)));
        };
    }
    
    public final CatmullRomWidth middle;
    public final CatmullRomWidth[] others;
    
    public CatmullRomRoad(Spline spline, int nbSubSegments, float width) {
        this.middle = new CatmullRomWidth(spline, nbSubSegments, width);
        this.others = new CatmullRomWidth[1];
        others[0] = new CatmullRomWidth(spline, nbSubSegments, new RoadCrossSection(RoadCrossSection.WithEdges(width)));
    }
}
