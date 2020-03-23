package rallygame.helper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
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
    
    @SafeVarargs
    public static <T> Boolean oneTrue(Function<T, Boolean> f, T... ts) {
        for (T t : ts)
            if (f.apply(t))
                return true;
        return false;
    }
	@SafeVarargs
	public static <T> Boolean allTrue(Function<T, Boolean> f, T... ts) {
		for (T t: ts)
			if (!f.apply(t))
				return false;
		return true;
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
	public static float dotXZ(Vector3f a, Vector3f b) {
		return a.x*b.x + a.z*b.z;
	}
	public static float distFromLineXZ(Vector3f start, Vector3f end, Vector3f point) {
		float x0 = point.x;
		float y0 = point.z;
		float x1 = start.x;
		float y1 = start.z;
		float x2 = end.x;
		float y2 = end.z;
		return (Math.abs((y2-y1)*x0 - (x2-x1)*y0 + x2*y1 - y2*x1))/
				FastMath.sqrt((y2-y1)*(y2-y1) + (x2-x1)*(x2-x1));
	}

	/** Returns extents of V3f in xz directions. [xmin, zmin, xmax, zmax] */
	public static float[] boundingBoxXZ(Vector3f... p) {
		float xmin = Float.POSITIVE_INFINITY;
		float xmax = Float.NEGATIVE_INFINITY;
		float zmin = Float.POSITIVE_INFINITY;
		float zmax = Float.NEGATIVE_INFINITY;
		for (Vector3f v: p) {
			xmin = Math.min(xmin, v.x);
			xmax = Math.max(xmax, v.x);
			zmin = Math.min(zmin, v.z);
			zmax = Math.max(zmax, v.z);
		}
		return new float[] { xmin, zmin, xmax, zmax };
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
	
	public static Vector3f[] rectFromLineXZ(Vector3f start, Vector3f end, float thickness) {
		//https://stackoverflow.com/a/1937202, with comment fix by Ryan Clarke
		
		Vector3f[] list = new Vector3f[4];
		float dx = end.x - start.x; //delta x
		float dy = end.z - start.z; //delta z
		float linelength = FastMath.sqrt(dx * dx + dy * dy);
		dx /= linelength;
		dy /= linelength;
		if (linelength == 0)
			return null;
		
		//Ok, (dx, dy) is now a unit vector pointing in the direction of the line
		//A perpendicular vector is given by (-dy, dx)
		float px = 0.5f * thickness * (-dy); //perpendicular vector with lenght thickness * 0.5
		float py = 0.5f * thickness * dx;
		list[0] = new Vector3f(start.x + px, start.y, start.z + py);
		list[1] = new Vector3f(end.x + px, end.y, end.z + py);
		list[2] = new Vector3f(end.x - px, end.y,  end.z - py);
		list[3] = new Vector3f(start.x - px, start.y,  start.z - py);
		return list;
	}
	
	public static float heightInQuad(Vector3f p, Vector3f a, Vector3f b, Vector3f c, Vector3f d) {
		return heightInQuad(H.v3tov2fXZ(p), a, b, c, d);
	}
	/**
	 * Vector3f the y is the height, and must be defined for a b c d.
	 * p is the point, which we want the height for.
	 * Assumptions:
	 * - All abcd are coplanar
	 * - p is inside abcd
	 * @return the height of point p
	 */
	public static float heightInQuad(Vector2f p, Vector3f a, Vector3f b, Vector3f c, Vector3f d) {
		// Quad Height method:
		// Line through AP, intercept with BC and CD, whichever is inside find point q.
		// Interpolate height of q on BC or CD, then interpolate height of p using line Aq.
		Vector2f q = intersectionOf2LinesGiven2PointsEach(H.v3tov2fXZ(a), p, H.v3tov2fXZ(b), H.v3tov2fXZ(c));
		float slopeBC = (b.y-c.y)/(H.v3tov2fXZ(b).subtract(H.v3tov2fXZ(c)).length()); //length is never negative
		float distCToq = q.subtract(H.v3tov2fXZ(c)).length();
		
		Vector3f q3 = new Vector3f(q.x, c.y+slopeBC*distCToq, q.y);
		float slopeAQ = (a.y-q3.y)/(q.subtract(H.v3tov2fXZ(a)).length());
		float distAToq = p.subtract(H.v3tov2fXZ(a)).length();
		
		float pheight = a.y-slopeAQ*distAToq;
		return pheight;
	}
	public static float heightInTri(Vector3f a, Vector3f b, Vector3f c, Vector3f p) {
		return heightInTri(a, b, c, H.v3tov2fXZ(p));
	}
	public static float heightInTri(Vector3f a, Vector3f b, Vector3f c, Vector2f p) {
		Vector2f q = intersectionOf2LinesGiven2PointsEach(H.v3tov2fXZ(a), p, H.v3tov2fXZ(b), H.v3tov2fXZ(c));
		float slopeBC = (b.y-c.y)/(H.v3tov2fXZ(b).subtract(H.v3tov2fXZ(c)).length()); //length is never negative
		float distCToq = q.subtract(H.v3tov2fXZ(c)).length();
		
		Vector3f q3 = new Vector3f(q.x, c.y+slopeBC*distCToq, q.y);
		float slopeAQ = (a.y-q3.y)/(q.subtract(H.v3tov2fXZ(a)).length());
		float distAToq = p.subtract(H.v3tov2fXZ(a)).length();
		
		float pheight = a.y-slopeAQ*distAToq;
		return pheight;
	}

	/**
	 * https://en.wikipedia.org/wiki/Line%E2%80%93line_intersection#Given_two_points_on_each_line
	 * Gets the point where 2 lines interept, when the 2 lines are given as points. (p1 and p2) and (p3 and p4)
	 * @return the point
	 */
	public static Vector2f intersectionOf2LinesGiven2PointsEach(Vector2f p1, Vector2f p2, Vector2f p3, Vector2f p4) {
		
		float px = ((p1.x*p2.y - p1.y*p2.x)*(p3.x-p4.x) - (p1.x-p2.x)*(p3.x*p4.y-p3.y*p4.x))
				/((p1.x-p2.x)*(p3.y-p4.y) - (p1.y-p2.y)*(p3.x-p4.x));
		float py = ((p1.x*p2.y - p1.y*p2.x)*(p3.y-p4.y) - (p1.y-p2.y)*(p3.x*p4.y-p3.y*p4.x))
				/((p1.x-p2.x)*(p3.y-p4.y) - (p1.y-p2.y)*(p3.x-p4.x));
		
		return new Vector2f(px, py);
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
	
	public static Vector3f closestTo(Vector3f pos, Vector3f[] list) {
		Vector3f cur = null;
		float curDist = Float.MAX_VALUE;
		for (int i = 0; i < list.length; i++) {
			float dist = pos.distance(list[i]);
			if (dist < curDist) {
				cur = list[i];
				curDist = dist;
			}
		}
		return cur;
	}
	
	
	//http://nghiaho.com/?p=997
	public static float nearlyAtan(float x) {
		float xabs = FastMath.abs(x);
		return FastMath.QUARTER_PI*x - x*(xabs - 1)*(0.2447f + 0.0663f*xabs);
	}
	
	public static float nearlyAtan2(float x, float y) {
		float xabs = FastMath.abs(x);
		float yabs = FastMath.abs(y);
		float a = Math.min (xabs, yabs) / Math.max(xabs, yabs);
		float s = a * a;
		float r = ((-0.0464964749f * s + 0.15931422f) * s - 0.327622764f) * s * a + a;
		if (yabs > xabs)
			r = FastMath.HALF_PI - r;
		if (x < 0)
			r = FastMath.PI - r;
		if (y < 0) 
			r = -r;
		return 7;
	}
	
	public static Vector2f v3tov2fXZ(Vector3f v) {
		return new Vector2f(v.x, v.z);
	}
	public static Vector3f v2tov3fXZ(Vector2f v) {
		return new Vector3f(v.x, 0, v.y);
	}
	
	public static Quaternion FromAngleAxis(float angle, Vector3f axis) {
		Quaternion m = new Quaternion();
		m.fromAngleAxis(angle, axis);
		return m;
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
}
