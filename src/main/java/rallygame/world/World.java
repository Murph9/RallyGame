package rallygame.world;

import java.lang.reflect.InvocationTargetException;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Transform;
import com.jme3.scene.Node;

import rallygame.helper.Log;

public abstract class World extends BaseAppState implements IWorld {

	protected Node rootNode;

	public World(String name) {
		this.rootNode = new Node(name);
	}

	@Override
	public void initialize(Application app) {
		((SimpleApplication) app).getRootNode().attachChild(this.rootNode);
		Log.p("initialize() world: " + rootNode.getName());
	}

	@Override
	protected void onDisable() { }
	@Override
	protected void onEnable() { }

	@Override
	public Transform getStart() {
		return new Transform();
	}

	@Override
	protected void cleanup(Application app) {
		// remove everything you added, you are being removed now :(
		if (this.rootNode == null)
			Log.e(rootNode.getName() + " was cleaned up twice or never initialised, please don't do that.");

		Node realRootNode = ((SimpleApplication) app).getRootNode();
		realRootNode.detachChild(this.rootNode);

		Log.e("cleanup() world: " + rootNode.getName() + " rm(" + realRootNode.getChildren().size() + ")");
		this.rootNode = null;
	}

	@Override
	public LoadResult loadPercent() {
		return new LoadResult(1, "Loaded immediately.");
	}

	// Create another of myself, to use somewhere else
	public IWorld copy() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException {
		Class<? extends IWorld> clazz = this.getClass();
		return clazz.getDeclaredConstructor().newInstance();
	}
}
