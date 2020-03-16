package rallygame.car.ai;

import com.jme3.math.Vector3f;

import rallygame.car.ray.RayCarControl;
import rallygame.drive.ICheckpointDrive;

public class RaceAI extends CarAI {

    private ICheckpointDrive race;
    private final boolean doFowardRayCast;
    
    public RaceAI(RayCarControl car, ICheckpointDrive race) {
        this(car, race, true);
    }
	public RaceAI(RayCarControl car, ICheckpointDrive race, boolean doForwardRayCast) {
		super(car);
        this.race = race;
        this.doFowardRayCast = doForwardRayCast;
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

        float velocity = car.vel.length();

        // very still, flip
        if (velocity < 0.05f && car.up.y < 0) {
            onEvent("Flip", true);
        }

        applySelfTractionControl(tpf);

        //prevent hitting straight into walls
        if (doFowardRayCast) {
            float result = forwardRayCollideTime();
            if (result < 1.5f) {
                onEvent("Brake", true);
            }
        }
        
        // if going too slow speed up
        if (velocity < 2) {
            onEvent("Accel", true);
            onEvent("Brake", false);
        }
    }
}
