package car;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Container;

import game.App;
import helper.H;

public class TractionCurveGraph extends Container {

	private WheelData latData;
	private WheelData longData;
	
	public TractionCurveGraph(MyPhysicsVehicle p, Vector3f size) {
		super();
		
		this.setPreferredSize(size);
		updateMyPhysicsVehicle(p);
	}
	
	public void updateMyPhysicsVehicle(MyPhysicsVehicle p) {
		this.latData = p.car.w_flatdata;
		this.longData = p.car.w_flongdata;
		drawGraphs();
	}

	private void drawGraphs() {
		this.detachAllChildren();
		
		Vector3f size = getPreferredSize();
		float max = VehicleGripHelper.tractionFormula(latData, VehicleGripHelper.calcSlipMax(latData, 0.0005f));
		Float[] points = simulateGraphPoints(latData);
		for (int i = 0; i < points.length; i++) {
			Vector3f pos = new Vector3f(i*(size.x/points.length), -(size.y/2)+(size.y/2)*(points[i]/max), 0);
			this.attachChild(H.makeShapeBox(App.rally.getAssetManager(), ColorRGBA.Blue, pos, 1));
		}
		
		max = VehicleGripHelper.tractionFormula(longData, VehicleGripHelper.calcSlipMax(longData, 0.0005f));
		points = simulateGraphPoints(longData);
		for (int i = 0; i < points.length; i++) {
			Vector3f pos = new Vector3f(i*(size.x/points.length), -(size.y)+(size.y/2)*(points[i]/max), 0);
			this.attachChild(H.makeShapeBox(App.rally.getAssetManager(), ColorRGBA.Red, pos, 1));
		}
	}
	private Float[] simulateGraphPoints(WheelData d) {
		Vector3f size = getPreferredSize();
		Float[] list = new Float[(int)size.x/2];
		for (int i = 0; i < list.length; i++)
			list[i] = VehicleGripHelper.tractionFormula(d, (float)i*FastMath.PI/size.x);
		return list;
	}
	
	public void update(float tpf) {
		//TODO draw dots for current wheel values
		//might be hard because each wheel has 2 values..
	}
}
