package game;

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

import de.lessvoid.nifty.controls.DropDown;
import de.lessvoid.nifty.screen.Screen;


//Its short for help, if the name was any longer it might not actually be helpful
public class H {

	/**
	 * Easier way of typing System.out.println(); -> H.p();
	 * @param o The thing you wanted printed
	 */
	public static void p(Object o) {
		System.out.println(o);
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
		return RGeomList(n);
	}
	private static List<Geometry> RGeomList(Node n) {
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
		arrow.setLineWidth(1); // make arrow thicker
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
	
	public static <T> DropDown<T> findDropDownControl(Screen screen, final String id) {
		return screen.findNiftyControl(id, DropDown.class);
	}
}
