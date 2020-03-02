package car.ai;

import car.ray.RayCarControl;

public class NullAI extends CarAI {

	public NullAI(RayCarControl car) {
		super(car);
	}

	@Override
	public void run() {
		//Nothing by choice
	}
}