package rallygame.car.ai;

import com.jme3.math.Vector3f;

import rallygame.car.ray.RayCarControl;
import rallygame.car.ray.RayCarControlInput;
import rallygame.drive.ICheckpointDrive;
import rallygame.helper.H;

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
        
        boolean tooFast = tooFastForNextCheckpoints(atPos);
        if (tooFast) {
            onEvent(RayCarControlInput.ACTION_ACCEL, false);
            onEvent(RayCarControlInput.ACTION_BRAKE, true);
        }

        tryStuffIfStuck(tpf);
        detectVeryLongFall(tpf);

        float velocity = car.vel.length();

        // very still, flip
        if (velocity < 0.05f && car.up.y < 0) {
            onEvent(RayCarControlInput.ACTION_FLIP, true);
        }

        applySelfTractionControl(tpf);

        //prevent hitting straight into walls
        if (doFowardRayCast) {
            float result = forwardRayCollideTime();
            if (result < 1.5f) {
                onEvent(RayCarControlInput.ACTION_BRAKE, true);
            }
        }
        
        // if going too slow speed up
        if (velocity < 2) {
            onEvent(RayCarControlInput.ACTION_ACCEL, true);
            onEvent(RayCarControlInput.ACTION_BRAKE, false);
        }
    }

    private boolean tooFastForNextCheckpoints(Vector3f atPos) {
        Vector3f[] checkpoints = race.getNextCheckpoints(car, 6);
        if (checkpoints == null)
            return false;

        Vector3f targetDir = car.location.subtract(atPos).normalize();
        for (int i = 0; i < checkpoints.length - 1; i++) {
        
            if (checkpoints[i].subtract(checkpoints[i+1]).normalize().dot(targetDir) > 0.9f) {
                //pretty colinear, ignore
            } else {
                Vector3f[] wall = this.getOuterWallFromCheckpoints(checkpoints[i], checkpoints[i+1]);
                boolean result = this.IfTooFastForWall(H.v3tov2fXZ(wall[0]), H.v3tov2fXZ(wall[1]),
                H.v3tov2fXZ(car.location), H.v3tov2fXZ(car.vel), H.v3tov2fXZ(atPos));
                if (result) {
                    return true;
                }
            }
        }

        return false;
    }
}
