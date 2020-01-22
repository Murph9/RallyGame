package world;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

import effects.LoadModelWrapper;

public class MovingWorld extends World {
    
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

		//still box
		startGeometry = LoadModelWrapper.create(app.getAssetManager(), 
			new Geometry("box", new Box(20, 0.25f, 20)), 
			ColorRGBA.Green);
		startGeometry.setLocalTranslation(0, 0, 0);
		startGeometry.addControl(new RigidBodyControl(0));

		this.rootNode.attachChild(startGeometry);
        getState(BulletAppState.class).getPhysicsSpace().add(startGeometry);

		//moving box
		box = LoadModelWrapper.create(app.getAssetManager(),
			new Geometry("Box", new Box(20, 0.25f, 20)),
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


	private float PLATFORM_SPEED = 5;
	private float PLATFORM_RESET_TIME = 30;
	@Override
	public void update(float tpf) {
		//always moving but not rotating (kinematic please)
		
		// box.getControl(RigidBodyControl.class).setAngularVelocity(new Vector3f(0, 0, 0));
		box.getControl(RigidBodyControl.class).setLinearVelocity(new Vector3f(0, 0, PLATFORM_SPEED));
		resetTimer += tpf;
		if (resetTimer > PLATFORM_RESET_TIME) {
			resetTimer = 0;
			box.getControl(RigidBodyControl.class).setPhysicsLocation(new Vector3f(40.1f, 0, -PLATFORM_RESET_TIME/2*PLATFORM_SPEED));
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
