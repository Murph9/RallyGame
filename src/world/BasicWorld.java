package world;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

import game.App;
import game.LoadModelWrapper;

public class BasicWorld extends World {
	
	private Spatial startGeometry; 
	
	public BasicWorld() {
		super("basicWorldRoot");
	}
	
	@Override
	public WorldType getType() {
		return WorldType.BASIC;
	}
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		
		AssetManager am = app.getAssetManager();
		
		Material matfloor = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		matfloor.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		matfloor.setColor("Color", ColorRGBA.Green);
		
		Box start = new Box(10, 0.25f, 10); //Something to spawn on (or in hint hint)
		startGeometry = new Geometry("Starting Box", start);
		startGeometry.setMaterial(matfloor);
		startGeometry.setLocalTranslation(0, -0.1f, 0);
		startGeometry.addControl(new RigidBodyControl(0));
		
		startGeometry = LoadModelWrapper.create(app.getAssetManager(), startGeometry, ColorRGBA.Green);
		
		this.rootNode.attachChild(startGeometry);
		((App)this.app).getPhysicsSpace().add(startGeometry);
	}
	
	@Override
	public void reset() {
		
	}

	@Override
	public void update(float tpf) {
		//nothing
	}
	
	public void cleanup() {
		this.rootNode.detachChild(startGeometry);
		((App)this.app).getPhysicsSpace().add(startGeometry);

		super.cleanup();
	}
}
