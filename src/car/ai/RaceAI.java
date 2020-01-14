package car.ai;

import java.util.List;

import com.jme3.math.Vector3f;

import car.ray.RayCarControl;
import car.ray.RayWheelControl;
import drive.DriveRace;

public class RaceAI extends CarAI {

    private float stuckTimer;
	private DriveRace race;
	
	public RaceAI(RayCarControl car, DriveRace race) {
		super(car);
		this.race = race;
	}

	@Override
	public void update(float tpf) {
		Vector3f pos = car.getPhysicsLocation();
		Vector3f atPos = race.getNextCheckpoint(car, pos);
		if (atPos == null) {
			justBrake();
			return;
		}
		
		driveAt(atPos);

        float velocity = car.getLinearVelocity().length();

        // if going too slow speed up
        if (velocity < 2) {
            onEvent("Accel", true);
            onEvent("Brake", false);
        }

        // very still, flip
        if (velocity < 0.05f && car.up.y < 0) {
            onEvent("Flip", true);
        }

        // very still for a while, reset
        if (velocity < 0.1f) {
            stuckTimer += tpf;
            if (stuckTimer > 3) {
                onEvent("Reset", true);
                stuckTimer = 0;
            }
        } else {
            stuckTimer = 0;
        }

        //reduce excess wheel slipping
        List<RayWheelControl> wheels = car.getDriveWheels();
        float gripSum = 0;
        for (RayWheelControl wheel: wheels) {
            gripSum += wheel.getRayWheel().skidFraction;
        }
        if (velocity > 10 && gripSum > wheels.size()) {
            onEvent("Accel", false);
        }
	}
}
