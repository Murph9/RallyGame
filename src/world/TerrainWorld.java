package world;

import java.util.ArrayList;
import java.util.List;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Node;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

import game.App;

public class TerrainWorld implements World {

	//http://wiki.jmonkeyengine.org/jme3/beginner/hello_terrain.html
	
	private boolean isInit;
	
	private Node rootNode;
	private PhysicsSpace phys;
	
	AbstractHeightMap heightMap;
	private TerrainQuad terrain;
	
	public TerrainWorld() {
		rootNode = new Node("terrainRoot");	
	}
	
	@Override
	public WorldType getType() {
		return WorldType.TERRAIN;
	}

	@Override
	public boolean isInit() {
		return isInit;
	}

	@Override
	public Node init(PhysicsSpace space, ViewPort view) {
		isInit = true;
		phys = space;
		
		AssetManager am = App.rally.getAssetManager();
		
		// 1. Create terrain material and load four textures into it.
	    Material mat_terrain = new Material(am, "Common/MatDefs/Terrain/Terrain.j3md");

	    // 1.1) Add ALPHA map (for red-blue-green coded splat textures)
	    mat_terrain.setTexture("Alpha", am.loadTexture("Textures/Terrain/splat/alphamap.png"));

	    // 1.2) Add GRASS texture into the red layer (Tex1).
	    Texture grass = am.loadTexture("Textures/Terrain/splat/grass.jpg");
	    grass.setWrap(WrapMode.Repeat);
	    mat_terrain.setTexture("Tex1", grass);
	    mat_terrain.setFloat("Tex1Scale", 64f);

	    // 1.3) Add DIRT texture into the green layer (Tex2)
	    Texture dirt = am.loadTexture("Textures/Terrain/splat/dirt.jpg");
	    dirt.setWrap(WrapMode.Repeat);
	    mat_terrain.setTexture("Tex2", dirt);
	    mat_terrain.setFloat("Tex2Scale", 32f);

	    // 1.4) Add ROAD texture into the blue layer (Tex3)
	    Texture rock = am.loadTexture("Textures/Terrain/splat/road.jpg");
	    rock.setWrap(WrapMode.Repeat);
	    mat_terrain.setTexture("Tex3", rock);
	    mat_terrain.setFloat("Tex3Scale", 128f);

	    // 2. Create the height map 
	    Texture heightMapImage = am.loadTexture("Textures/Terrain/splat/mountains512.png");
	    heightMap = new ImageBasedHeightMap(heightMapImage.getImage());
	    heightMap.setHeightScale(0.3f);
	    heightMap.load();

	    /*  3. We have prepared material and heightmap.
	     * Now we create the actual terrain:
	     * 3.1) Create a TerrainQuad and name it "my terrain".
	     * 3.2) A good value for terrain tiles is 64x64 -- so we supply 64+1=65.
	     * 3.3) We prepared a heightmap of size 512x512 -- so we supply 512+1=513.
	     * 3.4) As LOD step scale we supply Vector3f(1,1,1).
	     * 3.5) We supply the prepared heightmap itself.
	     */
	    terrain = new TerrainQuad("my terrain", 65, 513, heightMap.getHeightMap());

	    // 4. We give the terrain its material, position & scale it, and attach it.
	    terrain.setMaterial(mat_terrain);
	    terrain.setLocalTranslation(0, -100, 0);
	    terrain.setLocalScale(2f, 1f, 2f);
	    terrain.setShadowMode(ShadowMode.Receive);
	    rootNode.attachChild(terrain);

	    // 5. The LOD (level of detail) depends on were the camera is:
	    List<Camera> cameras = new ArrayList<Camera>();
	    cameras.add(view.getCamera());
	    TerrainLodControl control = new TerrainLodControl(terrain, cameras);
	    terrain.addControl(control);
	    
	    RigidBodyControl rbc = new RigidBodyControl(0.0f);
	    terrain.addControl(rbc);
	    phys.add(rbc);
	    
		return rootNode;
	}

	@Override
	public Node getRootNode() {
		return rootNode;
	}

	@Override
	public Vector3f getWorldStart() {
//		float height = heightMap.getTrueHeightAtPoint(0, 0);
		return new Vector3f(0, -57.586773f, 0);
	}

	@Override
	public Matrix3f getWorldRot() {
		return new Matrix3f();
	}

	@Override
	public void update(float tpf, Vector3f playerPos, boolean force) {
		
	}

	@Override
	public void reset() {
		
	}

	@Override
	public void cleanup() {
		rootNode.detachChild(terrain);
	}

	@Override
	public Vector3f getNextPieceClosestTo(Vector3f pos) {
		return null;
	}

}
