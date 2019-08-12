package car;

import java.util.LinkedList;
import java.util.List;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.simsilica.lemur.Container;

import car.ray.CarDataConst;
import car.ray.RayCarControl;
import helper.H;

public class PowerCurveGraph extends Container {

	private AssetManager am;
	private CarDataConst car;
	private List<Spatial> things;

	public PowerCurveGraph(AssetManager am, RayCarControl p, Vector3f size) {
		super();

		this.am = am;
		
		this.setPreferredSize(size);
		updateMyPhysicsVehicle(p);
	}
	
	public void updateMyPhysicsVehicle(RayCarControl p) {
		this.car = p.getCarData();
		drawGraphs();
	}
	
	private void drawGraphs() {
		if (things != null)
			for (Spatial g: things)
				this.detachChild(g);
		things = new LinkedList<Spatial>();
		
		BitmapFont guiFont = am.loadFont("Interface/Fonts/Default.fnt");

		Vector3f size = getPreferredSize();
		Vector3f topleftPadding = new Vector3f(size.x*0.1f, size.y*0.1f, 0);
		
		Vector3f topLeft = new Vector3f(topleftPadding.x, -(size.y)+(size.y-topleftPadding.y), 0);
		Vector3f bottomRight = new Vector3f(size.x,-size.y,0);
		
		float sx = bottomRight.x - topLeft.x;
		float sy = topLeft.y - bottomRight.y;
		
		//grid lines
		for (int i = 0; i <= 3; i++) {
			Vector3f start = new Vector3f(topLeft.x, topLeft.y+((bottomRight.y-topLeft.y)*i/3), 0);
			Geometry g = H.makeShapeLine(am, ColorRGBA.Gray, start, start.add(new Vector3f(sx, 0, 0)));
			this.attachChild(g);
			this.things.add(g);
		}
		for (int i = 0; i <= car.e_torque.length; i++) {
			Vector3f start = new Vector3f(topLeft.x+(sx*i/car.e_torque.length), topLeft.y, 0);
			Geometry g = H.makeShapeLine(am, ColorRGBA.Gray, start, start.add(new Vector3f(0, -sy, 0)));
			this.attachChild(g);
			this.things.add(g);
		}

		//float magicValue = 1.79f; //makes the graphs overlap at around 5400 rpm (assuming the scale matches)
		
		float maxTorque = H.maxInArray(car.e_torque);
		float maxKW = H.maxInArray(car.e_torque, (x, i) -> x*i*1000/9549);
		Vector3f lastTorquePos = new Vector3f(topLeft.x, bottomRight.y+(sy*car.e_torque[0]/maxTorque), 0);
		Vector3f lastKWPos = new Vector3f(topLeft.x, bottomRight.y+(sy*(car.e_torque[0]*0*1000/9549)/maxKW), 0);
		
		//actual values
		for (int i = 1; i < car.e_torque.length; i++) {
			Vector3f torque = new Vector3f(topLeft.x+i*(sx/car.e_torque.length), bottomRight.y+(sy*car.e_torque[i]/maxTorque), 0);
			Geometry g = H.makeShapeLine(am, ColorRGBA.Blue, lastTorquePos, torque);
			this.attachChild(g);
			this.things.add(g);
			lastTorquePos = torque;
			
			Vector3f kw = new Vector3f(topLeft.x+i*(sx/car.e_torque.length), bottomRight.y+(sy*car.e_torque[i]*i*1000/(9549*maxKW)), 0);
			Geometry g2 = H.makeShapeLine(am, ColorRGBA.Red, lastKWPos, kw);
			this.attachChild(g2);
			this.things.add(g2);
			lastKWPos = kw;
		}
		
		//labels, numbers

		float fontSize = guiFont.getCharSet().getRenderedSize();
		//0 label
		BitmapText label = new BitmapText(guiFont, false);
		label.setSize(fontSize);
		label.setColor(ColorRGBA.White);
		label.setText("0");
		label.setLocalTranslation(topLeft.x-fontSize, bottomRight.y+fontSize, 0);
		attachChild(label);
		this.things.add(label);
		
		//max labels (note they aren't inside each other)
		label = new BitmapText(guiFont, false);
		label.setSize(fontSize);
		label.setColor(ColorRGBA.Blue);
		label.setText(maxTorque+" Nm");
		label.setLocalTranslation(topLeft.x-fontSize, topLeft.y+fontSize, 0);
		attachChild(label);
		this.things.add(label);
		label = new BitmapText(guiFont, false);
		label.setSize(fontSize);
		label.setColor(ColorRGBA.Red);
		label.setText(maxKW+" Kw");
		label.setLocalTranslation(topLeft.x-fontSize, topLeft.y, 0);
		attachChild(label);
		this.things.add(label);
	}
}
