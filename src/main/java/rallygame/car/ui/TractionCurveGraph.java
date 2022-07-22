package rallygame.car.ui;

import com.jme3.asset.AssetManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Container;

import rallygame.car.data.CarDataConst;
import rallygame.car.data.WheelDataTractionConst;
import rallygame.car.ray.GripHelper;
import rallygame.helper.Geo;

public class TractionCurveGraph extends Container {

	private float maxLat;
	private WheelDataTractionConst latData;
	private float maxLong;
	private WheelDataTractionConst longData;
	private AssetManager am;

	public TractionCurveGraph(AssetManager am, CarDataConst data, Vector3f size) {
		super();
		
		this.am = am;
		this.setPreferredSize(size);
		setCarDataConst(data);
	}
	
	public void setCarDataConst(CarDataConst data) {
		this.latData = data.wheelData[0].traction.pjk_lat;
		this.longData = data.wheelData[0].traction.pjk_long;
		drawGraphs();
	}

	private void drawGraphs() {
		this.detachAllChildren();
		
		Vector3f size = getPreferredSize();
		maxLat = GripHelper.tractionFormula(latData, GripHelper.calcSlipMax(latData));
		Float[] points = simulateGraphPoints(size, latData);
		for (int i = 0; i < points.length; i++) {
			Vector3f pos = new Vector3f(i*(size.x/points.length), -(size.y/2)+(size.y/2)*(points[i]/ maxLat), 0);
			this.attachChild(Geo.makeShapeBox(am, ColorRGBA.Blue, pos, 1));
		}
		
		maxLong = GripHelper.tractionFormula(longData, GripHelper.calcSlipMax(longData));
		points = simulateGraphPoints(size, longData);
		for (int i = 0; i < points.length; i++) {
			Vector3f pos = new Vector3f(i*(size.x/points.length), -(size.y)+(size.y/2)*(points[i]/maxLong), 0);
			this.attachChild(Geo.makeShapeBox(am, ColorRGBA.Red, pos, 1));
		}
	}
	private Float[] simulateGraphPoints(Vector3f screenSize, WheelDataTractionConst d) {
		Float[] list = new Float[(int)screenSize.x/2];
		for (int i = 0; i < list.length; i++)
			list[i] = GripHelper.tractionFormula(d, (float)i*FastMath.PI/screenSize.x);
		return list;
	}
	
	public void update(float tpf) {
	}
}
