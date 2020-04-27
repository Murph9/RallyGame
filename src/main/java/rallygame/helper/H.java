package rallygame.helper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

/**Its short for help, name length was a concern*/
public class H {

	public static String str(Object[] ol, String sep) {
		if (ol == null) return null;
        if (sep == null) sep = ",";
        String str = "";
		for (Object o: ol)
			str += sep + o;
		return str.substring(sep.length());
	}
	public static String str(Iterable<Object> ol, String sep) {
        if (ol == null) return null;
        if (sep == null) sep = ",";
		String str = "";
		for (Object o: ol)
			str += sep + o.toString();
		return str.substring(sep.length());
	}
	
	public static String join(Object... os) {
		return str(os, ",");
	}
    
    
    public static <T> Boolean oneTrue(Function<T, Boolean> f, List<T> ts) {
        for (T t : ts)
            if (f.apply(t))
                return true;
        return false;
	}
	@SafeVarargs
	public static <T> Boolean oneTrue(Function<T, Boolean> f, T... ts) {
		return oneTrue(f, Arrays.asList(ts));
	}
	public static <T> Boolean allTrue(Function<T, Boolean> f, List<T> ts) {
		for (T t : ts)
			if (!f.apply(t))
				return false;
		return true;
	}
	@SafeVarargs
	public static <T> Boolean allTrue(Function<T, Boolean> f, T... ts) {
		return allTrue(f, Arrays.asList(ts));
	}
    
    public static float skew(float input, float min, float max, float outMin, float outMax) {
        float v = (input - min) / (max - min);
        return (v + outMin) * (outMax - outMin);
    }

	public static Vector3f clamp(Vector3f v, float value) {
		float length = v.length();
		Vector3f newV = new Vector3f(v);
		if (length > Math.abs(value)) {
			newV.normalizeLocal().multLocal(value);
		}
		return newV;
	}

	public static float lerpArray(float i, float[] array) {
		if (i < 0) i = 0; //prevent array issues
		int whole = (int) i;
		float rem = i - whole;
		float low = array[clamp(whole, 0, array.length-1)]; //clamp to the the end of the array to prevent an index exeception
		float high = array[clamp(whole+1, 0, array.length-1)];
		return FastMath.interpolateLinear(rem, low, high);
	}
		
	public static int clamp(int input, int low, int high) {
		return Math.max(low, Math.min(input, high)); //low <= input <= high
	}
	
	public static float cylinderInertia(float r, float mass) {
		return mass*r*r/2;
	}

	public static String substringBeforeFirst(String s, char c) {
		if (s.lastIndexOf(c) == -1)
			return s;
		return s.substring(0, s.indexOf(c));
	}
	public static String substringAfterLast(String s, char c) {
		if (s.lastIndexOf(c) == -1)
			return s;
		return s.substring(s.lastIndexOf(c) + 1);
	}
	
	public static String leftPad(String str, int length, char pad) {
		StringBuilder sb = new StringBuilder();

		for (int toPrepend = length - str.length(); toPrepend > 0; toPrepend--) {
			sb.append(pad);
		}

		sb.append(str);
		return sb.toString();
	}

    public static String decimalFormat(float num, String format) {
        return new DecimalFormat(format).format(num);
    }
	public static String roundDecimal(float num, int places) {
		if (places <= 0) {
			return Integer.toString(Math.round(num));
		}
		return String.format("%."+places+"f", num);
	}
	public static String roundDecimal(double num, int places) { //...
		return roundDecimal((float)num,places);
    }
	public static String round3f(Vector3f vec, int places) {
		if (vec == null)
			return "x:?, y:?, z:?";
		return "x:"+H.roundDecimal(vec.x, places) + ", y:"+H.roundDecimal(vec.y, places)+", z:"+H.roundDecimal(vec.z, places);
	}
	
	
	/** copies to first array */
	public static void addTogether(float[] a, float[] b) {
		if (a.length != b.length) {
			Log.e("Arrays not the same length: " + a.length + " and " + b.length);
			return;
		}
		for (int i = 0; i < a.length; i++) {
			a[i] += b[i];
		}
	}
	/** copies to new array */
	public static float[] addTogetherNew(float[] a, float[] b) {
		if (a.length != b.length) {
			Log.e("Arrays not the same length: " + a.length + " and " + b.length);
			return null;
		}
		float[] out = new float[a.length];
		System.arraycopy(a, 0, out, 0, a.length);
		for (int i = 0; i < a.length; i++) {
			out[i] += b[i];
		}
		return out;
	}
	
	
	
	public static Vector2f v3tov2fXZ(Vector3f v) {
		return new Vector2f(v.x, v.z);
	}
	public static Vector3f v2tov3fXZ(Vector2f v) {
		return new Vector3f(v.x, 0, v.y);
	}
	
	/**Generate a random Vector2f([0,1), [0,1))
	 * scaleNegative for [-1, 1) parts
	 */
	public static Vector2f randV2f(float max, boolean scaleNegative) {
		float offset = scaleNegative ? max : 0;
		float scale = scaleNegative ? 2 : 1;
		return new Vector2f(FastMath.nextRandomFloat() * scale * max - offset, FastMath.nextRandomFloat() * scale * max - offset);
	}
	
	/**Generate a random Vector3f([0,1), [0,1), [0,1))
	 * scaleNegative for [-1, 1) parts
	 */
	public static Vector3f randV3f(float max, boolean scaleNegative) {
		float offset = scaleNegative ? max : 0;
		float scale = scaleNegative ? 2 : 1;
		return new Vector3f(FastMath.nextRandomFloat()*scale*max-offset, FastMath.nextRandomFloat()*scale*max-offset, FastMath.nextRandomFloat()*scale*max-offset);
	}
	public static <T> T randFromArray(T[] array) {
		return array[FastMath.nextRandomInt(0, array.length-1)];
    }
    public static <T> T randFromList(List<T> list) {
        return list.get(FastMath.nextRandomInt(0, list.size() - 1));
    }
	
	
	public static Map<String, Object> toMap(Object obj) {
        if (obj == null) return null;
        Field[] fields = obj.getClass().getFields();
        Map<String, Object> map = new HashMap<String, Object>();
        try {
        	for (Field f : fields)
        		map.put(f.getName(), f.get(obj));
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
        return map;
	}
    
    public static float minIn(float ...array) {
        return minInArray(array);
    }
    public static float minInArray(float[] array) {
        float result = Float.POSITIVE_INFINITY;
        for (float value : array)
            result = Math.min(result, value);
        return result;
    }
    public static float maxInArray(float[] array) {
		float result = Float.NEGATIVE_INFINITY;
		for (float value: array)
			result = Math.max(result, value);
		return result;
	}
	public static float maxInArray(float[] array, BiFunction<Float, Integer, Float> func) {
		float result = Float.NEGATIVE_INFINITY;
		for (int i = 0; i < array.length; i++)
			result = Math.max(result, func.apply(array[i], i));
		return result;
    }
    
    public static void writeToFile(String data, String filePath) {
        File file = new File(filePath);
        if (!file.exists())
            try {
                file.createNewFile();
            } catch (IOException e1) {
                e1.printStackTrace();
                return;
            }

        try (PrintWriter out = new PrintWriter(file)) {
            out.println(data);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        Log.p("Done, see file in: " + file.getAbsolutePath());
	}
	
	public static String asOrdinal(int value) {
		if (value <= 0)
			return null;
		if (value >= 11 && value <= 13)
			return value + "th";
		switch(value % 10) {
			case 1:
				return value+"st";
			case 2:
				return value+"nd";
			case 3:
				return value+"rd";
			default:
				return value+"th";
		}
	}
}
