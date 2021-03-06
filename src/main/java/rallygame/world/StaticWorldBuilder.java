package rallygame.world;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.Transform;
import com.jme3.scene.Spatial;

import rallygame.effects.LoadModelWrapper;

public class StaticWorldBuilder extends World {

	protected StaticWorld world;
    protected RigidBodyControl landscape;
    private Spatial model;
	
	public StaticWorldBuilder(StaticWorld world) {
		super("staticworld type: " + world.name);
		this.world = world;
	}
	
	public WorldType getType() {
		return WorldType.STATIC;
	}

	@Override
	public void initialize(Application app) {
        super.initialize(app);
        
		// imported model
		model = LoadModelWrapper.create(app.getAssetManager(), world.name);
		model.scale(world.scale);

		CollisionShape col = CollisionShapeFactory.createMeshShape(model);
		landscape = new RigidBodyControl(col, 0);
		model.addControl(landscape);

		getState(BulletAppState.class).getPhysicsSpace().add(landscape);
        rootNode.attachChild(model);
	}

    @Override
    public void reset() {
        // reset is a play action, so its usually resets dynamic stuff which this doesn't have
    }

	@Override
	public Transform getStart() {
		return new Transform(world.start, world.rot);
	}
    
	@Override
	public void cleanup(Application app) {
		PhysicsSpace phys = getState(BulletAppState.class).getPhysicsSpace();

		phys.remove(landscape);
		landscape = null;

		rootNode.detachChild(model);
		model = null;

		super.cleanup(app);
	}

    public StaticWorld getTypeForDriveRace() {
        return this.world;
    }
	
	// this doesn't have an empty constructor so we define it manually
	public IWorld copy() {
		return new StaticWorldBuilder(world);
	}
}
