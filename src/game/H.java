package game;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;


//Its short for help, if the name was any longer it might not actually be helpful
public class H {

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
		float low = H.niceArray(whole, array);
		float high = H.niceArray(whole+1, array);
		return FastMath.interpolateLinear(rem, low, high);
	}
	
	public static float lerpTorqueArray(int rpm, float[] array) {
		if (rpm < 0) rpm = 0; //prevent negative torque issues
		
		int intrpm = (rpm / 1000); //basically divide by 1000 and round down
		float remrpm = (float)(rpm % 1000)/1000;
		float before = H.niceArray(intrpm, array);
		float after = H.niceArray(intrpm+1, array);
		return FastMath.interpolateLinear(remrpm, before, after);
	}
	
	public static float niceArray(int i, float[] array) {
		if (i < 0) i = 0;
		if (i > array.length-1) i = array.length-1; 
		return array[i];
	}
	
	public static float cylinderInertia(float r, float mass) {
		return mass*r*r/2;
	}
}
