package world;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/** World Piece
 * @author Jake
 */
public interface WP {
	
	enum NodeType {
		A, B, C, D, E, F, G, H; //for defining what can connect to the end of the previous piece
	}
	
	static final Quaternion
		STRIAGHT = Quaternion.IDENTITY,
		LR_180 = new Quaternion(0, 1, 0, 0),
		
		LEFT_90 = new Quaternion(0, 0.7071f, 0, 0.7071f),
		LEFT_45 = new Quaternion(0, 0.3827f, 0, 0.9239f),

		RIGHT_90 = new Quaternion(0, -0.7071f, 0, 0.7071f),
		RIGHT_45 = new Quaternion(0, -0.3827f, 0, 0.9239f),
		RIGHT_135 = new Quaternion(0, -0.9239f, 0, 0.3827f)
	;
	
	//shouldn't change per model in the set
	float getScale(); //TODO scaling screws with blender piece collision
	boolean needsMaterial();
	//boolean canMirror(); //TODO if you are ever thinking of trying not to make the inverses dont.
	
	//change per model
	String getName();
	Vector3f getNewPos(); //what the piece does to the track
	Quaternion getNewAngle(); //change of angle (deg) for the next peice
	
	NodeType startNode();
	NodeType endNode();
}
