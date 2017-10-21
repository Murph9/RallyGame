package world.highway;

import java.util.Arrays;
import java.util.HashMap;
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
import com.jme3.terrain.noise.basis.FilteredBasis;

import game.App;
import helper.H;
import jme3tools.optimize.GeometryBatchFactory;
import terrainWorld.NoiseBasedWorld;
import terrainWorld.TerrainChunk;
import terrainWorld.TileListener;

//TODO rename to road maker or something
//TODO should fetch the heights based on the noise generator, instead of the terrain
//	should help generation code, and also allow preperation for more world at once
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
	
	private enum CurveTypes {
		//defined as going straight is +z(y) only
		Straight(new Vector2f[] { new Vector2f(0,0), new Vector2f(0,40), new Vector2f(0,40), new Vector2f(0,80) }),
		
//		Right(new Vector2f[] { new Vector2f(0,0), new Vector2f(0,40), new Vector2f(-40,80), new Vector2f(-80,80) }),
//		Left(new Vector2f[] { new Vector2f(0,0), new Vector2f(0,40), new Vector2f(40,80), new Vector2f(80,80) }),
		;
		//TODO make more on: https://www.desmos.com/calculator/cahqdxeshd
		//note that a line from 0,0 to 20,30 on that should be 0,0 to -20,30
		
		public final Vector2f[] points; 
		CurveTypes(Vector2f[] ps) {
			this.points = ps;
		}
	}
	
	private Spatial treeGeoms[] = new Spatial[Tree_Strings.length];
	
	private HighwayWorld world;
	private NoiseBasedWorld terrain;
	
	private InstancedNode treeNode = new InstancedNode("tree node");
	
	//you can calculate the last pos and the last rot based on these points
	private Vector3f[] lastPoints = new Vector3f[] { new Vector3f(0,0,0), new Vector3f(0,0,0), new Vector3f(0,0,0), new Vector3f(0,0,1) };
	
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
		
		//TODO random start angle H.p("Terrain started with direction: ", angle);
		
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
		
		generateRoadBit(); //init first road
	}
	
	private float getHeightFromBasis(Vector3f pos) {
		return getHeightFromBasis(H.v3tov2fXZ(pos));
	}
	private float getHeightFromBasis(Vector2f pos) {
		//TODO the purterb filters and iterative filters don't work unless i fetch the full chunk buffer 
		//causing the worst perf issue ive had that wasn't deliberate :(
		
		FilteredBasis fb = terrain.getFilteredBasis()[0];
		float[] heights = fb.getBuffer(pos.x, pos.y, 0, this.terrain.blockSize).array();
		
		H.p(heights.length, heights[(heights.length/2)+1] * terrain.getWorldHeight(), this.lastPoints[3]);
		return heights[(heights.length/2)+1] * terrain.getWorldHeight();
	}
	
	private void initRoads(TerrainChunk chunk) {
		while (true) {
			float height = 0;
			Vector2f pos = H.v3tov2fXZ(lastPoints[3]);
			TerrainChunk tc = terrain.chunkFor(pos);
			if (tc != null)
				height = tc.getHeight(pos);
			
			if (Float.isNaN(height) || height == 0) {
				height = chunk.getHeight(pos); //try last chunk (that won't be loaded at the moment)
				if (!Float.isNaN(height) || height != 0) {
					break;
				}
			}

			generateRoadBit();
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
			float height = chunk.getHeight(H.v3tov2fXZ(lastPoints[3]));
			if (Float.isNaN(height) || height == 0) {
				break; //no more space to load road on
			}
			H.p("height before:", height);
			generateRoadBit();
		}
		
		return true;
	}
	
	private void generateRoadBit() {
		//pick from the list of biezer curves
		Vector2f[] points = H.randFromArray(CurveTypes.values()).points;

		//transform points based of the last points
		Vector3f[] newPoints = new Vector3f[] {
				H.v2tov3fXZ(points[0]),
				H.v2tov3fXZ(points[1]),
				H.v2tov3fXZ(points[2]),
				H.v2tov3fXZ(points[3]),
			};
		
		float oldAngle = FastMath.atan2(lastPoints[3].z - lastPoints[2].z, lastPoints[3].x - lastPoints[2].x) - FastMath.HALF_PI;
		Quaternion q = new Quaternion().fromAngleAxis(-oldAngle, Vector3f.UNIT_Y); 
		for (int i = 0; i < newPoints.length; i++) {
			newPoints[i] = q.mult(newPoints[i]).add(lastPoints[3]);
			newPoints[i].y = getHeightFromBasis(newPoints[i]);
		}
		
		RoadMesh m = new RoadMesh(5, 2, Arrays.asList(newPoints));
		world.generateRoad(m);
		
		App.rally.getRootNode().attachChild(H.makeShapeArrow(App.rally.getAssetManager(), ColorRGBA.White, q.mult(new Vector3f(10,0,0)), lastPoints[3]));
		App.rally.getRootNode().attachChild(H.makeShapeArrow(App.rally.getAssetManager(), ColorRGBA.Red, newPoints[3].subtract(newPoints[0]), newPoints[0]));
		
		lastPoints = newPoints;
		
		H.p("next road point:", lastPoints[3]);
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