package world.wp;

import java.util.ArrayList;
import java.util.List;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public enum Valley implements WP {
	STRAIGHT("straight.blend", new Vector3f(25,0,0), WP.STRIAGHT),
	STRAIGHT_DOWN("straight_down.blend", new Vector3f(20,-1.33697f, 0), WP.DOWN_8),
	STRAIGHT_UP("straight_up.blend", new Vector3f(20,1.33697f, 0), WP.UP_8),
	
	RIGHT("right.blend", new Vector3f(12.94095f,0,1.70371f), WP.RIGHT_15),
	LEFT("left.blend", new Vector3f(12.94095f,0,-1.70371f), WP.LEFT_15),
	
//	TUNNEL("tunnel.blend", new Vector3f(10f, 0,0), WP.STRIAGHT)
	;
	
	private static String dir = "assets/wb/valley/";
	
	String name;
	Vector3f newPos; //what the piece does to the track
	Quaternion newRot; //change of angle (deg) for the next piece
	NodeType startNode;
	NodeType endNode;
	
	Valley(String s, Vector3f a, Quaternion g) {
		this.name = s;
		this.newPos = a;
		this.newRot = g;
		this.startNode = NodeType.A;
		this.endNode = NodeType.A;
	}
	
	public float getScale() { return 2; }
	public boolean needsMaterial() { return false; }
	
	public String getName() { return dir+name; }
	public Vector3f getNewPos() { return new Vector3f(newPos); }
	public Quaternion getNewAngle() { return new Quaternion(newRot); }
	
	public NodeType startNode() {
		return startNode;
	}
	public NodeType endNode() {
		return endNode;
	}
	
	//TODO textures
	
	static class Builder extends DefaultBuilder {
		Builder() {
			super(Valley.values());
		}
		
		protected void selectNewPiece() {
			//TODO
			List<WPObject> wpoList = new ArrayList<>();
			for (WPObject w: wpos) {
				if (nextNode == null || nextNode == w.wp.startNode()) {
					
					if (isAngleGood(w))
						wpoList.add(w);
				}
			}
			
			if (wpoList.isEmpty()) { 
				try {
					throw new Exception("No pieces with the node start " + nextNode.name() + " found.");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			int i = (int)(Math.random()*wpoList.size());
			WPObject wpo = wpoList.get(i);
			
			int count = 0;
			while (!PlacePiece(wpo)) {
				i = (int)(Math.random()*wpoList.size()); //so select a new one
				wpo = wpoList.get(i);
				count++;
				if (count > 100) {
					break; //please no loops huh?
				}
			}
		}
		
		private boolean isAngleGood(WPObject wpo) {
			float angle = angleFromHorizontal(nextRot.mult(wpo.wp.getNewAngle()));
//			H.p(angle);
			return FastMath.abs(angle) < FastMath.DEG_TO_RAD*9; //they happen to angle up/down at 8'
//			return FastMath.abs(angle) < FastMath.QUARTER_PI;
		}
		
		private float angleFromHorizontal(Quaternion q) {
			Vector3f res = q.mult(new Vector3f(0,1,0)); //still somehow gets wonky, probably not checking enough axis
			//should probably not care about what xy direction im going along the plane, should only deal with the z changes 
			return FastMath.tan(res.x/res.y);
		}
	}
}
