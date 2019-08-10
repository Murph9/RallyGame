package world;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import game.App;
import helper.Log;

//Controls the physical (and visual) things in the level

public abstract class World extends AbstractAppState {

	protected SimpleApplication app;
	protected Node rootNode;
	
	public World (String name) {
		this.rootNode = new Node(name);
	}
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		this.app = (SimpleApplication)app;
		
		this.app.getRootNode().attachChild(this.rootNode);
		Log.e("initialize() world: " + rootNode.getName());
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
			Log.e("I was probably cleaned up twice or never, please don't.");

		this.initialized = false;
		app = null;
		
		App.CUR.getRootNode().detachChild(this.rootNode);
		
		Log.e("cleanup() world: " + rootNode.getName());
		this.rootNode = null;
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
