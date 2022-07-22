package rallygame.car.ray;

import com.jme3.math.FastMath;

import rallygame.car.data.WheelDataTractionConst;

public class GripHelper {
	private static float ERROR = 0.0005f; //our fixed error, we don't really care how close it is past 3 or 4 decimals
	
	//http://www.gamedev.net/topic/462784-simplified-pacejka-magic-formula/

	//There were fancy versions of the Pacejka's Formula here but there were removed
	//Try the git repositiory to get them back. (it should say 'removed' in the git message)

	/** Pacejka's Formula simplified from the bottom of:.
	 * http://www.edy.es/dev/docs/pacejka-94-parameters-explained-a-comprehensive-guide/
	 * @param w Pick the lateral or longitudial version to send.
	 * @param slip Slip angle or slip ratio (it doesn't matter except for one value changes on it)
	 * @return The force expected
	 */
	public static float tractionFormula(WheelDataTractionConst w, float slip) {
		return FastMath.sin(w.C * FastMath.atan(w.B*slip - w.E * (w.B*slip - FastMath.atan(w.B*slip))));
	}
	public static float loadFormula(WheelDataTractionConst w, float load, float loadQuadratic) {
		return Math.max(0, w.D * (1 - loadQuadratic * load) * load);
	}
	public static float calcMaxLoad(WheelDataTractionConst w, float loadQuadratic) {
		return loadFormula(w, dloadFormula(w, loadQuadratic), loadQuadratic);
	}
	private static float dloadFormula(WheelDataTractionConst w, float loadQuadratic) {
		return 1 / (2f * loadQuadratic);
	}

	//returns the slip value that gives the closest to 1 from the magic formula (should be called twice, lat and long)
	public static float calcSlipMax(WheelDataTractionConst w) {
		double lastX = 0.2f; //our first guess (usually finishes about 0.25f)
		double nextX = lastX + 10*ERROR; //just so its a larger diff that error

		while (Math.abs(lastX - nextX) > ERROR) {
			lastX = nextX;
			nextX = iterate(w, lastX, ERROR);
		}
		
		if (!Double.isNaN(nextX))
			return (float)nextX;

		//attempt guess type 2 (numerical) (must be between 0 and 2)
		double max = -1;
		float pos = -1;
		for (int i = 0; i < 200; i++) {
			float value = tractionFormula(w, i/100f);
			if (value > max) {
				max = value;
				pos = i/100f;
			}
		}
		
		if (max < 0)
			return Float.NaN;
		
		return pos;
	}
	private static double iterate(WheelDataTractionConst w, double x, double error) {
		return x - ((tractionFormula(w, (float)x)-1) / dtractionFormula(w, (float)x, error)); 
		//-1 because we are trying to find a max (which happens to be 1)
	}
	private static double dtractionFormula(WheelDataTractionConst w, double slip, double error) {
		return (tractionFormula(w, (float)(slip+error)) - tractionFormula(w , (float)(slip-error)))/ (2*error);
	}
}