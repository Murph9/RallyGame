package rallygame.service;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.jme3.math.Vector2f;
import com.jme3.terrain.geomipmap.TerrainQuad;

//AStar on a terrainquad
public class AStar {

    private final TerrainQuad quad;

    public AStar(TerrainQuad quad) {
        this.quad = quad;
    }

    public List<Vector2f> findPath(Vector2f start, Vector2f end) {
        Queue<Vector2f> queue = new LinkedList<>();
        List<Vector2f> path = new LinkedList<>();

        return null;
    }
}