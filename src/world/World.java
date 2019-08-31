package world;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import helper.Log;

//Controls the physical (and visual) things in the level

public abstract class World extends BaseAppState {

	protected Node rootNode;
	
	public World (String name) {
		this.rootNode = new Node(name);
	}
	
	@Override
	public void initialize(Application app) {
		((SimpleApplication)app).getRootNode().attachChild(this.rootNode);
		Log.p("initialize() world: " + rootNode.getName());
	}

	@Override
	protected void onDisable() {
	}
	@Override
	protected void onEnable() {
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


	@Override
	protected void cleanup(Application app) {
		//remove everything you added, you are being removed now :(
		if (this.rootNode == null)
			Log.e("I was probably cleaned up twice or never, please don't.");
		
		((SimpleApplication)app).getRootNode().detachChild(this.rootNode);

		Log.e("cleanup() world: " + rootNode.getName());
		this.rootNode = null;
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
