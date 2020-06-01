package rallygame.world.path;

import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.util.BufferUtils;

import rallygame.helper.H;

public class GrassTerrain extends Mesh {

    // https://github.com/Simsilica/IsoSurface/blob/master/src/main/java/com/simsilica/iso/plot/GrassZone.java

    private final TerrainQuad quad;
    private final Vector3f scale;
    private final Function<Vector2f, Boolean> posValid;

    private final List<Grass> triangles;

    public GrassTerrain(TerrainQuad quad, Vector3f scale, int count, Function<Vector2f, Boolean> posValid) {
        this.quad = quad;
        this.scale = scale;
        this.posValid = posValid;
        this.triangles = new LinkedList<>();

        init(count);
    }

    private void init(int count) {
        FloatBuffer vertexBuffer = BufferUtils.createVector3Buffer(count * 3);
        FloatBuffer normalBuffer = BufferUtils.createVector3Buffer(count * 3);
        FloatBuffer textureBuffer = BufferUtils.createVector2Buffer(count * 3);

        for (int i = 0; i < count; i++) {
            Vector2f pos = validPoint();
            if (pos == null)
                continue; //then it lost the lottery
            float height = quad.getHeight(pos);
            if (Float.isNaN(height))
                continue;

            triangles.add(new Grass(new Vector3f(pos.x, height, pos.y), 1));
            vertexBuffer.put(pos.x - 1).put(height).put(pos.y);
            vertexBuffer.put(pos.x + 1).put(height).put(pos.y);
            vertexBuffer.put(pos.x).put(height + 1).put(pos.y);

            normalBuffer.put(0).put(0).put(1);
            normalBuffer.put(0).put(0).put(1);
            normalBuffer.put(0).put(0).put(1);

            textureBuffer.put(0).put(0);
            textureBuffer.put(1).put(0);
            textureBuffer.put(0.5f).put(1);
        }

        this.setBuffer(VertexBuffer.Type.Position, 3, vertexBuffer);
        this.setBuffer(VertexBuffer.Type.Normal, 3, normalBuffer);
        this.setBuffer(VertexBuffer.Type.TexCoord, 2, textureBuffer);

        this.setMode(Mesh.Mode.Triangles);
        this.updateCounts();
        this.updateBound();
    }

    private Vector2f validPoint() {
        Vector2f pos = H.randV2f(scale.x * quad.getTerrainSize(), true);
        if (posValid.apply(pos))
            return null;
        return pos;
    }

    protected void update(Camera cam) {
        FloatBuffer vertexBuffer = this.getFloatBuffer(VertexBuffer.Type.Position);
        vertexBuffer.rewind();

        Quaternion rot = cam.getRotation();
        for (Grass f : triangles) {
            vertexBuffer.put(f.calcPoints(rot));
        }

        this.setBuffer(VertexBuffer.Type.Position, 3, vertexBuffer);
        this.updateBound();
    }

    private class Grass {
        private final Vector3f pos;
        private final float size;

        public Grass(Vector3f pos, float size) {
            this.pos = pos;
            this.size = size;
        }

        private float[] calcPoints(Quaternion camRot) {
            Vector3f left = pos.add(camRot.mult(new Vector3f(-size, 0, 0)));
            Vector3f right = pos.add(camRot.mult(new Vector3f(size, 0, 0)));

            return new float[] {
                left.x, left.y, left.z,
                right.x, right.y, right.z,
                pos.x, pos.y + size, pos.z
            };
        }
    }
}
