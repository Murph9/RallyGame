package car.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

import car.ray.GripHelper;
import car.ray.RayCarControl;
import car.ray.RayCarControlInput;
import car.ray.RayWheelControl;
import game.DebugAppState;
import helper.H;
import helper.Log;
import service.ray.IPhysicsRaycaster;
import service.ray.RaycasterResult;

public abstract class CarAI implements ICarAI {
    
    private static final float SLOW_SPEED_LIMIT = 4;

    protected final RayCarControlInput input;
    protected final float BEST_LAT_FORCE;
    protected final float BEST_LONG_FORCE;
    
    protected IPhysicsRaycaster raycaster;
    private DebugAppState debug;

    protected final CarAIData data;

	public CarAI(RayCarControl car) {
        this.input = car.getInput();

        this.data = new CarAIData(this, car);
        
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

    public final void update(float tpf) {
        this.data.tpf = tpf;
        this.run();
    }
    
    protected final void setTarget(Vector3f target) {
        this.data.target = target;
    }

	protected abstract void run();
	
	protected final void onEvent(String act, boolean ifdown) {
		onEvent(act, ifdown, ifdown ? 1 : 0);
	}
	protected final void onEvent(String act, boolean ifdown, float amnt) {
		input.handleInput(act, ifdown, amnt);
    }

    /** Calculates based on the ideal situation whether the car can make the point at the current speed */
	protected final boolean IfTooSlowForPoint(Vector3f target, Vector3f pos, Vector3f speed) {
		return IfTooSlowForPoint(H.v3tov2fXZ(target), H.v3tov2fXZ(pos), H.v3tov2fXZ(speed));
	}
	/** Calculates based on the ideal situation whether the car can make the point at the current speed */
    protected final boolean IfTooSlowForPoint(Vector2f target, Vector2f pos, Vector2f speed) {

        // r = (m*v*v)/f
        float bestRadius = data.car.getCarData().mass * speed.lengthSquared() / BEST_LAT_FORCE;

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
    protected final boolean ifDrifting() {
        if (data.car.vel.length() < SLOW_SPEED_LIMIT)
            return false; //can't drift slowly

        if (data.car.angularVel.length() > 0.7f) {
            return true; //predict starting a drift (should really be compared with size)
        }

        return data.car.driftAngle() >= 5;
    }

    /**Gets (in seconds) time till a collision */
    protected final float forwardRayCollideTime() {
        RaycasterResult result = forwardRay();
        if (result == null)
            return Float.MAX_VALUE;

        Vector3f selfVel = data.car.vel;
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

        float distanceBetween = data.car.location.distance(result.obj.getPhysicsLocation());
        return distanceBetween/selfDiffWithRel.length();
    }

    private RaycasterResult forwardRay() {
        if (raycaster == null) {
            Log.e("CarAI needs the raycaster set :(");
            return null;
        }

        RaycasterResult result = raycaster.castRay(data.car.location,
                data.car.vel.normalize().mult(100), data.car.getPhysicsObject());
        if (result != null) {
            return result;
        }
        
        return null;
    }

    public void runBehaviour(CarAIBehaviour behaviour) {
        behaviour.run(this.data);
    }

    public static CarAIBehaviour driveAt = new CarAIBehaviour((data) -> {
        // Direct the AI towards a point
        Vector3f curPos = data.car.location;
        Quaternion w_angle = data.car.getPhysicsObject().getPhysicsRotation();
        Vector3f velocity = w_angle.inverse().mult(data.car.vel);
        int reverse = (velocity.z < 0 ? -1 : 1);

        Vector3f w_forward = new Vector3f(data.car.forward); // this is already in world space
        w_forward.y = 0; // don't care for vertical directions
        w_forward.normalizeLocal();

        Vector3f targetDir = data.target.subtract(curPos);
        targetDir.y = 0; // still no caring about the vertical
        targetDir.normalizeLocal();

        // angle between target and direction
        float angF = w_forward.angleBetween(targetDir);
        // and get the sign for the angle
        float ang = data.car.left.normalize().angleBetween(targetDir);

        // get attempted turn angle as pos or negative
        float nowTurn = angF * Math.signum(FastMath.HALF_PI - ang);

        // turn towards
        if (nowTurn < 0) {
            data.ai.onEvent("Left", false);
            data.ai.onEvent("Right", true, Math.abs(nowTurn) * reverse);
        } else {
            data.ai.onEvent("Left", true, Math.abs(nowTurn) * reverse);
            data.ai.onEvent("Right", false);
        }

        boolean accel = data.ai.IfTooSlowForPoint(data.target, curPos, data.car.vel);
        boolean targetInFront = curPos.subtract(data.target).length() > curPos.add(data.car.forward)
                .subtract(data.target).length();
        if (accel && targetInFront) {
            if (data.ai.ifDrifting()) {
                // aim at point
                data.ai.onEvent("Accel", false);
                data.ai.onEvent("Brake", false);
            } else { // drive at point
                data.ai.onEvent("Accel", true);
                data.ai.onEvent("Brake", false);
            }
        } else {
            // drive as best to point, and don't accel
            data.ai.onEvent("Accel", false);
            data.ai.onEvent("Brake", true);
        }
    });

    public static CarAIBehaviour justBrake = new CarAIBehaviour((data) -> {
        /** Only brake, no accel and no steering */
		data.ai.onEvent("Left", false);
		data.ai.onEvent("Right", false);
		
		data.ai.onEvent("Accel", false);
		data.ai.onEvent("Brake", true);
    });

    public static CarAIBehaviour detectVeryLongFall = new CarAIBehaviour((data) -> {
        float fallTimer = (float) data.selfData.get("fallTimer");
        
        if (data.car.noWheelsInContact()) {
            // no wheels in contact for a while, then reset
            fallTimer += data.tpf;

            if (fallTimer > 7) {
               data.ai.onEvent("Reset", true);
                fallTimer = 0;
            }
        } else {
            fallTimer = 0;
        }

        data.selfData.put("fallTimer", fallTimer);
    });

    public static CarAIBehaviour tryStuffIfStuck = new CarAIBehaviour((data) -> {
        float stuckTimer = (float) data.selfData.get("stuckTimer");
        float reverseTimer = (float) data.selfData.get("reverseTimer");
        
        if (reverseTimer > 0) {
            //reverse for a bit
            data.ai.onEvent("Reverse", true);
            reverseTimer -= data.tpf;
            if (reverseTimer < 0) {
                data.ai.onEvent("Reverse", false);
                data.ai.onEvent("Right", false);
                data.ai.onEvent("Left", false);
            }
        } else if (data.car.vel.length() < 0.5f) {
            stuckTimer += data.tpf;
            if (stuckTimer > 2) {
                reverseTimer = 3; //for 3 seconds
            }
        } else {
            stuckTimer = 0;
        }

        data.selfData.put("stuckTimer", stuckTimer);
        data.selfData.put("reverseTimer", reverseTimer);
    });

    public static CarAIBehaviour applySelfTractionControl = new CarAIBehaviour((data) -> {
        // reduce excess wheel slipping
        List<RayWheelControl> wheels = data.car.getDriveWheels();
        float gripSum = 0;
        for (RayWheelControl wheel : wheels) {
            gripSum += wheel.getRayWheel().skidFraction;
        }
        if (data.car.vel.length() > SLOW_SPEED_LIMIT && gripSum > wheels.size()) {
            data.ai.onEvent("Accel", false);
        }
    });
}

class CarAIData {
    final CarAI ai;
    final RayCarControl car;
    final Map<String, Object> selfData;

    float tpf;
    Vector3f target;

    public CarAIData(CarAI ai, RayCarControl car) {
        this.ai = ai;
        this.car = car;
        this.selfData = new HashMap<>();
    }
}

class CarAIBehaviour {

    private final Consumer<CarAIData> function;

    public CarAIBehaviour(Consumer<CarAIData> function) {
        this.function = function;
    }

    public void run(CarAIData data) {
        this.function.accept(data);
    }
}