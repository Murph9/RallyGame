package world;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import game.App;
import helper.H;

//Controls the physical (and visual) things in the level

public abstract class World extends AbstractAppState {

	protected Application app;
	protected Node rootNode;
	
	public World (String name) {
		this.rootNode = new Node(name);
	}
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		this.app = app;
		
		App.rally.getRootNode().attachChild(this.rootNode);
	}

	//'Starts again'
	public abstract void reset(); //reset anything the player did, like reseting dynamically generated things
	//update the world if it needs
	public abstract void update(float tpf);
	//type of world
	public abstract WorldType getType(); 

	//player start pos
	public Vector3f getStartPos() { return new Vector3f(); }
	//player rotation
	public Matrix3f getStartRot() { return new Matrix3f(Matrix3f.IDENTITY); }

	public void cleanup() {
		//remove everything you added, you are being removed now :(
		if (this.rootNode == null)
			H.e("I was probably cleaned up twice or never, please don't.");
		
		App.rally.getRootNode().detachChild(this.rootNode);
		this.rootNode = null;
		
		this.initialized = false;
		app = null;
		
		super.cleanup();
	}
	
	//AI things:
	public Vector3f getNextPieceClosestTo(Vector3f pos) {
		return null;
	}
	
	//Create another of myself, to use somewhere else
	public World copy() throws InstantiationException, IllegalAccessException {
		Class<? extends World> clazz = this.getClass();
		return clazz.newInstance();
	}
}
