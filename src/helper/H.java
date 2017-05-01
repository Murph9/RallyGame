package helper;

import java.util.LinkedList;
import java.util.List;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.debug.Arrow;

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
	        System.out.print(o.toString() + " ");
	    }
		System.out.println();
	}
	public static void p(Object[] ol, String sep) {
		if (sep == null) sep = "\n";
		for (Object o: ol)
			System.out.print(o+sep);
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
	public static void e(Object[] ol, String sep) {
		if (sep == null) sep = "\n";
		for (Object o: ol)
			System.err.print(o+sep);
		System.out.println();
	}
	public static void e(Iterable<Object> ol, String sep) {
		H.e(ol, sep);
	}
	public static void e(Exception e) {
		e.printStackTrace(System.err);
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
		Arrow arrow = new Arrow(dir);
		Geometry arrowG = createShape(am, arrow, color, pos);
		arrowG.setShadowMode(ShadowMode.Off);
		return arrowG;
	}
	
	public static Geometry createShape(AssetManager am, Mesh shape, ColorRGBA color, Vector3f pos) {
		Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		mat.getAdditionalRenderState().setWireframe(true);
		mat.setColor("Color", color);
		
		Geometry g = new Geometry("coordinate axis", shape);
		g.setMaterial(mat);
		g.setLocalTranslation(pos);
		return g;
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
	
	public static Vector3f screenTopLeft() {
		return new Vector3f(0, App.rally.getSettings().getHeight(), 0);
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
}
