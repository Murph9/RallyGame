package world;

import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import effects.LoadModelWrapper;

public class StaticWorldBuilder extends World {

	private StaticWorld world;
    private RigidBodyControl landscape;
    private Spatial model;

    private Vector3f[] path;
	
	public StaticWorldBuilder(StaticWorld world) {
		super("staticworld-" + world.name);
		this.world = world;
	}
	
	public WorldType getType() {
		return WorldType.STATIC;
	}

	@Override
	public void initialize(Application app) {
        super.initialize(app);
        
		// imported model
		model = LoadModelWrapper.create(app.getAssetManager(), world.name, null);
		model.scale(world.scale);

		CollisionShape col = CollisionShapeFactory.createMeshShape(model);
		landscape = new RigidBodyControl(col, 0);
		model.addControl(landscape);

		getState(BulletAppState.class).getPhysicsSpace().add(landscape);
        rootNode.attachChild(model);
        

        //attempt to read checkpoints from model (move to another class please)
        if (model instanceof Node) {
            List<Vector3f> _checkpoints = new LinkedList<Vector3f>();
			Spatial s = ((Node) model).getChild(0);
			for (Spatial points: ((Node)s).getChildren()) {
				if (points.getName().equals("Points")) {
					for (Spatial checkpoint: ((Node)points).getChildren()) {
						_checkpoints.add(checkpoint.getLocalTranslation());
					}
				}
            }
            if (!_checkpoints.isEmpty()) {
                this.path = new Vector3f[_checkpoints.size()];
                _checkpoints.toArray(this.path);
            }
        }
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
    public Vector3f[] getPath() {
        if (this.path == null)
            return super.getPath();
        return this.path;
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
	
	// this doesn't have an empty constructor so we define it manually
	public World copy() {
		return new StaticWorldBuilder(world);
	}
}
