package world;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

import car.CarBuilder;
import effects.LoadModelWrapper;

public class MovingWorld extends World {
    
    private static final float BOX_HEIGHT = 1;
    private static final float BOX_SIZE = 20;
    private static final float PLATFORM_SPEED = 5;
    private static final float PLATFORM_RESET_TIME = 30;
    
    private Spatial startGeometry;
	private Spatial box;
	
	private float resetTimer;
	
	public MovingWorld() {
		super("movingWorldRoot");
	}
	
	@Override
	public WorldType getType() {
		return WorldType.FLAT;
	}
	
	@Override
	public void initialize(Application app) {
		super.initialize(app);

        getState(CarBuilder.class).get(0).setPhysicsLocation(new Vector3f(0, BOX_HEIGHT + 0.5f, 0));

		//still box
		startGeometry = LoadModelWrapper.create(app.getAssetManager(), 
			new Geometry("box", new Box(BOX_SIZE, BOX_HEIGHT, BOX_SIZE)), 
			ColorRGBA.Green);
		startGeometry.setLocalTranslation(0, 0, 0);
		startGeometry.addControl(new RigidBodyControl(0));

		this.rootNode.attachChild(startGeometry);
        getState(BulletAppState.class).getPhysicsSpace().add(startGeometry);

		//moving box
		box = LoadModelWrapper.create(app.getAssetManager(),
			new Geometry("Box", new Box(BOX_SIZE, BOX_HEIGHT, BOX_SIZE)),
			ColorRGBA.Cyan);
		
		RigidBodyControl rbc = new RigidBodyControl(10000);
		box.addControl(rbc);
		rbc.setGravity(new Vector3f());
        rootNode.attachChild(box);

		getState(BulletAppState.class).getPhysicsSpace().add(box);
		
		resetTimer = Float.MAX_VALUE;
	}
	
	@Override
	public void reset() {
		
	}

	@Override
	public void update(float tpf) {
        RigidBodyControl control = box.getControl(RigidBodyControl.class);

		//always moving but not rotating much
		control.setAngularVelocity(new Vector3f(0, 0, 0));
		control.setLinearVelocity(new Vector3f(0, 0, PLATFORM_SPEED));
		resetTimer += tpf;
		if (resetTimer > PLATFORM_RESET_TIME) {
			resetTimer = 0;
            control.setPhysicsLocation(new Vector3f(BOX_SIZE * 2 + 0.1f, 0, -PLATFORM_RESET_TIME / 2 * PLATFORM_SPEED));
            control.setPhysicsRotation(new Quaternion());
		}
	}
	
	@Override
	public void cleanup(Application app) {
		this.rootNode.detachChild(startGeometry);
		getState(BulletAppState.class).getPhysicsSpace().remove(startGeometry);

        this.rootNode.detachChild(box);
        getState(BulletAppState.class).getPhysicsSpace().remove(box);

		super.cleanup(app);
	}
}
