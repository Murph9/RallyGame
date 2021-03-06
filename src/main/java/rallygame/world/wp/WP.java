package rallygame.world.wp;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

/** World Piece
 * @author murph9
 */
public interface WP {
	
	enum NodeType {
		A, B, C, D, E, F, G, H; //for defining what can connect to the end of the previous piece
	}
	
	static final String FileFormat = "%s%s.blend.glb";
	static final Quaternion
		STRAIGHT = new Quaternion(0, 0, 0, 1),
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
		Rocks(new Rocks.Builder()),
		MainRoad(new MainRoad.Builder())
		;
		
		private DefaultBuilder builder;
		DynamicType(DefaultBuilder db) {
			this.builder = db;
		}
		
		public DefaultBuilder getBuilder() {
			builder.reset(); //just incase its already been used, physics space is probably wrong for example
			return builder.copy();
		}
	}
	
	//shouldn't change per model in the set
	float getScale();
	//boolean canMirror(); //This is commented out so if you are ever thinking of trying to make the inverses: DO NOT - [5]hrs wasted so far
	
	//change per model
	String getName();
	Vector3f getNewPos(); //what the piece does to the track
	Quaternion getNewAngle(); //change of angle (deg) for the next peice
	
	NodeType startNode();
	NodeType endNode();
}
