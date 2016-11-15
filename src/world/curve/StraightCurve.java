package world.curve;

import java.util.function.BiFunction;

import com.jme3.math.Vector3f;

import game.H;

//https://en.wikipedia.org/wiki/B%C3%A9zier_curve#Linear_B.C3.A9zier_curves
public class StraightCurve implements Curve {

	private Vector3f[] nodes;
	private BSegment[] output;
	
	private BiFunction<Vector3f, Vector3f, BSegment> funct;
	
	
	StraightCurve(Vector3f[] nodes, BiFunction<Vector3f, Vector3f, BSegment> funct) {
		this.nodes = nodes;
		this.funct = funct;
	}
	
	@Override
	public BSegment[] calcPoints() {
		if (nodes == null || nodes.length != 2) {
			H.e("Incorrect starting points given, need 2");
			return null;
		}
		if (output != null){
			return output;
		}
		
		int N = 3; //start middle end
		output = new BSegment[N + 1];
		
		Vector3f g = nodes[1].subtract(nodes[0]); //derivative (happens to be constant)
		
		for (int i = 0; i < N + 1; i++) {
			float t = ((float) i)/N;
			
			Vector3f p = nodes[0].add(nodes[1].subtract(nodes[0]).mult(t)); //point
			
			output[i] = funct.apply(p, g);
		}
		
		return output;
	}

	@Override
	public Vector3f[] getNodes() {
		return nodes;
	}
}
