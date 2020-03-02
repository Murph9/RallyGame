package car.ai;

import com.jme3.math.Vector3f;

import car.ray.RayCarControl;
import drive.ICheckpointDrive;

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
	public void run() {
		Vector3f atPos = race.getNextCheckpoint(data.car);
		if (atPos == null) {
			runBehaviour(justBrake);
			return;
		}
        
        runBehaviour(driveAt);

        runBehaviour(tryStuffIfStuck);
        runBehaviour(detectVeryLongFall);

        float velocity = data.car.vel.length();

        // very still, flip
        if (velocity < 0.05f && data.car.up.y < 0) {
            onEvent("Flip", true);
        }

        runBehaviour(applySelfTractionControl);

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
