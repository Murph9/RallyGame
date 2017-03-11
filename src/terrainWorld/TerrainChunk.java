package terrainWorld;

import com.jme3.scene.Node;
import com.jme3.terrain.geomipmap.TerrainQuad;

public class TerrainChunk extends TerrainQuad
{
    private Node staticRigidObjects;
    private Node staticNonRigidObjects;

    public TerrainChunk(String name, int patchSize, int totalSize, float[] heightmap)
    {
        super(name, patchSize, totalSize, heightmap);
    }

    public Node getStaticRigidObjectsNode() { return this.staticRigidObjects; }
    public void setStaticRigidObjectsNode(Node node) { this.staticRigidObjects = node; }

    public Node getStaticNonRigidObjectsNode() { return this.staticNonRigidObjects; }
    public void setStaticNonRigidObjectsNode(Node node) { this.staticNonRigidObjects = node; }
}
