package rallygame.world.track;

import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.instancing.InstancedNode;
import com.jme3.terrain.geomipmap.TerrainQuad;

import rallygame.game.App;
import rallygame.helper.H;
import jme3tools.optimize.GeometryBatchFactory;

public class TreeTrackHelper {

	private static final String[] Tree_Strings = new String[] {
			"objects/tree_0.blend.glb",
			"objects/tree_1.blend.glb",
			"objects/tree_2.blend.glb",
			"objects/tree_3.blend.glb",
			"objects/tree_4.blend.glb",
			"objects/tree_5.blend.glb",
			"objects/tree_6.blend.glb",
		};
	private final Spatial treeGeoms[] = new Spatial[Tree_Strings.length];
	
	private App app;
	private final TerrainQuad world;
	private float worldSize;
	private final int treeCount;
	public TreeTrackHelper(App app, TerrainQuad world, int worldSize, int treeCount) {
		this.app = app;
		this.world = world;
		this.treeCount = treeCount;
		this.worldSize = worldSize*world.getLocalScale().x;
		for (int i = 0; i < Tree_Strings.length; i++) {
			Spatial spat = app.getAssetManager().loadModel(Tree_Strings[i]);
			if (spat instanceof Node) {
				for (Spatial s: ((Node) spat).getChildren()) {
					this.treeGeoms[i] = s;
				}
			} else {
				this.treeGeoms[i] = (Geometry) spat;
			}
		}
	}
	
	public Node getTreeNode() {
		InstancedNode node = new InstancedNode("trees");
		for (int i = 0; i < treeCount; i++) {
			Vector3f pos = H.randV3f(worldSize, true);
			float height = world.getHeight(new Vector2f(pos.x, pos.z));
			if (Float.isNaN(height) || height == 0)
				continue; //might be less than treeCount
			
			Spatial s = treeGeoms[FastMath.nextRandomInt(0, treeGeoms.length-1)].clone();
			
			Vector3f newPos = new Vector3f(pos.x, height, pos.z);
			s.setLocalTranslation(newPos);
			s.addControl(new RigidBodyControl(0));
			node.attachChild(s);
			this.app.getPhysicsSpace().add(s);
		}
		GeometryBatchFactory.optimize(node);
		
		return node;
	}
}