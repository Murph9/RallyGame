package world;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

import effects.LoadModelWrapper;
import game.App;

public class StaticWorldBuilder extends World {

	private StaticWorld world;
    private RigidBodyControl landscape;
    private Spatial model;
	
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
		AssetManager am = app.getAssetManager();

		Material mat = new Material(am, "Common/MatDefs/Misc/ShowNormals.j3md");
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);

		// imported model
		model = LoadModelWrapper.create(am, world.name, ColorRGBA.White);
		model.scale(world.scale);

		CollisionShape col = CollisionShapeFactory.createMeshShape(model);
		landscape = new RigidBodyControl(col, 0);
		model.addControl(landscape);

		((App) this.app).getPhysicsSpace().add(landscape);
		rootNode.attachChild(model);
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
		PhysicsSpace phys = ((App)this.app).getPhysicsSpace();

		phys.remove(landscape);
		landscape = null;

		rootNode.detachChild(model);
		model = null;

		super.cleanup();
	}
	
	//because this doesn't have an empty constructor, we define it manually
	public World copy() {
		return new StaticWorldBuilder(world);
	}
}
