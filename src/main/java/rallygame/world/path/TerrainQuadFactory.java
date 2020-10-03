package rallygame.world.path;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.terrain.geomipmap.TerrainQuad;

import rallygame.helper.H;

public class TerrainQuadFactory {
    
    private final FilteredBasisTerrain basis;

    public TerrainQuadFactory(FilteredBasisTerrain basis) {
        this.basis = basis;
    }

    public TerrainQuad create(AssetManager am, Vector2f offset, Vector3f scale, int sideLength) {
        float[] heightMap = basis.getBuffer(sideLength, offset).array();
        var terrain = new TerrainQuad("path terrain", sideLength, sideLength, heightMap);
        terrain.setLocalScale(scale);

        terrain.setLocalTranslation(pieceGridToWorldPos(offset.mult((sideLength - 1)), scale));

        terrain.setMaterial(createMaterial(am, scale.y));
        
        return terrain;
    }

    private Material createMaterial(AssetManager am, float scale) {
        var baseMat = new Material(am, "MatDefs/terrainheight/TerrainColorByHeight.j3md");
        baseMat.setColor("LowColor", new ColorRGBA(1.0f, 0.55f, 0.0f, 1.0f));
        baseMat.setColor("HighColor", new ColorRGBA(0.0f, 0.0f, 1.0f, 1.0f));
        baseMat.setFloat("Scale", scale * 0.6f); // margin of 0.2f
        baseMat.setFloat("Offset", scale * 0.2f);
        return baseMat;
    }

    private static Vector3f pieceGridToWorldPos(Vector2f offset, Vector3f scale) {
        return H.v2tov3fXZ(H.v3tov2fXZ(scale).multLocal(offset));
    }
}
