package car.ai;

import com.jme3.math.Vector3f;

import car.ray.RayCarControl;
import drive.DriveRace;

public class RaceAI extends CarAI {

	private DriveRace race;
	
	public RaceAI(RayCarControl car, DriveRace race) {
		super(car);
		this.race = race;
	}

	@Override
	public void update(float tpf) {
		Vector3f pos = car.getPhysicsLocation();
		Vector3f atPos = race.getNextCheckpoint(this, pos);
		if (atPos == null) {
			justBrake();
			return;
		}
		
		driveAt(atPos);
		
		//if going too slow speed up
		if (car.getLinearVelocity().length() < 10) {
			onEvent("Accel", true, 1);
			onEvent("Brake", false, 0);
			
			if (car.getLinearVelocity().length() < 1 && car.up.y < 0) { //very still
				onEvent("Flip", true, 1);
			}
		}
		if (car.getLinearVelocity().length() > 20) {
			onEvent("Accel", false, 0); //don't go too fast
			onEvent("Brake", false, 0);
		}
	}
}
