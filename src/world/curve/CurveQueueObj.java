package world.curve;

import com.jme3.math.FastMath;

public class CurveQueueObj implements Comparable<CurveQueueObj> {

	float time; //when it should be added (randomish)
	Curve curve; //actual data to place with
	String rule; //probably contains the split instructions for the next elements
	
	public CurveQueueObj(float t, Curve curve, String rule) {
		this.time = t;
		this.curve = curve;
		this.rule = rule;
	}
	
	@Override
	public int compareTo(CurveQueueObj arg0) {
		return (int) FastMath.sign(this.time - arg0.time);
	}
}
