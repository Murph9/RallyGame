package rallygame.world;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

import rallygame.car.CarBuilder;
import rallygame.car.ray.RayCarControl;
import rallygame.effects.LoadModelWrapper;

public class FlatWorld extends World {
    
    private static final float RESET_DISTANCE = 1000;
	private Spatial startGeometry;
	
	public FlatWorld() {
		super("flatWorldRoot");
	}
	
	@Override
	public WorldType getType() {
		return WorldType.FLAT;
	}
	
	@Override
	public void initialize(Application app) {
		super.initialize(app);

		startGeometry = LoadModelWrapper.create(app.getAssetManager(), 
			new Geometry("box", new Box(20, 0.25f, 20)), 
			ColorRGBA.Green);
		startGeometry.setLocalTranslation(0, -0.1f, 0);
		startGeometry.addControl(new RigidBodyControl(0));

		this.rootNode.attachChild(startGeometry);
		getState(BulletAppState.class).getPhysicsSpace().add(startGeometry);
	}
	
	@Override
	public void reset() {
		
	}

	@Override
	public void update(float tpf) {
        RayCarControl car = getState(CarBuilder.class).get(0);
        if (car != null) {
            Vector3f pos = car.location.clone();

            // We reset the cars position so the large floating point errors don't occur
            if (pos.length() > RESET_DISTANCE) {
                pos.x = 0;
                pos.z = 0;
                car.setPhysicsProperties(pos, null, null, null);
            }

            pos.y = 0;
            startGeometry.setLocalTranslation(pos.clone());
            startGeometry.getControl(RigidBodyControl.class).setPhysicsLocation(pos.clone());
        }
	}
	
	@Override
	public void cleanup(Application app) {
		this.rootNode.detachChild(startGeometry);
		
		getState(BulletAppState.class).getPhysicsSpace().remove(startGeometry);

		super.cleanup(app);
	}
}
