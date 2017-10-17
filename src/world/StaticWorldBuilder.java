package world;

import java.util.ArrayList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import game.App;
import helper.H;

public class StaticWorldBuilder extends World {

	private StaticWorld world;
    private static List<RigidBodyControl> landscapes = new ArrayList<RigidBodyControl>(5);
    private static List<Spatial> models = new ArrayList<Spatial>(5); //at least 5
	
	public StaticWorldBuilder(StaticWorld world) {
		super("staticworld" + world.name);
		this.world = world;
	}
	
	public WorldType getType() {
		return WorldType.STATIC;
	}

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		addStaticWorld(true);
	}
	@Override
	public void reset() {
		//reset is a play action, so its usually resets dynamic stuff which this doesn't have
	}

	@Override
	public Vector3f getStartPos() {
		return world.start;
	}
	@Override
	public Matrix3f getStartRot() {
		return world.rot;
	}

	@Override
	public void update(float tpf) {
		//doesn't ever need to update
	}

	@Override
	public void cleanup() {
		removeStaticWorld(rootNode, App.rally.getPhysicsSpace());
		super.cleanup();
	}
	
	//because this doesn't have an empty constructor, we define it manually
	public World copy() {
		return new StaticWorldBuilder(world);
	}
	
	
	////making the world exist
	public void addStaticWorld(boolean ifShadow) {
		AssetManager as = App.rally.getAssetManager();
		
		Material mat = new Material(as, "Common/MatDefs/Misc/ShowNormals.j3md");
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		
	    //imported model
		Spatial worldNode = as.loadModel(world.name);
		if (worldNode instanceof Node) {
			for (Spatial s: ((Node) worldNode).getChildren()) {
				if (world.ifNeedsTexture) {
					s.setMaterial(mat);
				}
				addWorldModel(rootNode, App.rally.getPhysicsSpace(), s, ifShadow);
			}
		} else {
			Geometry worldModel = (Geometry) as.loadModel(world.name);
			
			if (world.ifNeedsTexture) {
				worldModel.setMaterial(mat);
			}
			addWorldModel(rootNode, App.rally.getPhysicsSpace(), worldModel, ifShadow);
		}
		
		H.e("Adding: "+ world.name);
	}
	
	private void addWorldModel(Node node, PhysicsSpace phys, Spatial s, boolean ifShadow) {
		s.scale(world.scale);
		
		CollisionShape col = CollisionShapeFactory.createMeshShape(s);
		RigidBodyControl landscape = new RigidBodyControl(col, 0);
		s.addControl(landscape);
		if (ifShadow) {
			s.setShadowMode(ShadowMode.Receive);
		}

		landscapes.add(landscape);
		models.add(s);
		
		phys.add(landscape);
		node.attachChild(s);
	}
	
	
	public void removeStaticWorld(Node node, PhysicsSpace phys) {
		for (RigidBodyControl r: landscapes) {
			phys.remove(r);
		}
		landscapes.clear();
		for (Spatial s: models) {
			node.detachChild(s);
		}
		models.clear();
	}
}
