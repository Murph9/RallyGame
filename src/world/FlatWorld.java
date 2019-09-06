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

public class FlatWorld extends World {
	
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
		//Ideally we would mod 1000 the car position so the grounds bumps don't occur
		//but doing so would require the car, and we just don't have the power for that here
		
		Vector3f pos = getApplication().getCamera().getLocation();
		pos.y = 0;
		startGeometry.setLocalTranslation(pos);
		startGeometry.getControl(RigidBodyControl.class).setPhysicsLocation(pos);
	}
	
	@Override
	public void cleanup(Application app) {
		this.rootNode.detachChild(startGeometry);
		
		getState(BulletAppState.class).getPhysicsSpace().remove(startGeometry);

		super.cleanup(app);
	}
}
