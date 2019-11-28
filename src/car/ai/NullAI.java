package car.ai;

import car.ray.RayCarControl;

public class NullAI extends CarAI {

	public NullAI(RayCarControl car) {
		super(car);
	}

	@Override
	public void update(float tpf) {
		//Nothing by choice
	}
}