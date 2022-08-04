package rallygame.car.data.wheel;

import java.io.Serializable;

import rallygame.car.data.SurfaceType;
import rallygame.car.ray.GripHelper;

public class WheelTraction implements Serializable {
    public String name;
	public WheelDataTractionConst pjk_lat;
	public WheelDataTractionConst pjk_long;

	public WheelDataTractionConst getLatTractionFor(SurfaceType type) {
		if (type == null || type == SurfaceType.Normal)
			return pjk_lat;

		var result = new WheelDataTractionConst(pjk_lat);
		switch(type) {
			case Dirt: result.D *= 0.7f; break;
			case Grass: result.D *= 0.6f; break;
			case Ice: result.D *= 0.3f; break;
			case None: result.D *= 0.0f; break;
			default:
		}
		result.max = GripHelper.calcSlipMax(result); // TODO VERY LARGE HACK
		return result;
	}

	public WheelDataTractionConst getLongTractionFor(SurfaceType type) {
		if (type == null || type == SurfaceType.Normal)
			return pjk_long;

		var result = new WheelDataTractionConst(pjk_long);
		switch(type) {
			case Dirt: result.D *= 0.7f; break;
			case Grass: result.D *= 0.6f; break;
			case Ice: result.D *= 0.3f; break;
			case None: result.D *= 0.0f; break;
			default:
		}
		result.max = GripHelper.calcSlipMax(result); // TODO VERY LARGE HACK
		return result;
	}
}
