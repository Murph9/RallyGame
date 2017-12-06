package world.highway;

import java.util.HashMap;
import java.util.Map;

import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.FastMath;
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
import terrainWorld.TerrainChunk;
import terrainWorld.TileListener;

public class TreeMaker implements TileListener {

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

	private InstancedNode treeNode = new InstancedNode("tree node");
	private Map<Vector3f, Node> treeNodes;
	
	public TreeMaker(HighwayWorld world) {
		this.world = world;
		this.treeNodes = new HashMap<Vector3f, Node>();
		
		this.treeNode.setShadowMode(ShadowMode.CastAndReceive);
		App.rally.getRootNode().attachChild(this.treeNode);
		
		for (int i = 0; i < Tree_Strings.length; i++) {
			Spatial spat = App.rally.getAssetManager().loadModel(Tree_Strings[i]);
			if (spat instanceof Node) {
				for (Spatial s: ((Node) spat).getChildren()) {
					this.treeGeoms[i] = s;
				}
			} else {
				this.treeGeoms[i] = (Geometry) spat;
			}
		}
	}

	@Override
	public boolean tileLoaded(TerrainChunk terrainChunk) {
		//spawn trees on it
		Vector3f pos = terrainChunk.getLocalTranslation();
		if (this.treeNodes.containsKey(pos)) {
			Node n = this.treeNodes.get(pos);
			if (n == null) {
				H.e("no trees with vector: ", terrainChunk.getLocalTranslation(), " :(");
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
				
				float height = terrainChunk.getHeight(new Vector2f(x,z));
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
		return true;
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
	public void tileLoadedThreaded(TerrainChunk terrainChunk) { }
	@Override
	public String imageHeightmapRequired(int x, int z) { return null; }

	public void cleanup() {
		App.rally.getRootNode().detachChild(treeNode);
	}
}
