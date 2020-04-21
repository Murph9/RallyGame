package rallygame.world.path;

import java.nio.FloatBuffer;
import java.util.function.Function;

import com.jme3.math.Vector2f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import rallygame.helper.H;

public class GrassTerrain extends Mesh {

    private final TerrainPiece piece;
    private final Function<Vector2f, Boolean> posValid;

    // TODO billboarding so they aren't all facing the same way
    // https://github.com/Simsilica/IsoSurface/blob/master/src/main/java/com/simsilica/iso/plot/GrassZone.java

    public GrassTerrain(TerrainPiece piece, int count, Function<Vector2f, Boolean> posValid) {
        this.piece = piece;
        this.posValid = posValid;

        init(count);
    }

    private void init(int count) {
        FloatBuffer vertexBuffer = BufferUtils.createVector3Buffer(count * 3);
        FloatBuffer normalBuffer = BufferUtils.createVector3Buffer(count * 3);
        FloatBuffer textureBuffer = BufferUtils.createVector2Buffer(count * 3);

        for (int i = 0; i < count; i++) {
            Vector2f pos = genValidPoint();
            float height = piece.terrain.getHeight(pos);
            if (Float.isNaN(height))
                continue;

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

        this.setStatic();
    }

    private Vector2f genValidPoint() {
        Vector2f pos = H.randV2f(piece.terrainScale.x * piece.sideLength, true);
        while (posValid.apply(pos)) {
            pos = H.randV2f(piece.terrainScale.x * piece.sideLength, true);
        }

        return pos;
    }
}
