package world.highway;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.instancing.InstancedNode;

import game.App;
import helper.H;
import jme3tools.optimize.GeometryBatchFactory;
import terrainWorld.Terrain;
import terrainWorld.TerrainChunk;
import terrainWorld.TileListener;

//TODO rename to road maker or something
public class TerrainListener implements TileListener {

	private static final String[] Tree_Strings = new String[] {
				"assets/objects/tree_0.blend",
				"assets/objects/tree_1.blend",
				"assets/objects/tree_2.blend",
				"assets/objects/tree_3.blend",
				"assets/objects/tree_4.blend",
				"assets/objects/tree_5.blend",
				"assets/objects/tree_6.blend",
			};
	
	private Spatial treeGeoms[] = new Spatial[Tree_Strings.length];
	
	private HighwayWorld world;
	private Terrain terrain;
	
	private InstancedNode treeNode = new InstancedNode("tree node");
	
	private Vector3f lastPoint;
	private Vector3f nextPoint;
	private float angle = FastMath.nextRandomFloat()*FastMath.TWO_PI;
	
	private int totalChunks;
	private int totalLoaded;
	private boolean terrainDoneLoading;
	
	private Map<Vector3f, Node> treeNodes;
	
	public TerrainListener(HighwayWorld world) {
		this.world = world;
		this.terrain = world.terrain;
		this.totalChunks = terrain.getTotalVisibleChunks();
		this.treeNodes = new HashMap<Vector3f, Node>();
		
		this.treeNode.setShadowMode(ShadowMode.CastAndReceive);
		App.rally.getRootNode().attachChild(this.treeNode);
		
		H.p("Terrain started with direction: ", angle);
		
		for (int i = 0; i < Tree_Strings.length; i++) {
			Spatial spat = App.rally.getAssetManager().loadModel(TerrainListener.Tree_Strings[i]);
			if (spat instanceof Node) {
				for (Spatial s: ((Node) spat).getChildren()) {
					this.treeGeoms[i] = s;
				}
			} else {
				this.treeGeoms[i] = (Geometry) spat;
			}
		}
		
		setNextPoint(); //init next point
	}
	
	private void initRoads(TerrainChunk chunk) {
		while (true) {
			float height = 0;
			Vector2f pos = H.v3tov2fXZ(nextPoint);
			TerrainChunk tc = terrain.chunkFor(pos);
			if (tc != null)
				height = tc.getHeight(pos);
			
			if (Float.isNaN(height) || height == 0) {
				height = chunk.getHeight(pos); //try last chunk (that won't be loaded at the moment)
				if (!Float.isNaN(height) || height != 0) {
					break;
				}
			}
	
			nextPoint.y = height;
			setNextPoint();
		}
	}
	
	public boolean tileLoaded(TerrainChunk chunk) {
		totalLoaded++;
		
		if (App.rally.IF_DEBUG)
			App.rally.getRootNode().attachChild(H.makeShapeBox(App.rally.getAssetManager(), ColorRGBA.Green, chunk.getLocalTranslation().add(0,110,0), 2));
		
		//terrain needs to load all of its tiles before we will use the grow method
		//this prevents the order of the tiles screwing with placements
		if (totalLoaded == totalChunks) {
			terrainDoneLoading = true;
			initRoads(chunk);
		}
		//spawn trees on it
		Vector3f pos = chunk.getLocalTranslation();
		if (this.treeNodes.containsKey(pos)) {
			Node n = this.treeNodes.get(pos);
			if (n == null) {
				H.e("no trees with vector: ", chunk.getLocalTranslation(), " :(");
				return true;
			}
			
			for (Spatial c: n.getChildren()) {
				App.rally.getPhysicsSpace().add(c);
			}
			this.treeNode.attachChild(this.treeNodes.get(pos));
		} else {
			//please not twice
			Node node = new Node("treenode");
			this.treeNode.attachChild(node);
			this.treeNodes.put(pos, node);
			
			int count = 100;
			float size = world.getTileSize()/2;
			for (int i = 0; i < count; i++) {
				float x = (2*FastMath.rand.nextFloat() - 1)*size + pos.x;
				float z = (2*FastMath.rand.nextFloat() - 1)*size + pos.z;
				
				float height = chunk.getHeight(new Vector2f(x,z));
				if (Float.isNaN(height) || height == 0)
					continue;
				
				Spatial s = treeGeoms[FastMath.nextRandomInt(0, treeGeoms.length-1)].clone();
				
				Vector3f newPos = new Vector3f(x, height, z);
				s.setLocalTranslation(newPos);
				s.addControl(new RigidBodyControl(0));
				node.attachChild(s);
				App.rally.getPhysicsSpace().add(s);
			}
			GeometryBatchFactory.optimize(node);
		}
	
		if (!terrainDoneLoading)
			return true;
		
		while (true) {
			float height = chunk.getHeight(H.v3tov2fXZ(nextPoint));
			if (Float.isNaN(height) || height == 0) {
				break; //no more space to load road on
			}
			nextPoint.y = height;
			
			setNextPoint();
		}
		
		return true;
	}
	private void setNextPoint() {
		if (lastPoint == null) {
			lastPoint = new Vector3f();
			nextPoint = new Vector3f();
			return;
		}
		
		lastPoint = lastPoint == null ? new Vector3f(Vector3f.ZERO) : lastPoint;

		Vector3f dir = nextPoint.subtract(lastPoint).normalize();
		float length = nextPoint.subtract(lastPoint).length();
		List<Vector3f> list = Arrays.asList(new Vector3f[] { lastPoint, lastPoint.add(dir.mult(length/3)).add(H.randV3f()), nextPoint.subtract(dir.mult(length/3)).add(H.randV3f()), nextPoint });
		RoadMesh m = new RoadMesh(5, 2, list);
		world.generateRoad(m);
		
		angle += FastMath.DEG_TO_RAD*FastMath.nextRandomFloat()*3*(FastMath.nextRandomFloat() < 0.5f ? -1 : 1);
		Quaternion rot = new Quaternion().fromAngleAxis(angle, Vector3f.UNIT_Y);
		
		Vector3f next = new Vector3f(FastMath.nextRandomFloat()*20 + 30, 0, 0);
		Vector3f point = nextPoint.add(rot.mult(next));
		
		lastPoint = nextPoint;
		nextPoint = point;
		nextPoint.y = 0;
		H.p("next road point:", nextPoint);
	}
	
	@Override
	public void tileLoadedThreaded(TerrainChunk terrainChunk) {
		//Do not add anything to the scene with this method as its on another thread
		//TODO use this method to keep it on the other thread?
	}
	@Override
	public boolean tileUnloaded(TerrainChunk terrainChunk) {
		Node n = this.treeNodes.get(terrainChunk.getLocalTranslation());
		if (n == null) {
			H.e("no trees at: ", terrainChunk.getLocalTranslation(), " :(");
			return true;
		}
		for (Spatial c: n.getChildren()) {
			App.rally.getPhysicsSpace().remove(c);
		}
		this.treeNode.detachChild(n);
		return true; 
	}
	@Override
	public String imageHeightmapRequired(int x, int z) { return null; }
	
	public void cleanup() {
		App.rally.getRootNode().detachChild(treeNode);
	}
}