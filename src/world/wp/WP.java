package world.wp;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import world.World;

/** World Piece
 * @author Jake
 */
public interface WP {
	
	enum NodeType {
		A, B, C, D, E, F, G, H; //for defining what can connect to the end of the previous piece
	}

	//Add a type here when you want one
	public enum DynamicType {
		Simple(new Simple.Builder()),
		Simple2(new Simple2.Builder()),
		Floating(new Floating.Builder()),
		City(new City.Builder()),
		Cliff(new Cliff.Builder()),
		Track(new Track.Builder()),
		Underground(new Underground.Builder()),
		Valley(new Valley.Builder()),
		;
		
		private World builder;
		DynamicType(World db) {
			this.builder = db;
		}
		
		public World getBuilder(/*PhysicsSpace space, ViewPort view*/) {
			builder.reset(); //just incase its already been used, physics space is probably wrong for example
			//builder.init(space, view);
			return builder;
		}
	}
	
	static final Quaternion
		STRIAGHT = Quaternion.IDENTITY,
		BACK = new Quaternion(0, 1, 0, 0),
		
		LEFT_15 = new Quaternion(0.0f, 0.13052f, 0.0f, 0.99144f),
		LEFT_45 = new Quaternion(0, 0.3827f, 0, 0.9239f),
		LEFT_90 = new Quaternion(0, 0.7071f, 0, 0.7071f),
		
		RIGHT_15 = new Quaternion(0.0f, -0.13052f, 0.0f, 0.99144f),
		RIGHT_45 = new Quaternion(0, -0.3827f, 0, 0.9239f),
		RIGHT_90 = new Quaternion(0, -0.7071f, 0, 0.7071f),
		RIGHT_135 = new Quaternion(0, -0.9239f, 0, 0.3827f),
		
		DOWN_8 = new Quaternion(0.0f, 0.0f, -0.06975647f, 0.9975641f),
		UP_8 = new Quaternion(0.0f, 0.0f, 0.06975647f, 0.9975641f)
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
