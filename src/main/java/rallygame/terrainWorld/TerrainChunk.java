package rallygame.terrainWorld;

import com.jme3.scene.Node;
import com.jme3.terrain.geomipmap.TerrainQuad;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TerrainChunk extends TerrainQuad
{
    private Node staticRigidObjects;
    private Node staticNonRigidObjects;

    private int fileSeed;
    
    public TerrainChunk(String name, int patchSize, int totalSize, float[] heightmap, int fileSeed)
    {
        super(name, patchSize, totalSize, heightmap);
        this.fileSeed = fileSeed;
    }

    public Node getStaticRigidObjectsNode() { return this.staticRigidObjects; }
    public void setStaticRigidObjectsNode(Node node) { this.staticRigidObjects = node; }

    public Node getStaticNonRigidObjectsNode() { return this.staticNonRigidObjects; }
    public void setStaticNonRigidObjectsNode(Node node) { this.staticNonRigidObjects = node; }
    
    @Override
    public void clearCaches() {
    	super.clearCaches();
    	
    	//then clear the cache
        try {
            this.save();
        } catch (IOException ex) {
            Logger.getLogger(TerrainChunk.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void save() throws IOException
    {
        float[] hmap = this.getHeightMap();

        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(System.getProperty("user.home") + "/.murph9/world_" + this.fileSeed + "/" + this.getName() + ".chunk"));
        out.writeObject(hmap);
        out.close();
    }
}
