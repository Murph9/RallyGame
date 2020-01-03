package world;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import helper.Log;

public abstract class World extends BaseAppState implements IWorld, IWorldPath {

	protected Node rootNode;
	
	public World(String name) {
        this.rootNode = new Node(name);
    }
	
	@Override
	public void initialize(Application app) {
		((SimpleApplication)app).getRootNode().attachChild(this.rootNode);
		Log.p("initialize() world: " + rootNode.getName());
	}

	@Override
	protected void onDisable() { }
	@Override
	protected void onEnable() { }

	public Vector3f getStartPos() { return new Vector3f(); }
	public Matrix3f getStartRot() { return new Matrix3f(Matrix3f.IDENTITY); }
    public Vector3f[] getPath() { return new Vector3f[0]; }

	@Override
	protected void cleanup(Application app) {
		//remove everything you added, you are being removed now :(
		if (this.rootNode == null)
			Log.e(rootNode.getName() + " was cleaned up twice or never initialised, please don't do that.");
		
		((SimpleApplication)app).getRootNode().detachChild(this.rootNode);

		Log.e("cleanup() world: " + rootNode.getName());
		this.rootNode = null;
	}
	
	// Simpler AI call
	public Vector3f getNextPieceClosestTo(Vector3f pos) {
		return null;
	}
}
