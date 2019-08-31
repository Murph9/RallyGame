package world;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

import effects.LoadModelWrapper;

public class FlatWorld extends World {
	
	private Spatial startGeometry;
	private Camera cam;  
	
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
		cam = app.getCamera();

		AssetManager am = app.getAssetManager();
		
		Material matfloor = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		matfloor.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		matfloor.setColor("Color", ColorRGBA.Green);
		
		Box start = new Box(20, 0.25f, 20);
		startGeometry = new Geometry("box", start);
		startGeometry.setMaterial(matfloor);
		startGeometry.setLocalTranslation(0, -0.1f, 0);
		startGeometry.addControl(new RigidBodyControl(0));
		startGeometry = LoadModelWrapper.create(app.getAssetManager(), startGeometry, ColorRGBA.Green);
		
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
		
		Vector3f pos = cam.getLocation();
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
