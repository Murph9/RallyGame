package rallygame.car.data.wheel;

import java.io.Serializable;

import rallygame.car.data.SurfaceType;

public class WheelTraction implements Serializable {
    public String name;
	public WheelDataTractionConst pjk_lat;
	public WheelDataTractionConst pjk_long;

	public WheelDataTractionConst getLatTractionFor(SurfaceType type) {
		if (type == null || type == SurfaceType.Normal)
			return pjk_lat;

		return null; // TODO which will throw somewhere later
	}

	public WheelDataTractionConst getLongTractionFor(SurfaceType type) {
		if (type == null || type == SurfaceType.Normal)
			return pjk_long;

		return null; // TODO which will throw somewhere later
	}
}
