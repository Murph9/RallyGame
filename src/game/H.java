package game;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;


//Its short for help, if the name was any longer it might not actually be helpful
public class H {

	/**
	 * Easier way of typing System.out.println();
	 * @param o The thing you wanted printed
	 */
	public static void p(Object o) {
		System.out.println(o);
	}
	
	public static Vector3f clamp(Vector3f v, double value) {
		float length = v.length();
		Vector3f newV = new Vector3f(v);
		if (length > Math.abs(value)) {
			newV.normalizeLocal().multLocal((float)value);
		}
		return newV;
	}
	
	public static float lerpArray(float i, float[] array) {
		if (i < 0) i = 0; //prevent array issues
		int whole = (int) i;
		float rem = i - whole;
		float low = array[clamp(whole, 0, array.length-1)];
		float high = array[clamp(whole+1, 0, array.length-1)];
		return FastMath.interpolateLinear(rem, low, high);
	}
	
	public static float lerpTorqueArray(int rpm, float[] array) {
		if (rpm < 0) rpm = 0; //prevent negative torque issues
		
		int intrpm = (rpm / 1000); //basically divide by 1000 and round down
		float remrpm = (float)(rpm % 1000)/1000;
		float before = array[clamp(intrpm, 0, array.length-1)];
		float after = array[clamp(intrpm+1, 0, array.length-1)];
		return FastMath.interpolateLinear(remrpm, before, after);
	}
	
	public static int clamp(int input, int low, int high) {
		if (input < low) input = low;
		if (input > high) input = high;
		return input;
	}
	
	public static float cylinderInertia(float r, float mass) {
		return mass*r*r/2;
	}
}
