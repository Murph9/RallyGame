package rallygame.world.path;

import java.util.function.BiFunction;

import com.jme3.math.Vector3f;

public class RoadCrossSection {

    public static BiFunction<Vector3f, Vector3f, Vector3f[]> Flat(float width) {
        final float w = width/2f;
        return (Vector3f mid, Vector3f normal) -> {
            return new Vector3f[] { 
                    mid.add(normal.mult(w)),
                    mid.add(normal.mult(-w))
                };
        };
    }

    public static BiFunction<Vector3f, Vector3f, Vector3f[]> WithEdges(float width) {
        final float w = width / 2f;
        return (Vector3f mid, Vector3f normal) -> {
            return new Vector3f[] { 
                    mid.add(normal.mult(w + 1)).add(0, -.5f, 0),
                    mid.add(normal.mult(w)),
                    mid.add(normal.mult(-w)),
                    mid.add(normal.mult(-w - 1)).add(0, -.5f, 0)
                };
        };
    }

    public final int count;
    public final BiFunction<Vector3f, Vector3f, Vector3f[]> CurveFunction;

    public RoadCrossSection(float width) {
        this(Flat(width));
    }
    
    public RoadCrossSection(BiFunction<Vector3f, Vector3f, Vector3f[]> func) {
        CurveFunction = func;
        count = CurveFunction.apply(new Vector3f(1, 0, 0), new Vector3f(0, 1, 0)).length;
    }
}
