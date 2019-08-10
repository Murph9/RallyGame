package car;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Container;

import car.ray.RayCarControl;
import car.ray.WheelDataTractionConst;
import game.App;
import helper.H;

import car.ray.RayCar;

public class TractionCurveGraph extends Container {

	private WheelDataTractionConst latData;
	private WheelDataTractionConst longData;
	
	public TractionCurveGraph(RayCarControl p, Vector3f size) {
		super();
		
		this.setPreferredSize(size);
		updateMyPhysicsVehicle(p);
	}
	
	public void updateMyPhysicsVehicle(RayCarControl p) {
		this.latData = p.getCarData().wheelData[0].pjk_lat;
		this.longData = p.getCarData().wheelData[0].pjk_long;
		drawGraphs();
	}

	private void drawGraphs() {
		this.detachAllChildren();
		
		Vector3f size = getPreferredSize();
		float max = RayCar.GripHelper.tractionFormula(latData, RayCar.GripHelper.calcSlipMax(latData));
		Float[] points = simulateGraphPoints(latData);
		for (int i = 0; i < points.length; i++) {
			Vector3f pos = new Vector3f(i*(size.x/points.length), -(size.y/2)+(size.y/2)*(points[i]/max), 0);
			this.attachChild(H.makeShapeBox(App.CUR.getAssetManager(), ColorRGBA.Blue, pos, 1));
		}
		
		max = RayCar.GripHelper.tractionFormula(longData, RayCar.GripHelper.calcSlipMax(longData));
		points = simulateGraphPoints(longData);
		for (int i = 0; i < points.length; i++) {
			Vector3f pos = new Vector3f(i*(size.x/points.length), -(size.y)+(size.y/2)*(points[i]/max), 0);
			this.attachChild(H.makeShapeBox(App.CUR.getAssetManager(), ColorRGBA.Red, pos, 1));
		}
	}
	private Float[] simulateGraphPoints(WheelDataTractionConst d) {
		Vector3f size = getPreferredSize();
		Float[] list = new Float[(int)size.x/2];
		for (int i = 0; i < list.length; i++)
			list[i] = RayCar.GripHelper.tractionFormula(d, (float)i*FastMath.PI/size.x);
		return list;
	}
	
	public void update(float tpf) {
		//TODO draw dots for real time current values
		//might be hard because each wheel has 2 values..
	}
}
