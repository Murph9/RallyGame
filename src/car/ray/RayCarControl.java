package car.ray;

import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource.Status;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.input.InputManager;
import com.jme3.input.RawInputListener;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import car.JoystickEventListener;
import car.MyKeyListener;
import car.ai.ICarAI;
import car.data.CarDataConst;
import drive.DriveBase;
import drive.race.DriveRace;
import helper.Log;
import service.ray.PhysicsRaycaster;

//visual/input things
public class RayCarControl extends RayCarPowered implements ICarPowered, ICarControlled {

	private final Application app;
    private final RayWheelControl[] wheelControls;
    private final RayCarControlDebug debug;
    private final RayCarControlInput input;
    
	//sound stuff
	private AudioNode engineSound;
	
	// control fields
	private float steerLeft;
	private float steerRight;
	private float steeringCurrent;
	private float brakeCurrent;
	private boolean handbrakeCurrent;
	private boolean ignoreSpeedFactor;

	// Car controlling things
	private final List<RawInputListener> controls;
	private ICarAI ai;
	
	private float travelledDistance;
	public float getDistanceTravelled() { return this.travelledDistance; }
	
	private final Node rootNode;
	private final PhysicsSpace space;
	
    private boolean added = false;
    
    //some directional world vectors for ease of direction/ai computation
    public Vector3f vel, forward, up, left, right;
	
	public RayCarControl(SimpleApplication app, CollisionShape shape, CarDataConst carData, Node rootNode) {
		super(shape, carData);
		this.app = app;
		this.space = app.getStateManager().getState(BulletAppState.class).getPhysicsSpace();
        this.debug = new RayCarControlDebug(this, false);
        this.input = new RayCarControlInput(this);

		vel = forward = up = left = right = new Vector3f();
		
		//init visual wheels
		this.wheelControls = new RayWheelControl[4];
		for (int i = 0; i < wheelControls.length; i++) {
			wheelControls[i] = new RayWheelControl(app, wheels[i], rootNode, carData.wheelOffset[i]);
		}
		
        this.controls = new LinkedList<RawInputListener>();
		
    	this.rootNode = rootNode;
        this.rootNode.addControl(rbc);
        space.addTickListener(this);
        
        setEnabled(true);
	}
	
	public void update(float tpf) {
        if (!this.rbEnabled())
            return; //don't update if the rigid body isn't

		if (ai != null) {
			ai.update(tpf);
		}
		
		if (engineSound != null && engineSound.getStatus() == Status.Playing) {
			//if sound exists
			float pitch = FastMath.clamp(0.5f+1.5f*((float)curRPM/(float)carData.e_redline), 0.5f, 2);
			engineSound.setPitch(pitch);
			
			float volume = 0.75f + this.accelCurrent * 0.25f;
			engineSound.setVolume(volume);
		}
	}
	public void giveSound(AudioNode audio) {
		if (engineSound != null) 
			rootNode.detachChild(engineSound); //in case we have 2 sounds running

		engineSound = audio;
		engineSound.setVelocityFromTranslation(true); //prevent camera based doppler?
		engineSound.setLooping(true);
		engineSound.play();
		rootNode.attachChild(engineSound);
	}
	
	@Override
	public void prePhysicsTick(PhysicsSpace space, float tpf) {
        if (rbEnabled()) {
            Matrix3f playerRot = rbc.getPhysicsRotationMatrix();
            vel = rbc.getLinearVelocity();
            forward = playerRot.mult(Vector3f.UNIT_Z);
            up = playerRot.mult(Vector3f.UNIT_Y);
            left = playerRot.mult(Vector3f.UNIT_X);
            right = playerRot.mult(Vector3f.UNIT_X.negate());
            
            travelledDistance += rbc.getLinearVelocity().length()*tpf;
            
            //wheel turning logic
            steeringCurrent = 0;
            if (steerLeft != 0) //left
                steeringCurrent += getBestTurnAngle(steerLeft, 1);
            if (steerRight != 0) //right
                steeringCurrent -= getBestTurnAngle(steerRight, -1);
            //TODO 0.3 - 0.4 seconds from lock to lock seems okay from what ive seen
            steeringCurrent = FastMath.clamp(steeringCurrent, -carData.w_steerAngle, carData.w_steerAngle);
			
			updateControlInputs(steeringCurrent, brakeCurrent, handbrakeCurrent);
        }
        
        super.prePhysicsTick(space, tpf);

		for (int i = 0; i < this.wheelControls.length; i++) {
			this.wheelControls[i].viewUpdate(tpf, rbc.getLinearVelocity(), carData.susByWheelNum(i).min_travel);
		}

        debug.update(app);
	}
	
	private float getBestTurnAngle(float trySteerAngle, float sign) {
		if (ignoreSpeedFactor)
			return trySteerAngle;
		
		Vector3f local_vel = rbc.getPhysicsRotation().inverse().mult(rbc.getLinearVelocity());
		if (local_vel.z < 0 || ((-sign * this.driftAngle) < 0 && Math.abs(this.driftAngle) > carData.minDriftAngle * FastMath.DEG_TO_RAD)) {
            return trySteerAngle;
            //when going backwards, slow or needing to turning against drift, you get no speed factor
            //eg: car is pointing more left than velocity, and is also turning left
            //and drift angle needs to be large enough to matter
        }
		
		// steering factor = atan(0.08 * vel - 1) + maxAngle*PI/2 + maxLat
//		return Math.min(-maxAngle*FastMath.atan(0.3f*(local_vel - 1))
//				+ maxAngle*FastMath.PI/2 + this.wheels[0].maxLat*2, Math.abs(trySteerAngle));
		
        //TODO PLEASE FIX THIS
        //TODO max turn angle really should just be the best angle on the lat traction curve (while going straight)
        // return FastMath.atan2(Math.abs(local_vel.z), carData.wheelData[0].maxLat);
        // return carData.wheelData[0].maxLat + FastMath.DEG_TO_RAD * driftAngle;
		return trySteerAngle;
		
		// remember that this value is clamped after this method is called
	}

    public RayCarControlInput getInput() {
        return this.input;
    }
	
	/** Please only call this from CarManager */
	public void cleanup(Application app) {
		if (!this.controls.isEmpty()) {
			for (RawInputListener ril: this.controls)
				app.getInputManager().removeRawInputListener(ril);
			this.controls.clear();
		}
		
		if (this.engineSound != null) {
			rootNode.detachChild(this.engineSound);
			app.getAudioRenderer().stopSource(engineSound);
			engineSound = null;
		}
		
		for (RayWheelControl w: this.wheelControls) {
			w.cleanup();
		}
		
		space.removeTickListener(this);
		space.remove(this.rootNode);
	}
	

	public Node getRootNode() {
		return rootNode;
	}
	public CarDataConst getCarData() {
		return carData;
	}
	public void setCarData(CarDataConst data) {
		this.carData = data;
	}
	
	public void attachAI(ICarAI ai, boolean setNotPlayer) {
		this.ai = ai;
        this.ai.setPhysicsRaycaster(new PhysicsRaycaster(space));

		if (setNotPlayer) {
			//remove any controls on the car
			if (this.controls != null && !this.controls.isEmpty()) {
				for (RawInputListener ril : this.controls)
					app.getInputManager().removeRawInputListener(ril);
				this.controls.clear();
			}
		}
	}
	public ICarAI getAI() {
		return this.ai;
	}
	public void attachControls(InputManager im) {
		if (controls.size() > 0) {
			Log.e("attachControls already called");
			return;
		}
		
		this.controls.add(new MyKeyListener(this.getInput()));
		this.controls.add(new JoystickEventListener(this.getInput()));
		
		for (RawInputListener ril: this.controls)
			im.addRawInputListener(ril);
	}
    
    public boolean noWheelsInContact() {
        return !wheels[0].inContact && !wheels[1].inContact && !wheels[2].inContact && !wheels[3].inContact;
    }
	public RayWheelControl getWheel(int w_id) {
		return this.wheelControls[w_id];
    }
    public List<RayWheelControl> getDriveWheels() {
        List<RayWheelControl> results = new LinkedList<>();

        if (this.carData.driveFront) {
            results.add(getWheel(0));
            results.add(getWheel(1));
        }
        if (this.carData.driveRear) {
            results.add(getWheel(2));
            results.add(getWheel(3));
        }

        return results;
    }
	
    //#region physics ones, thinking this looks bad
    public PhysicsRigidBody getPhysicsObject() {
        return rbc;
    }

	public Vector3f getPhysicsLocation() {
		return rbc.getPhysicsLocation();
	}
	public Quaternion getPhysicsRotation() {
		return rbc.getPhysicsRotation();
	}
	public Matrix3f getPhysicsRotationMatrix() {
		return rbc.getPhysicsRotationMatrix();
	}
	public Vector3f getLinearVelocity() {
		return rbc.getLinearVelocity();
	}
	public Vector3f getAngularVelocity() {
		return rbc.getAngularVelocity();
	}
    public float getAngularDamping() {
        return rbc.getAngularDamping();
    }
    
	public void setPhysicsLocation(Vector3f pos) {
		rbc.setPhysicsLocation(pos);
	}
	public void setLinearVelocity(Vector3f vel) {
		rbc.setLinearVelocity(vel);
	}
	public void setPhysicsRotation(Matrix3f rot) {
		rbc.setPhysicsRotation(rot);
	}
	public void setPhysicsRotation(Quaternion rot) {
		rbc.setPhysicsRotation(rot);
	}
	public void setAngularVelocity(Vector3f vel) {
		rbc.setAngularVelocity(vel);
    }
    public void setAngularDamping(float value) {
        rbc.setAngularDamping(value);
    }
    //#endregion
	
	public float getCurrentVehicleSpeedKmHour() {
		if (vel == null)
			return 0;
		return vel.length() * 3.6f;
	}
	
	//enabled functions
	public void setEnabled(boolean enabled) {
        if (this.space != null) {
            this.rbc.setEnabled(enabled);
            this.enableSound(enabled);
            
            if (enabled && !added) {
                space.add(this.rootNode);
                added = true;
            } else if (!enabled && added) {
                space.remove(this.rootNode);
                added = false;
            }
        }
    }
    
	private void enableSound(boolean enabled) {
        if (engineSound == null)
            return;
        if (enabled)
            engineSound.play();
        else
            engineSound.pause();
    }

    // #region ICarControlled
    public ICarControlled getControlledState() { return (ICarControlled)this; }
    public void setSteerLeft(float value) { steerLeft = value; }
    public void setSteerRight(float value) { steerRight = value; }
	public void setAccel(float value) { accelCurrent = value; }
	public void setBraking(float value) { brakeCurrent = value; }
	public void setHandbrake(boolean value) { handbrakeCurrent = value; }
    public void setNitro(boolean value) { ifNitro = value; }
    public void jump() { 
        rbc.applyImpulse(carData.JUMP_FORCE, new Vector3f()); // push up
        Vector3f old = rbc.getPhysicsLocation();
        old.y += 2; // and move up
        rbc.setPhysicsLocation(old);

        Vector3f vel = rbc.getLinearVelocity();
        vel.y = 0;
        rbc.setLinearVelocity(vel);
    }
    public void flip() { 
        rbc.setPhysicsRotation(new Quaternion());
        rbc.setPhysicsLocation(rbc.getPhysicsLocation().add(new Vector3f(0, 1, 0)));
    }
    public void reset() {
        setPhysicsRotation(new Matrix3f());
        setLinearVelocity(new Vector3f());
        setAngularVelocity(new Vector3f());

        this.curRPM = 1000;
        for (RayWheel w : this.wheels) {
            w.radSec = 0; // stop rotation of the wheels
            w.inContact = false; // stop any forces for at least one physics frame
        }

        DriveBase drive = this.app.getStateManager().getState(DriveBase.class);
        Transform transform = null;
        if (drive != null) {
            transform = drive.resetTransform(this);
            drive.resetWorld();
        } else {
            // TODO DriveRace hack, just happens to be the same as the abstract class method
            DriveRace race = this.app.getStateManager().getState(DriveRace.class);
            transform = race.resetTransform(this);
        }

        setPhysicsLocation(transform.getTranslation());
        setPhysicsRotation(transform.getRotation());
    }
    public void reverse(boolean value) {
        if (value)
            curGear = REVERSE_GEAR_INDEX;
        else
            curGear = 1;
    }
    public void ignoreSpeedFactor(boolean value) { ignoreSpeedFactor = value; }
    public void ignoreTractionModel(boolean value) { 
        if (value)
            tractionEnabled = !tractionEnabled;
    }
    // #endregion

	// #region ICarPowered
    public ICarPowered getPoweredState() { return (ICarPowered)this; }
	public float accelCurrent() { return accelCurrent; }
	public float brakeCurrent() { return brakeCurrent; }
	public int curGear() { return this.curGear; }
	public float nitro() { return this.getNitroRemaining(); }
	public float steeringCurrent() { return this.steeringCurrent; }
	public int curRPM() { return this.curRPM; }
	public boolean ifHandbrake() { return this.handbrakeCurrent; }
    public float driftAngle() { return this.driftAngle; }
    // #endregion
}
