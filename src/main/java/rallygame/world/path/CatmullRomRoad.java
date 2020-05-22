package rallygame.world.path;

import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;

import com.jme3.math.Spline;
import com.jme3.math.Vector3f;

public class CatmullRomRoad {

    public static BiFunction<Vector3f, Vector3f, Vector3f[]> SideA(float width) {
        final float w = width / 2f;
        return (Vector3f mid, Vector3f normal) -> {
            return new Vector3f[] { mid.add(normal.mult(w + 2)).add(0, -0.5f, 0), mid.add(normal.mult(w)) };
        };
    }
    
    public static BiFunction<Vector3f, Vector3f, Vector3f[]> SideB(float width) {
        final float w = width / 2f;
        return (Vector3f mid, Vector3f normal) -> {
            return new Vector3f[] { mid.add(normal.mult(-w)), mid.add(normal.mult(-w - 2)).add(0, -0.5f, 0) };
        };
    }
    
    private final Spline spline;
    private final int nbSubSegments;

    public final CatmullRomWidth middle;
    public final List<CatmullRomWidth> others;
    
    public CatmullRomRoad(Spline spline, int nbSubSegments, float width) {
        this.spline = spline;
        this.nbSubSegments = nbSubSegments;
        this.middle = new CatmullRomWidth(spline, nbSubSegments, width);
        this.others = new LinkedList<>();
    }

    public void addWidth(BiFunction<Vector3f, Vector3f, Vector3f[]> func) {
        others.add(new CatmullRomWidth(spline, nbSubSegments, func));
    }
}
