package world;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;

public interface World {

	WorldType getType(); //type of world
	
	boolean isInit(); //have we called init
	Node init(PhysicsSpace space, ViewPort view); //actually adds it to the scene
	Node getRootNode(); //the rootnode that we can add to the global space
	
	Vector3f getWorldStart(); //player start pos
	Matrix3f getWorldRot(); //and rotation
	void update(float tpf, Vector3f playerPos, boolean force); //update the world if it needs
	
	void reset(); //reset anything the player did, like reseting dynamically generated things
	void cleanup(); //remove everything you added, you are being removed now :(
	
	//AI things:
	Vector3f getNextPieceClosestTo(Vector3f pos);
}
