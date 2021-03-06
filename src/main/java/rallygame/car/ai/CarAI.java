package rallygame.car.ai;

import java.util.List;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

import rallygame.car.ray.GripHelper;
import rallygame.car.ray.RayCarControl;
import rallygame.car.ray.RayCarControlInput;
import rallygame.car.ray.RayWheel;
import rallygame.game.DebugAppState;
import rallygame.helper.H;
import rallygame.helper.Log;
import rallygame.helper.Trig;
import rallygame.service.ray.IPhysicsRaycaster;
import rallygame.service.ray.RaycasterResult;

public abstract class CarAI implements ICarAI {
    
    private static final float SLOW_SPEED_LIMIT = 4;

    protected final RayCarControl car;
    protected final RayCarControlInput input;
    protected final float BEST_LAT_FORCE;
    protected final float BEST_LONG_FORCE;
    
    protected IPhysicsRaycaster raycaster;
    private DebugAppState debug;

    private float reverseTimer;
    private float stuckTimer;

    private float fallTimer;

	public CarAI(RayCarControl car) {
        this.car = car;
        this.input = car.getInput();
		
        BEST_LAT_FORCE = GripHelper.calcMaxLoad(car.getCarData().wheelData[0].pjk_lat);
        BEST_LONG_FORCE = GripHelper.calcMaxLoad(car.getCarData().wheelData[0].pjk_long);

		//ignore all turning speed factor code for AIs
		onEvent("IgnoreSteeringSpeedFactor", true);
	}
	
	public final void setDebugAppState(DebugAppState debug) {
		this.debug = debug;
    }
    public final void setPhysicsRaycaster(IPhysicsRaycaster raycaster) {
        this.raycaster = raycaster;
    }

	public abstract void update(float tpf);
	
	protected final void onEvent(String act, boolean ifdown) {
		onEvent(act, ifdown, ifdown ? 1 : 0);
	}
	protected final void onEvent(String act, boolean ifdown, float amnt) {
		input.handleInput(act, ifdown, amnt);
    }


	/** Only brake, no accel and no steering */
	protected final void justBrake() {
		onEvent(RayCarControlInput.ACTION_LEFT, false);
		onEvent(RayCarControlInput.ACTION_RIGHT, false);
		
		onEvent(RayCarControlInput.ACTION_ACCEL, false);
		onEvent(RayCarControlInput.ACTION_BRAKE, true);
	}

	/**
	 * Helper method direct the AI towards a point
	 * @param curPos Current AI car position
	 * @param targetPos Target location
	 */
	protected final void driveAt(Vector3f targetPos) {
		Vector3f curPos = this.car.location;
		Quaternion w_angle = car.rotation;
		Vector3f velocity = w_angle.inverse().mult(car.vel);
		int reverse = (velocity.z < 0 ? -1 : 1);

		Vector3f w_forward = new Vector3f(car.forward); // this is already in world space
		w_forward.y = 0; // don't care for vertical directions
		w_forward.normalizeLocal();

		Vector3f targetDir = targetPos.subtract(curPos);
		targetDir.y = 0; // still no caring about the vertical
		targetDir.normalizeLocal();

		// angle between target and direction
		float angF = w_forward.angleBetween(targetDir);
		// and get the sign for the angle
		float ang = car.left.normalize().angleBetween(targetDir);

		// get attempted turn angle as pos or negative
		float nowTurn = angF * Math.signum(FastMath.HALF_PI - ang);

		// turn towards
		if (nowTurn < 0) {
			onEvent(RayCarControlInput.ACTION_LEFT, false);
			onEvent(RayCarControlInput.ACTION_RIGHT, true, Math.abs(nowTurn) * reverse);
		} else {
			onEvent(RayCarControlInput.ACTION_LEFT, true, Math.abs(nowTurn) * reverse);
			onEvent(RayCarControlInput.ACTION_RIGHT, false);
		}

		boolean accel = IfTooSlowForPoint(targetPos, curPos, this.car.vel);
		boolean targetInFront = curPos.subtract(targetPos).length() > curPos.add(car.forward).subtract(targetPos).length();
		if (accel && targetInFront) {
            if (ifDrifting()) {
                // aim at point
                this.onEvent(RayCarControlInput.ACTION_ACCEL, false);
                this.onEvent(RayCarControlInput.ACTION_BRAKE, false);
            } else { //drive at point
                this.onEvent(RayCarControlInput.ACTION_ACCEL, true);
                this.onEvent(RayCarControlInput.ACTION_BRAKE, false);
            }
        } else {
            //drive as best to point, and don't accel
            this.onEvent(RayCarControlInput.ACTION_ACCEL, false);
            this.onEvent(RayCarControlInput.ACTION_BRAKE, true, 0.3f);
		}
	}

    /** Calculates based on the ideal situation whether the car can make the point at the current speed */
	protected final boolean IfTooSlowForPoint(Vector3f target, Vector3f pos, Vector3f speed) {
		return IfTooSlowForPoint(H.v3tov2fXZ(target), H.v3tov2fXZ(pos), H.v3tov2fXZ(speed));
	}
	/** Calculates based on the ideal situation whether the car can make the point at the current speed */
    protected final boolean IfTooSlowForPoint(Vector2f target, Vector2f pos, Vector2f speed) {

        // r = (m*v*v)/f
        float bestRadius = this.car.getCarData().mass * speed.lengthSquared() / BEST_LAT_FORCE;

        // generate a curved cone that the car can reach using 2 circles on either side
        // using the speed as the tangent of the circles
        Vector2f radiusDir = new Vector2f(speed.y, -speed.x).normalize().mult(bestRadius);

        // circle 1
        Vector2f center1 = pos.add(radiusDir);
        float distance1 = target.subtract(center1).length();

        // circle 2
        Vector2f center2 = pos.add(radiusDir.negate());
        float distance2 = target.subtract(center2).length();

		if (debug != null) {
			debug.drawBox("aiTooFastForPointPos", ColorRGBA.White, H.v2tov3fXZ(target), 0.2f);
			debug.drawSphere("aiTooFastForPointc1", ColorRGBA.White, H.v2tov3fXZ(center1), bestRadius);
			debug.drawSphere("aiTooFastForPointc2", ColorRGBA.White, H.v2tov3fXZ(center2), bestRadius);
		}

        // finally return if the cone contains the target
        return distance1 > bestRadius && distance2 > bestRadius;
    }
    
    /** Similar to the too fast for point method, but it tries to avoid the wall as close as possible */
    protected final boolean IfTooFastForWall(Vector2f wallStart, Vector2f wallDir, Vector2f pos, Vector2f speed, Vector2f target) {
        // r = (m*v*v)/f
        float bestRadius = this.car.getCarData().mass * speed.lengthSquared() / BEST_LAT_FORCE;
        // generate a curved cone that the car can reach using 2 circles on either side
        // using the speed as the tangent of the circles
        Vector2f radiusDir = new Vector2f(speed.y, -speed.x).normalize().mult(bestRadius);

        // create both 'spheres'
        Vector2f center1 = pos.add(radiusDir);
        Vector2f center2 = pos.add(radiusDir.negate());
        
        if (debug != null) {
            Vector3f wallStart3f = H.v2tov3fXZ(wallStart);
            wallStart3f.y = this.car.location.y;
            Vector3f wallDir3f = H.v2tov3fXZ(wallDir);
            debug.drawArrow("aiTooFastForWall", ColorRGBA.Red, wallStart3f.add(0, 0.2f, 0), wallDir3f);
        }

        // calc if cone contains the wall
        float dist1 = Trig.distFromRay(wallStart, wallDir, center1);
        float dist2 = Trig.distFromRay(wallStart, wallDir, center2);
        return Math.max(dist1, dist2) < bestRadius; //pick the inner sphere (which should be further from the ray)
    }
    
    protected Vector3f[] getOuterWallFromCheckpoints(Vector3f checkpoint1, Vector3f checkpoint2, float roadWidth) {
        Vector3f wallDir = checkpoint2.subtract(checkpoint1);
        Vector3f wallStart = checkpoint1;

        Vector3f normal = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y).mult(wallDir);
        Vector3f newPosX = wallStart.add(normal.normalize().mult(roadWidth));
        Vector3f newPosXM = wallStart.add(normal.normalize().negate().mult(roadWidth));

        // pick the futhest pos so its the opposite wall
        if (newPosX.distance(car.location) < newPosXM.distance(car.location)) {
            return new Vector3f[] { newPosXM, wallDir };
        }

        return new Vector3f[] { newPosX, wallDir };
    }
    
    /** Detect a high drift angle, which might mean stop accelerating */
    protected final boolean ifDrifting() {
        if (this.car.vel.length() < SLOW_SPEED_LIMIT)
            return false; //can't drift slowly

        if (this.car.angularVel.length() > 0.7f) {
            return true; //predict starting a drift (should really be compared with size)
        }

        return this.car.driftAngle() >= 5;
    }

    /**Gets (in seconds) time till a collision */
    protected final float forwardRayCollideTime() {
        RaycasterResult result = forwardRay();
        if (result == null)
            return Float.MAX_VALUE;

        Vector3f selfVel = car.vel;
        Vector3f otherVel = result.obj.getLinearVelocity();

        //if its not moving, calculate time to hit
        if (otherVel.length() < 0.01f) {
            return result.dist/selfVel.length();
        }
        
        // calculate difference in relative velocity, so we don't randomly brake behind something
        Vector3f otherRelVel = otherVel.project(selfVel);
        if (otherRelVel.length() > selfVel.length())
            return Float.MAX_VALUE; //if their projected velocity is larger, no collision
        
        Vector3f selfDiffWithRel = selfVel.subtract(otherRelVel);

        float distanceBetween = car.location.distance(result.obj.getPhysicsLocation());
        return distanceBetween/selfDiffWithRel.length();
    }

    private RaycasterResult forwardRay() {
        if (raycaster == null) {
            Log.e("CarAI needs the raycaster set :(");
            return null;
        }

        RaycasterResult result = raycaster.castRay(car.location,
                car.vel.normalize().mult(100), this.car.getPhysicsObject());
        if (result != null) {
            return result;
        }
        
        return null;
    }

    //TODO change these methods below to be CarAi 'behaviours' in their own classes? Or some kind of composition?

    protected final void detectVeryLongFall(float tpf) {
        if (car.noWheelsInContact()) {
            //no wheels in contact for a while, then reset
            fallTimer += tpf;

            if (fallTimer > 7) {
                onEvent(RayCarControlInput.ACTION_RESET, true);
                fallTimer = 0;
            }
        } else {
            fallTimer = 0;
        }
    }

    protected final void tryStuffIfStuck(float tpf) {
        if (reverseTimer > 0) {
            //reverse for a bit
            onEvent(RayCarControlInput.ACTION_REVERSE, true);
            reverseTimer -= tpf;
            if (reverseTimer < 0) {
                onEvent(RayCarControlInput.ACTION_REVERSE, false);
                onEvent(RayCarControlInput.ACTION_RIGHT, false);
                onEvent(RayCarControlInput.ACTION_LEFT, false);
            }
        } else if (car.vel.length() < 0.5f) {
            stuckTimer += tpf;
            if (stuckTimer > 2) {
                reverseTimer = 3; //for 3 seconds
            }
        } else {
            stuckTimer = 0;
        }
    }

    protected final void applySelfTractionControl(float tpf) {
        if (car.vel.length() <= SLOW_SPEED_LIMIT)
            return;
        
        // reduce excess wheel slipping
        List<RayWheel> wheels = car.getDriveWheels();
        float gripSum = 0;
        for (RayWheel wheel : wheels) {
            gripSum += wheel.skidFraction;
        }
        if (gripSum > wheels.size() && ifDrifting()) {
            onEvent(RayCarControlInput.ACTION_ACCEL, false);
        }
    }

    protected final Vector3f calcBetterCheckpointPos(Vector3f pos, float offset, Vector3f c1, Vector3f c2) {
        Vector2f newPos = Trig.pointOnCircleClosestTo(H.v3tov2fXZ(c1), offset, H.v3tov2fXZ(pos), H.v3tov2fXZ(c2));
        Vector3f newPos3 = H.v2tov3fXZ(newPos);
        newPos3.y = pos.y;
        return newPos3;
    }
}
