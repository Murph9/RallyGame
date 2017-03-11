package world;

import com.jme3.app.state.AbstractAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;

public abstract class World extends AbstractAppState {

	protected boolean isInit;
	protected Node rootNode;
	protected PhysicsSpace phys;
	
	public abstract WorldType getType(); //type of world
	
	public boolean isInit() {
		return isInit;
		//have we called init
	}
	public abstract Node init(PhysicsSpace space, ViewPort view); //actually adds it to the scene
	
	public Node getRootNode() {
		//the rootnode that we can add to the global space
		return rootNode;
	}
	
	public Vector3f getWorldStart() { //player start pos
		return new Vector3f();
	}
	public Matrix3f getWorldRot() { //and rotation
		return new Matrix3f(Matrix3f.IDENTITY);
	} 
	public abstract void update(float tpf, Vector3f playerPos, boolean force); //update the world if it needs
	
	public abstract void reset(); //reset anything the player did, like reseting dynamically generated things
	public abstract void cleanup(); //remove everything you added, you are being removed now :(
	
	//AI things:
	public Vector3f getNextPieceClosestTo(Vector3f pos) {
		return null;
	}
}
