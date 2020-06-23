package rallygame.world.path;

import java.util.LinkedList;

import com.jme3.math.Vector3f;

class RoadPointList extends LinkedList<Vector3f> {
    private static final long serialVersionUID = 1L;
    public CatmullRomRoad road;

    public boolean failed;
    public String failedMessage;
}
