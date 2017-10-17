package helper;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;
import com.jme3.scene.shape.Box;

import game.App;

//Its short for help, name length was a concern (see H.p())
public class H {

	/**
	 * Easier way of typing System.out.println(); -> H.p();
	 * @param o The thing you wanted printed
	 */
	public static void p() {
		System.out.println();
	}
	public static void p(Object o) {
		System.out.println(o);
	}
	public static void p(Object... os) {
		for (Object o : os) {
	        System.out.print(o + " ");
	    }
		System.out.println();
	}
	public static void p(Object[] ol, String sep) {
		if (sep == null) sep = "\n";
		for (Object o: ol)
			System.out.print(o + sep);
		System.out.println();
	}
	public static void p(Iterable<Object> ol, String sep) {
		H.p(ol, sep);
	}
	public static void p(Object[][] matrix, String sep) {
		if (matrix == null)
			return;
		if (matrix.length == 0)
			return;
		
		System.out.println("Matrix:");
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[0].length; j++) {
				System.out.print(matrix[i][j] + sep);
			}
			System.out.println();
		}
	}
	
	//System.err
	public static void e(Object o) {
		System.err.println(o);
	}
	public static void e(Object... os) {
		for (Object o : os) {
	        System.err.print(o + " ");
	    }
		System.err.println();
	}
	public static void e(Object[] ol, String sep) {
		if (sep == null) sep = "\n";
		for (Object o: ol)
			System.err.print(o+sep);
		System.err.println();
	}
	public static void e(Iterable<Object> ol, String sep) {
		H.e(ol, sep);
	}
	public static void e(Exception e) {
		e.printStackTrace(System.err);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Boolean allTrue(Function<T, Boolean> f, T... ts) {
		for (T t: ts)
			if (!f.apply(t))
				return false;
		return true;
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
	public static float[] boundingBoxXZ(Vector3f... p) {
		float xmin = Float.MAX_VALUE;
		float xmax = Float.MIN_VALUE;
		float zmin = Float.MAX_VALUE;
		float zmax = Float.MIN_VALUE;
		for (Vector3f v: p) {
			xmin = Math.min(xmin, v.x);
			xmax = Math.max(xmax, v.x);
			zmin = Math.min(zmin, v.z);
			zmax = Math.max(zmax, v.z);
		}
		return new float[] { xmin, zmin, xmax, zmax };
		//return new Vector3f[] { new Vector3f(xmin, 0, zmin), new Vector3f(xmin, 0, zmax),new Vector3f(xmax, 0, zmax),new Vector3f(xmax, 0, zmin)};
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
	
	public static ColorRGBA lerpColor(float value, ColorRGBA a, ColorRGBA b) {
		return new ColorRGBA(
				FastMath.interpolateLinear(value, a.r, b.r),
				FastMath.interpolateLinear(value, a.g, b.g),
				FastMath.interpolateLinear(value, a.b, b.b),
				1
			);
	}
	
	public static int clamp(int input, int low, int high) {
		if (input < low) input = low;
		if (input > high) input = high;
		return input;
	}
	
	public static float cylinderInertia(float r, float mass) {
		return mass*r*r/2;
	}
	
	public static String roundDecimal(float num, int places) {
		if (places == 0) {
			return Integer.toString((int)num);
		}
		
		String s = Float.toString(num);
		String[] sa = s.split("\\.");
		
		if (sa.length > 1) {
			places = Math.min(sa[1].length(), places);
			return sa[0]+"."+sa[1].substring(0, places);
		} else {
			return sa.toString();
		}
	}
	public static String roundDecimal(double num, int places) { //...
		return roundDecimal((float)num,places);
	}
	
	public static List<Geometry> getGeomList(Node n) {
		return rGeomList(n);
	}
	private static List<Geometry> rGeomList(Node n) {
		List<Geometry> listg = new LinkedList<Geometry>();
		
		List<Spatial> list = n.getChildren();
		if (list.isEmpty()) return listg;
		
		for (Spatial sp: list) {
        	if (sp instanceof Node) {
        		listg.addAll(getGeomList((Node)sp));
        	}
        	if (sp instanceof Geometry) {
        		listg.add((Geometry)sp);
        	}
        }
		return listg;
	}
	
	public static Geometry makeShapeArrow(AssetManager am, ColorRGBA color, Vector3f dir, Vector3f pos) {
		if (!Vector3f.isValidVector(pos) || !Vector3f.isValidVector(dir)) {
			H.e("not valid pos or dir", pos, dir);
			return null;
		}
		Arrow arrow = new Arrow(dir);
		Geometry arrowG = createShape(am, arrow, color, pos, "an arrow");
		arrowG.setShadowMode(ShadowMode.Off);
		return arrowG;
	}
	public static Geometry makeShapeBox(AssetManager am, ColorRGBA color, Vector3f pos, float size) {
		if (!Vector3f.isValidVector(pos)) {
			H.e("not valid position");
			return null;
		}
		
		Box box = new Box(size, size, size);
		Geometry boxG = createShape(am, box, color, pos, "a box");
		boxG.setShadowMode(ShadowMode.Off);
		return boxG;
	}
	
	public static Geometry createShape(AssetManager am, Mesh shape, ColorRGBA color, Vector3f pos, String name) {
		Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", color);
		
		Geometry g = new Geometry(name, shape);
		g.setMaterial(mat);
		g.setLocalTranslation(pos);
		return g;
	}
	
	public static Vector3f[] rectFromLine(Vector3f start, Vector3f end, float thickness) {
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
	/**
	 * Vector3f the y is the height, and must be defined for a b c d.
	 * p is the point, which we want the height for.
	 * Assumptions:
	 * - All abcd are coplanar
	 * - p is inside abcd
	 * @return the height of point p
	 */
	//Quad Height method:
	//Line through AP, intercept with BC and CD, whichever is inside find point q.
	//Interpolate height of q on BC or CD, then interpolate height of p using line Aq.
	public static float heightInQuad(Vector3f p, Vector3f a, Vector3f b, Vector3f c, Vector3f d) {
		Vector2f q = intersectionOf2LinesGiven2PointsEach(H.v3tov2fXZ(a), H.v3tov2fXZ(p), H.v3tov2fXZ(b), H.v3tov2fXZ(c));
		float slopeBC = (b.y-c.y)/(H.v3tov2fXZ(b).subtract(H.v3tov2fXZ(c)).length()); //length is never negative
		float distCToq = q.subtract(H.v3tov2fXZ(c)).length();
		
		Vector3f q3 = new Vector3f(q.x, c.y+slopeBC*distCToq, q.y);
		float slopeAQ = (a.y-q3.y)/(q.subtract(H.v3tov2fXZ(a)).length());
		float distAToq = H.v3tov2fXZ(p).subtract(H.v3tov2fXZ(a)).length();
		
		float pheight = a.y-slopeAQ*distAToq;
		return pheight;
	}
	public static float heightInTri(Vector3f a, Vector3f b, Vector3f c, Vector3f p) {
		Vector2f q = intersectionOf2LinesGiven2PointsEach(H.v3tov2fXZ(a), H.v3tov2fXZ(p), H.v3tov2fXZ(b), H.v3tov2fXZ(c));
		float slopeBC = (b.y-c.y)/(H.v3tov2fXZ(b).subtract(H.v3tov2fXZ(c)).length()); //length is never negative
		float distCToq = q.subtract(H.v3tov2fXZ(c)).length();
		
		Vector3f q3 = new Vector3f(q.x, c.y+slopeBC*distCToq, q.y);
		float slopeAQ = (a.y-q3.y)/(q.subtract(H.v3tov2fXZ(a)).length());
		float distAToq = H.v3tov2fXZ(p).subtract(H.v3tov2fXZ(a)).length();
		
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
	
	
	//copies to first array
	public static void addTogether(float[] a, float[] b) {
		if (a.length != b.length) {
			H.e("Arrays not the same length: " + a.length + " and " + b.length);
			return;
		}
		for (int i = 1; i < a.length - 1; i++) {
			a[i] += b[i];
		}
	}
	//copies to new array
	public static float[] addTogetherNew(float[] a, float[] b) {
		if (a.length != b.length) {
			H.e("Arrays not the same length: " + a.length + " and " + b.length);
			return null;
		}
		float[] out = new float[a.length];
		System.arraycopy(a, 0, out, 0, a.length);
		for (int i = 1; i < a.length - 1; i++) {
			out[i] += b[i];
		}
		return out;
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
	
	public static Matrix3f FromAngleAxis(float angle, Vector3f axis) {
		Matrix3f m = new Matrix3f();
		m.fromAngleAxis(angle, axis);
		return m;
	}
	
	
	public static Vector3f randV3f() {
		return new Vector3f(FastMath.nextRandomFloat()*2-1, FastMath.nextRandomFloat()*2-1, FastMath.nextRandomFloat()*2-1);
	}
	public static <T> T randFromArray(T[] array) {
		return array[FastMath.nextRandomInt(0, array.length-1)];
	}
	
	public static Vector3f screenTopLeft() {
		return new Vector3f(0, App.rally.getSettings().getHeight(), 0);
	}
	public static Vector3f screenTopRight() {
		return new Vector3f(App.rally.getSettings().getWidth(), App.rally.getSettings().getHeight(), 0);
	}
	public static Vector3f screenBottomRight() {
		return new Vector3f(App.rally.getSettings().getWidth(), App.rally.getSettings().getHeight(), 0);
	}
	public static Vector3f screenMiddle() {
		return new Vector3f(App.rally.getSettings().getWidth()/2, App.rally.getSettings().getHeight()/2, 0);
	}
	
	//http://stackoverflow.com/a/677248
	public static class Duo<A, B> {
		public final A first;
	    public final B second;

	    public Duo(A first, B second) {
	    	super();
	    	this.first = first;
	    	this.second = second;
	    }

	    public int hashCode() {
	    	int hashFirst = first != null ? first.hashCode() : 0;
	    	int hashSecond = second != null ? second.hashCode() : 0;
	    	return (hashFirst + hashSecond) * hashSecond + hashFirst;
	    }

	    public boolean equals(Object other) {
	    	if (other instanceof Duo<?, ?>) {
				@SuppressWarnings("unchecked")
				Duo<A, B> otherPair = (Duo<A, B>) other;
	    		return 
	    		((  this.first == otherPair.first ||
	    			( this.first != null && otherPair.first != null &&
	    			  this.first.equals(otherPair.first))) &&
	    		 (	this.second == otherPair.second ||
	    			( this.second != null && otherPair.second != null &&
	    			  this.second.equals(otherPair.second))) );
	    	}

	    	return false;
	    }

	    public String toString()
	    { 
	    	return "(" + first + ", " + second + ")"; 
	    }
	}

	public static Boolean hasParentNode(Spatial s, Node node) {
		while (s != null) {
			if (s == node) {
				return true;
			}
			
			s = s.getParent();
		}
		return false;
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
}
