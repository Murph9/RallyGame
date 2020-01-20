package car.ai;

import com.jme3.math.Vector3f;

import car.ray.RayCarControl;
import drive.race.DriveRace;

public class RaceAI extends CarAI {

	private DriveRace race;
	
	public RaceAI(RayCarControl car, DriveRace race) {
		super(car);
		this.race = race;
	}

	@Override
	public void update(float tpf) {
		Vector3f atPos = race.getNextCheckpoint(car);
		if (atPos == null) {
			justBrake();
			return;
		}
        
        driveAt(atPos);
        

        tryStuffIfStuck(tpf);
        detectVeryLongFall(tpf);

        float velocity = car.getLinearVelocity().length();

        // very still, flip
        if (velocity < 0.05f && car.up.y < 0) {
            onEvent("Flip", true);
        }

        applySelfTractionControl(tpf);

        //prevent hitting straight into walls
        float result = forwardRayCollideTime();
        if (result < 1.5f) {
            onEvent("Brake", true);
        }
        
        // if going too slow speed up
        if (velocity < 2) {
            onEvent("Accel", true);
            onEvent("Brake", false);
        }
    }
}
