package world.curve;

import java.util.ArrayList;
import java.util.List;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Node;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

import game.App;
import game.H;
import world.World;
import world.WorldType;

/*
Basic idea is:
Make road our of random beizer curves
- these beizer curves to have road width (use the code from the 3d course)
  + WIDTH of basic road, similar to TDU: 
  		{45'(edge) 2.5(shoulder) | 4.5 (lane) || 4.5 | 2.5 45'}
- eventually overlay it on terrain
 + flatten the terrain under the road so it 'fits'
- add grass and trees TDU like
*/

//TODO reaching the 'gap' in the terrain edge and (casting a shadow over it?) causes a crash

public class HighwayWorld implements World {
	private boolean isInit;
	
	private Node rootNode;
	private PhysicsSpace phys;
	
	private AbstractHeightMap map;
	private TerrainQuad terrain;
	
	private RigidBodyControl c;
	
	private Roads road;
	
	public HighwayWorld() {
		rootNode = new Node("curveWorldRoot");
		road = new Roads();
	}
	
	@Override
	public WorldType getType() {
		return WorldType.HIGHWAY;
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
		
		generateTerrain(am, view);
		generateRoads(am, view);
		
		fixTerrainHeights();
		
		return rootNode;
	}
	
	private void generateTerrain(AssetManager am, ViewPort view) {
		//create the terrain (from terrain world)
		Material terrainMaterial = new Material(am, "Common/MatDefs/Terrain/HeightBasedTerrain.j3md");
		
		float grassScale = 64; //all 16
        float dirtScale = 64;
        float rockScale = 64;
		
        // GRASS texture
        Texture grass = am.loadTexture("Textures/Terrain/splat/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        terrainMaterial.setTexture("region1ColorMap", grass);
        terrainMaterial.setVector3("region1", new Vector3f(22, 82, grassScale)); //88,200

        // DIRT texture
        Texture dirt = am.loadTexture("Textures/Terrain/splat/dirt.jpg");
        dirt.setWrap(WrapMode.Repeat);
        terrainMaterial.setTexture("region2ColorMap", dirt);
        terrainMaterial.setVector3("region2", new Vector3f(0, 23, dirtScale));//0,90

        // ROCK texture
        Texture rock = am.loadTexture("Textures/Terrain/Rock/Rock.PNG");
        rock.setWrap(WrapMode.Repeat);
        terrainMaterial.setTexture("region3ColorMap", rock);
        terrainMaterial.setVector3("region3", new Vector3f(80, 130, rockScale)); //198,260

        terrainMaterial.setTexture("region4ColorMap", rock);
        terrainMaterial.setVector3("region4", new Vector3f(80, 130, rockScale));//198,260

        Texture rock2 = am.loadTexture("Textures/Terrain/Rock2/rock.jpg");
        rock2.setWrap(WrapMode.Repeat);

        terrainMaterial.setTexture("slopeColorMap", rock2);
        terrainMaterial.setFloat("slopeTileFactor", 32); //32

		//for reference some other implementations:
//		Texture heightMapImage = am.loadTexture("Textures/Terrain/splat/mountains512.png");
//		map = new ImageBasedHeightMap(heightMapImage.getImage());
//		map = new HillHeightMap(1025, 1000, 50, 200, FastMath.rand.nextLong());
		
		map = new DiamondSquareMap(1025);
	    map.load();
	    
	    map.normalizeTerrain(100); //TODO affects the terrain textures
	    
	    terrain = new TerrainQuad("my terrain", 65, 1025, map.getHeightMap());
	    terrain.setMaterial(terrainMaterial);
	    terrain.setShadowMode(ShadowMode.CastAndReceive);
	    terrain.setLocalScale(2.5f, 1, 2.5f); //its 1:1 without this (terrain point per meter)
	    
	    Material m_debug = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
	    m_debug.setColor("Color", ColorRGBA.Black);
	    
	    rootNode.attachChild(terrain);
	    List<Camera> cameras = new ArrayList<Camera>();
	    cameras.add(view.getCamera());
	    
	    TerrainLodControl control = new TerrainLodControl(terrain, cameras);
	    terrain.addControl(control);
	    c = new RigidBodyControl(0);

	    terrain.addControl(c);
	    phys.add(c);
	    //end terrain
	}

	private void generateRoads(AssetManager am, ViewPort view) {
		float height = terrain.getHeight(new Vector2f(0,0.001f));
		float height2 = terrain.getHeight(new Vector2f(0,75));
		Vector3f[] points = new Vector3f[] {
				new Vector3f(0,height,0),
				new Vector3f(0,height,50),
				new Vector3f(0,height2,25),
				new Vector3f(0,height2,75),
			};
		Curve curve = null;
		curve = new StraightCurve(new Vector3f[] { points[0],points[3] }, Roads.CurveFunction());
//		curve = new BeizerCurve(points, Roads.curveFunction());
		road.placePiece(new CurveQueueObj(0, curve, ""), rootNode, phys, true);
		
		for (int i = 0; i < 10; i++) {
			points[1] = points[3].add(points[3].subtract(points[2]));
			points[0] = points[3];
			
			int x = FastMath.rand.nextInt(50)+100;
			int z = FastMath.rand.nextInt(50)+100;
			
			int xm = x - (FastMath.rand.nextInt(20)+30);
			int zm = z - (FastMath.rand.nextInt(20)+30);

			//set the height of the road
			float y = rayHeightAt(x+points[0].x, z+points[0].z,null);
			
			points[3] = points[0].add(new Vector3f(x, 0, z));
			points[3].y = y;
			points[2] = points[0].add(new Vector3f(xm, 0, zm));
			points[2].y = y;

//			curve = new BeizerCurve(points, Roads.curveFunction());
			curve = new StraightCurve(new Vector3f[] { points[0],points[3] }, Roads.CurveFunction());
			road.placePiece(new CurveQueueObj(0, curve, null), rootNode, phys, true);
		}
	}
	
	private void fixTerrainHeights() {
		//TODO fix: very slow
		
		// offset it by radius because in the loop we iterate through 2 radii
        int size = 200;

        float xStepAmount = terrain.getLocalScale().x;
        float zStepAmount = terrain.getLocalScale().z;
        List<Vector2f> locs = new ArrayList<Vector2f>();
        List<Float> heights = new ArrayList<Float>();
        
        for (int z = -size; z < size; z++) {
            for (int x = -size; x < size; x++) {

                float posX = x * xStepAmount;
                float posZ = z * zStepAmount;

                // see if it is in the radius of the tool
            	Vector2f npos = new Vector2f(posX, posZ);
                
                float h = rayHeightAt(posX, posZ, "Quad");
                if (h != 0) {
                	heights.add(h - 0.2f);
                	locs.add(npos);
                }
                
            }
            H.e(z);
        }

        //update visuals
        terrain.setHeight(locs, heights);
        terrain.updateModelBound();

        //update physics
        phys.remove(c);
        c = new RigidBodyControl(0.0f);
	    terrain.addControl(c);
	    phys.add(c);
	}
	
	//interface nodes
	@Override
	public Node getRootNode() { return rootNode; }
	@Override
	public Vector3f getWorldStart() {
		if (terrain != null)
			return new Vector3f(0,terrain.getHeight(new Vector2f(0, 3))+0.5f,3);
		else 
			return new Vector3f(0,0.5f,0);
	}
	@Override
	public Matrix3f getWorldRot() { return new Matrix3f(Matrix3f.IDENTITY); }
	@Override
	public void update(float tpf, Vector3f playerPos, boolean force) { }
	@Override
	public void reset() { }
	@Override
	public void cleanup() { isInit = false; }
	@Override
	public Vector3f getNextPieceClosestTo(Vector3f pos) { return null; }

	private float rayHeightAt(float x, float z, String target) {
		CollisionResults results = new CollisionResults();
		Ray ray = new Ray(new Vector3f(x, 500, z), new Vector3f(0,-1,0));
		rootNode.collideWith(ray, results);
		
		float y = 0;
		if (results.size() > 0) {
			for (CollisionResult r : results) {
				if (target == null || r.getGeometry().getName().equals(target))
					return r.getContactPoint().y;
			}
		}
		return y;
	}
	
	}
