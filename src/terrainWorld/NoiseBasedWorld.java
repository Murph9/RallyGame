package terrainWorld;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.terrain.noise.basis.FilteredBasis;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

//see base class for terrainWorld info

//modified to cache the filtered basis calls
public class NoiseBasedWorld extends Terrain
{
    private Material terrainMaterial;
    private FilteredBasis filteredBasis;
    
	private HashMap<Vector2f, float[]> cache;

    public NoiseBasedWorld(SimpleApplication app, PhysicsSpace physicsSpace, int tileSize, int blockSize, Node rootNode) {
        super(app, physicsSpace, tileSize, blockSize, rootNode, 1);
        //TODO the 1 defines the save file name, useful for seeding
        
        cache = new HashMap<Vector2f, float[]>();
    }

    public final Material getMaterial() { return this.terrainMaterial; }
    public final void setMaterial(Material material) { this.terrainMaterial = material; }

    public final FilteredBasis getFilteredBasis() { return this.filteredBasis; }
    public final void setFilteredBasis(FilteredBasis basis) { this.filteredBasis = basis; }

    @SuppressWarnings("unused")
	@Override
    public TerrainChunk getTerrainChunk(TerrainLocation location) {
        TerrainChunk tq = this.worldTiles.get(location);

        if (tq != null)
            return tq;

        tq = this.worldTilesCache.get(location);

        if (tq != null)
            return tq;

        String tqName = "TerrainChunk_" + location.getX() + "_" + location.getZ();

        float[] heightmap = null; 
        File savedFile = new File(System.getProperty("user.home") + "/.murph9/world_" + this.fileSeed + "/" + tqName + ".chunk");

        if (false && savedFile.exists())
        {
            try
            {
                FileInputStream door = new FileInputStream(savedFile);
                try (ObjectInputStream reader = new ObjectInputStream(door)) {
                	heightmap = (float[])reader.readObject();
                }
            }
            catch(Exception ex)
            {
                Logger.getLogger(NoiseBasedWorld.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        else
        {
            heightmap = getHeightmap(location);
        }

        tq = new TerrainChunk(tqName, this.tileSize, this.blockSize, heightmap, this.fileSeed);
        tq.setLocalScale(new Vector3f(1f, this.worldHeight, 1f));

        // set position
        int tqLocX = location.getX() << this.bitshift;
        int tqLoxZ = location.getZ() << this.bitshift;
        tq.setLocalTranslation(new Vector3f(tqLocX, 0, tqLoxZ));

        // add rigidity
        tq.addControl(new RigidBodyControl(new HeightfieldCollisionShape(heightmap, tq.getLocalScale()), 0));

        tq.setMaterial(terrainMaterial);
        return tq;
    }
    
    private float[] getHeightmap(TerrainLocation tl) {
    	return getHeightmap(tl.getX(), tl.getZ());
    }
    public float[] getHeightmap(int x, int z) {
    	if (this.filteredBasis == null) 
    	{
    		try {
				throw new Exception("No filteredBasis");
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
    	}
    	
        Vector2f pos = new Vector2f(x, z);
    	if (cache.containsKey(pos))
    		return cache.get(pos);
    	
        float[] array = this.filteredBasis.getBuffer(x * (this.blockSize - 1), z * (this.blockSize - 1), 0, this.blockSize).array();
        cache.put(pos, array);

        return array;
    }
}
