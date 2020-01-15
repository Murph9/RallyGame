package car.ai;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

import car.ray.RayCarControl;
import car.ray.RayCar.GripHelper;
import game.DebugAppState;
import helper.H;
import helper.Log;
import service.ray.IPhysicsRaycaster;
import service.ray.RaycasterResult;

public abstract class CarAI {
	
	protected final RayCarControl car;
    protected final float BEST_LAT_FORCE;
    protected final float BEST_LONG_FORCE;
    
    protected IPhysicsRaycaster raycaster;
    private DebugAppState debug;

    private float reverseTimer;
    private float stuckTimer;

    private float fallTimer;

	public CarAI(RayCarControl car) {
        this.car = car;
		
        BEST_LAT_FORCE = GripHelper.calcMaxLoad(car.getCarData().wheelData[0].pjk_lat);
        BEST_LONG_FORCE = GripHelper.calcMaxLoad(car.getCarData().wheelData[0].pjk_long);

		//ignore all turning speed factor code for AIs
		car.onAction("IgnoreSteeringSpeedFactor", true, 1);
	}
	
	public void setDebugAppState(DebugAppState debug) {
		this.debug = debug;
    }
    public void setPhysicsRaycaster(IPhysicsRaycaster raycaster) {
        this.raycaster = raycaster;
    }

	public abstract void update(float tpf);
	
	protected void onEvent(String act, boolean ifdown) {
		onEvent(act, ifdown, ifdown ? 1 : 0);
	}
	protected void onEvent(String act, boolean ifdown, float amnt) {
		car.onAction(act, ifdown, amnt);
	}

	/** Only brake, no accel and no steering */
	protected void justBrake() {
		onEvent("Left", false);
		onEvent("Right", false);
		
		onEvent("Accel", false);
		onEvent("Brake", true);
	}

	/**
	 * Helper method direct the AI towards a point
	 * @param curPos Current AI car position
	 * @param targetPos Target location
	 */
	protected void driveAt(Vector3f targetPos) {
		Vector3f curPos = this.car.getPhysicsLocation();
		Matrix3f w_angle = car.getPhysicsRotationMatrix();
		Vector3f velocity = w_angle.invert().mult(car.vel);
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
			onEvent("Left", false);
			onEvent("Right", true, Math.abs(nowTurn) * reverse);
		} else {
			onEvent("Left", true, Math.abs(nowTurn) * reverse);
			onEvent("Right", false);
		}

		boolean accel = IfTooSlowForPoint(targetPos, curPos, this.car.getLinearVelocity());
		boolean targetInFront = curPos.subtract(targetPos).length() > curPos.add(car.forward).subtract(targetPos).length();
		if (accel && targetInFront && ifLowDriftAngle()) {
            //drive at point
            this.onEvent("Accel", true);
            this.onEvent("Brake", false);
        } else {
            //drive as best to point, and don't accel
            this.onEvent("Accel", false);
            this.onEvent("Brake", true);
		}
	}

    /** Calculates based on the ideal situation whether the car can make the point at the current speed */
	protected boolean IfTooSlowForPoint(Vector3f target, Vector3f pos, Vector3f speed) {
		return IfTooSlowForPoint(H.v3tov2fXZ(target), H.v3tov2fXZ(pos), H.v3tov2fXZ(speed));
	}
	/** Calculates based on the ideal situation whether the car can make the point at the current speed */
    protected boolean IfTooSlowForPoint(Vector2f target, Vector2f pos, Vector2f speed) {

        // r = (m*v*v)/f
        float bestRadius = this.car.getCarData().mass * speed.lengthSquared() / BEST_LAT_FORCE;

        // generate a cone that the car can reach using 2 circles on either side
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

        return distance1 > bestRadius && distance2 > bestRadius;
	}
    
    /** Detect a high drift angle, which might mean stop accelerating */
    protected boolean ifLowDriftAngle() {
        return this.car.driftAngle() < 5;
    }

    //TODO better helper ray cast method, to find out what to avoid
    
    /**Gets (in seconds) time till a collision */
    protected float forwardRayCollideTime() {
        RaycasterResult result = forwardRay();
        if (result == null)
            return Float.MAX_VALUE;

        Vector3f selfVel = car.getLinearVelocity();
        Vector3f otherVel = result.obj.getLinearVelocity();

        //if its not moving, calculate time to hit
        if (otherVel.length() < 0.01f) {
            return result.dist/selfVel.length();
        }

        return Float.MAX_VALUE;
        
        /*
        //TODO calcuate relative velocity correctly:
        // calculate relative velocity, so we don't randomly brake behind something
        Vector3f diffVel = otherVel.project(selfVel);

        //if the vector is away, then ignore this (detecting colinear but negative vectors)
        if (diffVel.normalize() != selfVel.normalize()) {
            return 9999999; //TODO don't use != please
        }
        
        float distanceBetween = car.getPhysicsLocation().distance(result.obj.getPhysicsLocation());
        return distanceBetween/diffVel.length();
        */
    }

    private RaycasterResult forwardRay() {
        if (raycaster == null) {
            Log.e("CarAI needs the raycaster set :(");
            return null;
        }

        RaycasterResult result = raycaster.castRay(car.getPhysicsLocation(),
                car.getLinearVelocity().normalize().mult(100), this.car.getPhysicsObject());
        if (result != null) {
            return result;
        }
        
        return null;
    }

    //TODO change these methods below to be CarAi 'behaviours' in their own class?

    protected void detectVeryLongFall(float tpf) {
        if (car.noWheelsInContact()) {
            //no wheels in contact for a while, then reset
            fallTimer += tpf;

            if (fallTimer > 7) {
                onEvent("Reset", true);
                fallTimer = 0;
            }
        } else {
            fallTimer = 0;
        }
    }

    protected void tryStuffIfStuck(float tpf) {
        if (reverseTimer > 0) {
            //reverse for a bit
            onEvent("Reverse", true);
            reverseTimer -= tpf;
            if (reverseTimer < 0) {
                onEvent("Reverse", false);
                onEvent("Right", false);
                onEvent("Left", false);
            }
        } else if (car.getLinearVelocity().length() < 0.5f) {
            stuckTimer += tpf;
            if (stuckTimer > 2) {
                reverseTimer = 3; //for 3 seconds
            }
        } else {
            stuckTimer = 0;
        }
    }
}
