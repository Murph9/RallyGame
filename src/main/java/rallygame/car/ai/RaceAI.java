package rallygame.car.ai;

import com.jme3.math.Vector3f;

import rallygame.car.ray.RayCarControl;
import rallygame.car.ray.RayCarControlInput;
import rallygame.helper.H;
import rallygame.service.checkpoint.ICheckpointProgress;

public class RaceAI extends CarAI {

    private final ICheckpointProgress checkpointProgress;
    private final boolean doFowardRayCast;
    private float roadWidth;
    private float catchUp;
    
    public RaceAI(RayCarControl car, ICheckpointProgress checkpointProgress) {
        this(car, checkpointProgress, true);
    }
	public RaceAI(RayCarControl car, ICheckpointProgress checkpointProgress, boolean doForwardRayCast) {
		super(car);
        this.checkpointProgress = checkpointProgress;
        this.doFowardRayCast = doForwardRayCast;
        roadWidth = 5;
    }
    public void setRoadWidth(float roadWidth) {
        this.roadWidth = roadWidth;
    }
    public void useCatchUp(float value) {
        this.catchUp = value; // a usual value is ~0.3f
    }

    @Override
	public void update(float tpf) {
		Vector3f atPos = checkpointProgress.getNextCheckpoint(car);
		if (atPos == null) {
			justBrake();
			return;
		}
        
        Vector3f[] nextCheckpoints = checkpointProgress.getNextCheckpoints(car, 2);
        Vector3f pos = calcBetterCheckpointPos(checkpointProgress.getLastCheckpoint(car), roadWidth*0.9f, nextCheckpoints[0], nextCheckpoints[1]);
        driveAt(pos);
        
        boolean tooFast = tooFastForNextCheckpoints(atPos, roadWidth);
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

        if (catchUp > 0) {
            Vector3f grav = car.getPhysicsObject().getGravity();
            Vector3f targetDir = atPos.subtract(car.location);
            car.getPhysicsObject().applyImpulse(targetDir.normalize().mult(car.getCarData().mass*grav.length()*tpf*catchUp), Vector3f.ZERO);
            // TODO it goes way too fast for corners, might need a better slow down:
            // car.getPhysicsObject().applyImpulse(car.vel.negate().normalize().mult(car.getCarData().mass*grav.length()*tpf*0.03f), Vector3f.ZERO);
        }
    }

    private boolean tooFastForNextCheckpoints(Vector3f atPos, float roadWidth) {
        Vector3f[] checkpoints = checkpointProgress.getNextCheckpoints(car, 3);
        if (checkpoints == null)
            return false;

        Vector3f targetDir = car.location.subtract(atPos).normalize();
        for (int i = 0; i < checkpoints.length - 1; i++) {
            if (checkpoints[i] == null || checkpoints[i + 1] == null)
                continue;

            if (checkpoints[i].subtract(checkpoints[i+1]).normalize().dot(targetDir) > 0.97f) {
                //pretty colinear, ignore
            } else {
                Vector3f[] wall = this.getOuterWallFromCheckpoints(checkpoints[i], checkpoints[i+1], roadWidth);
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
