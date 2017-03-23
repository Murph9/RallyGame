package world.curve;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import javax.imageio.ImageIO;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

import game.App;
import helper.H;
import jme3tools.optimize.GeometryBatchFactory;
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

Later Things:
- intesections
- other kinds of road
 */

//TODO things that could help
//https://github.com/jayfella/TerrainWorld/blob/master/src/worldtest/Main.java
//https://en.wikipedia.org/wiki/Diamond-square_algorithm
//jme3test.terrain.TerrainTestAdvanced.java

//TODO reaching the 'gap' in the terrain edge and (casting a shadow over it?) causes a crash

//TODO cleanup properly, we keep hiting the memory limit

//TODO thinking about creating an l-system road network
//Main notes: http://www.tmwhere.com/city_generation.html
//Other: https://www.reddit.com/r/gamedev/comments/19ic3j/procedural_content_generation_how_to_generate/


//TODO other terrain generation method called PerlinNoise
public class CurveWorld extends World {
	private AbstractHeightMap map;
	private TerrainQuad terrain;
	
	private RigidBodyControl c;
	
	private static final String Tree_String = "assets/objects/tree.blend";
	private Node treeNode;
	private Spatial treeGeom;
	
	private Roads road;
	
	private LRoadGenerator lrg;
	
	public CurveWorld() {
		super("curveWorldRoot");
		road = new Roads();
	}
	
	@Override
	public WorldType getType() {
		return WorldType.CURVE;
	}
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		
//		/* TODO this all has all been removed until we sort out the l-system stuff
		generateTerrain(app);
		/*
		boolean roadPlease = true; //SET the type here
		if (roadPlease)
			generateRoads(app);
		else
			generateCity(app);
		
		fixTerrainHeights();
		generateRandomTrees(app);
		*/

		generateLargeFlatBox(app);
//		lrg = new LRoadGenerator(rootNode, 0.2f);
//		lrg.init();
		
		saveTerrainToFile();
	}
	
	private void generateLargeFlatBox(Application app) {
		Material matfloor = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
//		Material matfloor = new Material(app.getAssetManager(), "assets/mat/SolidColor2.j3md");
		matfloor.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		matfloor.setColor("Color", ColorRGBA.Green);
		
		Box start = new Box(100, 0.25f, 100);
		Geometry startBox = new Geometry("Starting Box", start);
		startBox.setMaterial(matfloor);
		startBox.setLocalTranslation(0, -0.1f, 0);
		startBox.addControl(new RigidBodyControl(0));
		
		this.rootNode.attachChild(startBox);
		App.rally.getPhysicsSpace().add(startBox);
	}

	
	private void generateTerrain(Application app) {
		AssetManager am = app.getAssetManager();
		
		//create the terrain (from terrain world)
		Material terrainMaterial = new Material(am, "Common/MatDefs/Terrain/HeightBasedTerrain.j3md");
		
		float grassScale = 64; //all 16
        float dirtScale = 64;
        float rockScale = 64;
		
        // GRASS texture
        Texture grass = am.loadTexture("assets/terrain/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        terrainMaterial.setTexture("region1ColorMap", grass);
        terrainMaterial.setVector3("region1", new Vector3f(22, 82, grassScale)); //88,200

        // DIRT texture
        Texture dirt = am.loadTexture("assets/terrain/dirt.jpg");
        dirt.setWrap(WrapMode.Repeat);
        terrainMaterial.setTexture("region2ColorMap", dirt);
        terrainMaterial.setVector3("region2", new Vector3f(0, 23, dirtScale));//0,90

        // ROCK texture
        Texture rock = am.loadTexture("assets/terrain/Rock.PNG");
        rock.setWrap(WrapMode.Repeat);
        terrainMaterial.setTexture("region3ColorMap", rock);
        terrainMaterial.setVector3("region3", new Vector3f(80, 130, rockScale)); //198,260

        terrainMaterial.setTexture("region4ColorMap", rock);
        terrainMaterial.setVector3("region4", new Vector3f(80, 130, rockScale));//198,260

        Texture rock2 = am.loadTexture("assets/terrain/rock.jpg");
        rock2.setWrap(WrapMode.Repeat);

        terrainMaterial.setTexture("slopeColorMap", rock2);
        terrainMaterial.setFloat("slopeTileFactor", 32); //32

		//for reference some other implementations:
//		Texture heightMapImage = am.loadTexture("Textures/Terrain/splat/mountains512.png");
//		map = new ImageBasedHeightMap(heightMapImage.getImage());
//		map = new HillHeightMap(1025, 1000, 50, 200, FastMath.rand.nextLong());
		
		map = new DiamondSquareMap(1025);
//		map = new PerlinNoise(1025);
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
	    cameras.add(app.getViewPort().getCamera());
	    
	    TerrainLodControl control = new TerrainLodControl(terrain, cameras);
	    terrain.addControl(control);
	    c = new RigidBodyControl(0);

	    terrain.addControl(c);
	    App.rally.getPhysicsSpace().add(c);
	    //end terrain
	}

	@SuppressWarnings("unused")
	private void generateRoads(Application app) {
		float height = terrain.getHeight(new Vector2f(0,0.001f));
		float height2 = terrain.getHeight(new Vector2f(0,75));
		Vector3f[] points = new Vector3f[] {
				new Vector3f(0,height,0),
				new Vector3f(0,height,50),
				new Vector3f(0,height2,25),
				new Vector3f(0,height2,75),
			};
		Curve curve = null;
		curve = new BeizerCurve(points, Roads.CurveFunction());
		road.placePiece(new CurveQueueObj(0, curve, null), rootNode, App.rally.getPhysicsSpace(), true);
		
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

			curve = new BeizerCurve(points, Roads.CurveFunction());
			road.placePiece(new CurveQueueObj(0, curve, null), rootNode, App.rally.getPhysicsSpace(), true);
		}
	}
	
	public void generateCity(Application app) {
		int size = 50;
		
		// l-system like generation attempt like http://www.tmwhere.com/city_generation.html
		PriorityQueue<CurveQueueObj> Q = new PriorityQueue<CurveQueueObj>(10);
		List<CurveQueueObj> S = new ArrayList<CurveQueueObj>(size);

		Q.add(road.generateFirst(terrain));
		
		int i = 0;
		while (!Q.isEmpty() && i < size) {
			CurveQueueObj q = Q.poll();
			S.add(q);
			i++;
			
			for (CurveQueueObj c: road.genNextNodes(q, terrain))
				Q.add(c);
		}
		
		for (CurveQueueObj c: S) {
			road.placePiece(c, rootNode, App.rally.getPhysicsSpace(), false);
			H.p("Placing: " + c.rule);
		}
	}

	@SuppressWarnings("unused")
	private void generateRandomTrees(Application app) {
		Spatial spat = app.getAssetManager().loadModel(CurveWorld.Tree_String);
		if (spat instanceof Node) {
			for (Spatial s: ((Node) spat).getChildren()) {
				this.treeGeom = s;
			}
		} else {
			this.treeGeom = (Geometry) spat;
		}
		
		treeNode = new Node("curveWorldTreeNode");
		
		rootNode.attachChild(treeNode);
		
		int count = 1000;
		for (int i = 0; i < count; i++) {
			Spatial s = treeGeom.clone();
			int x = FastMath.rand.nextInt(2048) - 1024;
			int z = FastMath.rand.nextInt(2048) - 1024;
			
			if (rayHeightAt(x, z, "Quad") != 0)
				continue; //don't place on road
			
			Vector3f newPos = new Vector3f(x, terrain.getHeight(new Vector2f(x,z)), z);
			s.setLocalTranslation(newPos);
			s.addControl(new RigidBodyControl(0));
			rootNode.attachChild(s);
			App.rally.getPhysicsSpace().add(s);
		}
		
		//If you remove this line: fps = fps/n for large n
		GeometryBatchFactory.optimize(treeNode);
	}
	
	@SuppressWarnings("unused")
	private void fixTerrainHeights() {
		//TODO fix: very slow
		
		// offset it by radius because in the loop we iterate through 2 radii
        int size = 150;

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
        App.rally.getPhysicsSpace().remove(c);
        c = new RigidBodyControl(0.0f);
	    terrain.addControl(c);
	    App.rally.getPhysicsSpace().add(c);
	}
	
	private void saveTerrainToFile() {
		//print the terrain to file (for debugging)
		
		float[] height_map = terrain.getHeightMap();
		double p = Math.sqrt(height_map.length);
		
		int q = (int) p;
		int counter = 0;
		BufferedImage image = new BufferedImage(q, q, BufferedImage.TYPE_INT_RGB);

		for (int x = 0; x < q; x++) {
			for (int y = 0; y < q; y++) {
				double value = Math.round(height_map[counter]);

				int qq = (int) value; // OR int qq = 255- (int) value;
				image.setRGB(x, y, new Color(qq, qq, qq).getRGB());
				counter = counter + 1;
			}
		}
		File outputfile = new File("saved.png");
		try {
			ImageIO.write(image, "png", outputfile);
		} catch (IOException ex) {
			//this is debug code so, oh well
			H.e("Image from terrain failed");
		}
//		*/
	}
	
	@Override
	public Vector3f getStartPos() {
		if (terrain != null)
			return new Vector3f(0,terrain.getHeight(new Vector2f(0, 3))+0.5f,3);
		else 
			return new Vector3f(0,0.5f,0);
	}
	@Override
	public void update(float tpf) {
		if (!isEnabled())
			return;
		
		if (lrg != null) 
			lrg.update(tpf);
	}
	@Override
	public void reset() { }

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
	
	/*
	private void drawMeACurve(Curve bc, boolean helpPoints) {
		BSegment[] nodes = bc.calcPoints();
		
		for (int i = 1; i < nodes.length; i++) {
			for (int j = 2; j < nodes[i].v.length; j++) { //avoid the first one
				Vector3f[] vs = new Vector3f[] {
					nodes[i-1].v[j-1],
					nodes[i-1].v[j],
					nodes[i].v[j-1],
					nodes[i].v[j],
				};
				
				drawMeAQuad(vs, ColorRGBA.Black);
			}
		}
		
		if (helpPoints) {
			Vector3f[] nds = bc.getNodes();
			for (Vector3f v: nds)
				drawMeAVerySmallBox(v, ColorRGBA.LightGray);
//			drawMeALine(nds[0], nds[1], ColorRGBA.Blue);
//			drawMeALine(nds[2], nds[3], ColorRGBA.Blue); //TODO doesn't work for anything but cubic curves
		}
	}
	private void drawMeALine(Vector3f start, Vector3f end, ColorRGBA colour) {
		Line line = new Line(start, end);
		Geometry geometry = new Geometry("line", line);
		Material mat = new Material(App.rally.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", colour);
		mat.getAdditionalRenderState().setLineWidth(2);
		geometry.setMaterial(mat);
		rootNode.attachChild(geometry);
	}

	private void adjustHeights(Curve curve) {
		BSegment[] bseg = curve.calcPoints();
		Vector3f[] nodes = new Vector3f[bseg.length];
		for ( int i = 0; i < bseg.length; i++)
			nodes[i] = bseg[i].v[0];
		
		for (Vector3f p : nodes) {
			adjustHeight(p, 25);
		}
	}
	
	private void adjustHeight(Vector3f pos, float radius) {
		// offset it by radius because in the loop we iterate through 2 radii
        int radiusStepsX = (int) (radius / terrain.getLocalScale().x);
        int radiusStepsZ = (int) (radius / terrain.getLocalScale().z);

        float xStepAmount = terrain.getLocalScale().x;
        float zStepAmount = terrain.getLocalScale().z;
        List<Vector2f> locs = new ArrayList<Vector2f>();
        List<Float> heights = new ArrayList<Float>();
        
        for (int z = -radiusStepsZ; z < radiusStepsZ; z++) {
            for (int x = -radiusStepsX; x < radiusStepsX; x++) {

                float posX = pos.x + (x * xStepAmount);
                float posZ = pos.z + (z * zStepAmount);

                // see if it is in the radius of the tool
            	Vector2f npos = new Vector2f(posX, posZ);
                
                float h = rayHeightAt(posX, posZ, "Quad");
                if (h != 0) {
                	heights.add(h - 0.2f);
                	locs.add(npos);
                }
            }
        }

        //update visuals
        terrain.setHeight(locs, heights);
        terrain.updateModelBound();

        //update physics
        getPhysicsSpace.remove(c);
        c = new RigidBodyControl(0.0f);
	    terrain.addControl(c);
	    getPhysicsSpace.add(c);
	}
	
	private boolean isInRadius(float x, float y, float radius) {
        Vector2f point = new Vector2f(x, y);
        // return true if the distance is less than equal to the radius
        return point.length() <= radius;
    }
    */
}
