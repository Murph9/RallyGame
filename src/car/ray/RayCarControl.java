package car.ray;

import java.util.LinkedList;
import java.util.List;

import com.jme3.audio.AudioNode;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.input.RawInputListener;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import car.JoystickEventListener;
import car.MyKeyListener;
import car.ai.CarAI;
import game.App;
import helper.H;
import helper.Log;

//visual/input things
public class RayCarControl extends RayCarPowered {

	private final RayWheelControl[] wheelControls;
	
	//sound stuff
	public AudioNode engineSound;
	
	// Car controlling things
	private final List<RawInputListener> controls;
	private CarAI ai;
	
	public float travelledDistance;
	
	private final Node rootNode;
	private final PhysicsSpace space;
	
    private boolean enabled = false;
    private boolean added = false;
    
    //some directional world vectors for ease of direction/ai computation
    public Vector3f vel, forward, up, left, right;
	
	public RayCarControl(PhysicsSpace space, CollisionShape shape, CarDataConst carData, Node rootNode) {
		super(shape, carData);
		this.space = space;

		//init visual wheels
		this.wheelControls = new RayWheelControl[4];
		for (int i = 0; i < wheelControls.length; i++) {
			wheelControls[i] = new RayWheelControl(wheels[i], rootNode, carData.wheelOffset[i]);
		}
		
        this.controls = new LinkedList<RawInputListener>();
		
		//its possible to shift the center of gravity offset for the collision shape (TODO add value to CarDataConst)

    	this.rootNode = rootNode;
        this.rootNode.addControl(rbc);
        space.addTickListener(this);
        
        setEnabled(true);
	}
	
	public void update(float tpf) {
		if (ai != null) {
			ai.update(tpf);
		}
		
		if (engineSound != null) {
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
		engineSound.setLooping(true);
		engineSound.play();
		rootNode.attachChild(engineSound);
	}
	public void enableSound(boolean value) {
		if (engineSound == null)
			return;
		if (value)
			engineSound.play();
		else
			engineSound.pause();
	}
	
	@Override
	public void prePhysicsTick(PhysicsSpace space, float tpf) {
		Matrix3f playerRot = rbc.getPhysicsRotationMatrix();
		vel = rbc.getLinearVelocity();
		forward = playerRot.mult(new Vector3f(0,0,1));
		up = playerRot.mult(new Vector3f(0,1,0));
		left = playerRot.mult(new Vector3f(1,0,0));
		right = playerRot.mult(new Vector3f(-1,0,0));
		
		travelledDistance += rbc.getLinearVelocity().length()*tpf;
		
		//wheel turning logic
		steeringCurrent = 0;
		if (steerLeft != 0) //left
			steeringCurrent += getMaxSteerAngle(steerLeft, 1);
		if (steerRight != 0) //right
			steeringCurrent -= getMaxSteerAngle(steerRight, -1);
		//TODO 0.3 - 0.4 seconds from lock to lock seems okay from what ive seen
		steeringCurrent = FastMath.clamp(steeringCurrent, -carData.w_steerAngle, carData.w_steerAngle);
		setSteering(steeringCurrent);
		
		setBraking(brakeCurrent);
		setHandbrake(handbrakeCurrent);
		
		super.prePhysicsTick(space, tpf);
		
		for (int i = 0; i < this.wheelControls.length; i++) {
			this.wheelControls[i].physicsUpdate(tpf, rbc.getLinearVelocity(), carData.susByWheelNum(i).min_travel);
		}
	}
	
	private float getMaxSteerAngle(float trySteerAngle, float sign) {
		if (ignoreSpeedFactor)
			return trySteerAngle;
		
		Vector3f local_vel = rbc.getPhysicsRotation().inverse().mult(rbc.getLinearVelocity());
		if (local_vel.z < 0 || ((-sign * this.driftAngle) < 0 && Math.abs(this.driftAngle) > carData.minDriftAngle * FastMath.DEG_TO_RAD)) 
			return trySteerAngle; //when going backwards, slow or needing to turning against drift, you get no speed factor
		//eg: car is pointing more left than velocity, and is also turning left
		//and drift angle needs to be large enough to matter
		
		float maxAngle = carData.w_steerAngle/2;
		//steering factor = atan(0.08 * vel - 1) + maxAngle*PI/2 + maxLat //TODO what is this 0.08f
		float value = Math.min(-maxAngle*FastMath.atan(0.3f*(rbc.getLinearVelocity().length() - 1))
				+ maxAngle*FastMath.PI/2 + this.wheels[0].maxLat*2, Math.abs(trySteerAngle));
		
		//TODO turn back value should be vel dir + maxlat instead of just full lock
		
		//remember that this value is clamped after this method is called
		return value;
	}

	public void onAction(String binding, boolean value, float tpf) {
		if (binding == null) {
			helper.Log.p("No binding given?");
			return;
		}
		
		//value == 'if pressed (down) - we get one back when its unpressed (up)'
		//tpf is being used as the value for joysticks. deal with it
		switch (binding) {
			case "Left": 
				steerLeft = tpf;
				break;
	
			case "Right":
				steerRight = tpf;
				break;
	
			case "Accel":
				accelCurrent = tpf;
				break;
	
			case "Brake":
				brakeCurrent = tpf;
				break;
	
			case "Nitro":
				if (carData.nitro_on) {
					ifNitro = value;
					if (!ifNitro)
						this.nitroTimeout = 2;
				}
				break;
	
			case "Jump":
				if (value) {
					rbc.applyImpulse(carData.JUMP_FORCE, new Vector3f(0,0,0)); //push up
					Vector3f old = rbc.getPhysicsLocation();
					old.y += 2; //and move up
					rbc.setPhysicsLocation(old);
				}
				break;
	
			case "Handbrake":
				handbrakeCurrent = value;
				break;
	
			case "Flip":
				if (value) this.flipMe();
				break;
				
			case "Reset":
				if (value) this.reset();
				break;

			case "Rotate180":
				if (value) this.rotate180();
				break;
	
			case "Reverse":
				if (value) curGear = 0;
				else curGear = 1;
				break;
				
			case "IgnoreSteeringSpeedFactor":
				ignoreSpeedFactor = value;
				break;
	
			default:
				//nothing
				System.err.println("unknown binding: "+binding);
				break;
		}
	}
	
	private void flipMe() {
		rbc.setPhysicsRotation(new Quaternion());
		rbc.setPhysicsLocation(rbc.getPhysicsLocation().add(new Vector3f(0,1,0)));
	}
	private void reset() {
		setPhysicsRotation(new Matrix3f());
		setLinearVelocity(new Vector3f());
		setAngularVelocity(new Vector3f());
		
		this.curRPM = 1000;
		for (RayWheel w: this.wheels) {
			w.radSec = 0; //stop rotation of the wheels
		}
		
		if (App.rally.drive == null) 
			return;
		
		Vector3f pos = App.rally.drive.world.getStartPos();
		Matrix3f rot = App.rally.drive.world.getStartRot();
		if (pos != null && rot != null) {
			setPhysicsLocation(pos);
			setPhysicsRotation(rot);
			setAngularVelocity(new Vector3f());
		}
		
		App.rally.drive.reset(); //TODO this is a hack, the world state should be listening to the event instead
	}
	private void rotate180() {
		rbc.setPhysicsRotation(new Quaternion().fromAngleAxis(FastMath.PI, new Vector3f(0,1,0)));
	}
	
	
	//Please only call this from the car manager
	public void cleanup() {
		if (this.controls != null && !this.controls.isEmpty()) {
			for (RawInputListener ril: this.controls)
				App.rally.getInputManager().removeRawInputListener(ril);
			this.controls.clear();
		}
		
		if (this.engineSound != null) {
			rootNode.detachChild(this.engineSound);
			App.rally.getAudioRenderer().stopSource(engineSound);
			engineSound = null;
		}
		
		for (RayWheelControl w: this.wheelControls) {
			w.cleanup();
		}
		
		space.removeTickListener(this);
		space.remove(this.rootNode);
		Log.e("Write more RayCarControl.cleanup()");
	}
	
	//Phyics get/set methods
	public Node getRootNode() {
		return rootNode;
	}
	public CarDataConst getCarData() {
		return carData;
	}
	public void setCarData(CarDataConst data) {
		this.carData = data;
		this.carData.load();
	}
	
	public void attachAI(CarAI ai) {
		this.ai = ai;
	}
	public CarAI getAI() {
		return this.ai;
	}
	public void attachControls() {
		if (controls.size() > 0) {
			Log.e("attachControls already called");
			return;
		}
		
		this.controls.add(new MyKeyListener(this));
		this.controls.add(new JoystickEventListener(this));
		
		for (RawInputListener ril: this.controls)
			App.rally.getInputManager().addRawInputListener(ril);
	}
	
	public RayWheelControl getWheel(int w_id) {
		return this.wheelControls[w_id];
	}
	
	//physics ones, thinking this looks bad
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
	
	public float getCurrentVehicleSpeedKmHour() {
		if (vel == null)
			return 0;
		return vel.length() * 3.6f;
	}

	public PhysicsRigidBody getPhysicsObject() {
		return rbc;
	}
	
	public String statsString() {
		return H.round3f(this.getPhysicsLocation(), 2)
		 + "\nspeed:"+ H.round3f(vel, 2) + "m/s\nRPM:" + curRPM
		 + "\nengine:" + engineTorque + "\ndrag:" + dragValue 
		 + "N\nG Forces:" + H.roundDecimal(planarGForce.length()/rbc.getGravity().length(), 2);
	}
	
	
	//enabled functions
	public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (this.space != null) {
        	this.rbc.setEnabled(enabled);
        	
            if (enabled && !added) {
            	Log.p("added");
                space.add(this.rootNode);
                
                added = true;
            } else if (!enabled && added) {
                space.remove(this.rootNode);
                added = false;
            }
        }
    }
	public boolean isEnabled() {
        return enabled;
    }
}
