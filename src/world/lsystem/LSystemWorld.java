package world.lsystem;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

import game.App;
import world.World;
import world.WorldType;

//TODO thinking about creating an l-system road network
//Main notes: http://www.tmwhere.com/city_generation.html
//Other: https://www.reddit.com/r/gamedev/comments/19ic3j/procedural_content_generation_how_to_generate/

public class LSystemWorld extends World {

	private LRoadGenerator lrg;
	
	public LSystemWorld() {
		super(LSystemWorld.class.getSimpleName());
	}
	
	@Override
	public WorldType getType() {
		return WorldType.LSYSTEM;
	}
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		placeLargeFlatBox(app);
		
		lrg = new LRoadGenerator(rootNode, 0.2f);
		lrg.init();
	}
	
	private void placeLargeFlatBox(Application app) {
		Material matfloor = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		matfloor.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		matfloor.setColor("Color", ColorRGBA.Green);
		
		Box start = new Box(10, 0.25f, 10);
		Geometry startBox = new Geometry("Starting Box", start);
		startBox.setMaterial(matfloor);
		startBox.setLocalTranslation(0, -0.25f, 0);
		startBox.addControl(new RigidBodyControl(0));
		
		this.rootNode.attachChild(startBox);
		App.CUR.getPhysicsSpace().add(startBox);
	}

	@Override
	public Vector3f getStartPos() {
		return new Vector3f(0,0.5f,0);
	}
	@Override
	public void update(float tpf) {
		if (!isEnabled())
			return;
		
		if (lrg != null) 
			lrg.update(tpf);
	}
	@Override
	public void reset() { }
	
	@Override
	public void cleanup() {
		super.cleanup();
		try {
			throw new Exception("not implemented");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
