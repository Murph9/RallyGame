package rallygame.car.ray;

import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.input.InputManager;
import com.jme3.input.RawInputListener;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import rallygame.car.JoystickEventListener;
import rallygame.car.MyKeyListener;
import rallygame.car.ai.ICarAI;
import rallygame.car.data.CarDataConst;
import rallygame.drive.IDrive;
import rallygame.helper.Log;
import rallygame.service.averager.AverageFloatFramerate;
import rallygame.service.averager.IAverager;
import rallygame.service.ray.PhysicsRaycaster;

//visual/input things
public class RayCarControl extends RayCarPowered implements ICarPowered, ICarControlled {

    private final Application app;
    private final RayCarVisuals visuals;
    private final RayCarControlDebug debug;
    private final RayCarControlInput input;
        
    // Steering averager (to get smooth left <-> right)
    private AverageFloatFramerate steeringAverager;

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
    
    public float getDistanceTravelled() { return this.travelledDistance; }
    
    private final Node rootNode;
    private final PhysicsSpace space;
    
    private boolean added = false;
    
    //some fields to prevent needing to call GetPhysicsObject a lot
    public Vector3f vel, forward, up, left, right;
    public Vector3f angularVel;
    public Quaternion rotation;
    public Vector3f location;
    
    public RayCarControl(SimpleApplication app, CollisionShape shape, CarDataConst carData) {
        super(shape, carData); //TODO this collision shape is an arugment against extending RayCarPowered
        this.app = app;
        this.space = app.getStateManager().getState(BulletAppState.class).getPhysicsSpace();
        this.debug = new RayCarControlDebug(this, false);
        this.input = new RayCarControlInput(this);

        vel = forward = up = left = right = location = angularVel = new Vector3f();
        rotation = new Quaternion();

        this.steeringAverager = new AverageFloatFramerate(0.2f, IAverager.Type.Weighted);

        this.rootNode = new Node("Car:" + carData);
        this.rootNode.addControl(rbc);

        this.visuals = new RayCarVisuals(app, this);
        
        this.controls = new LinkedList<RawInputListener>();
        
        space.addTickListener(this);
        setEnabled(true);
    }
    
    public void update(float tpf) {
        if (!this.rbEnabled())
            return; //don't update if the rigid body isn't

        if (ai != null) {
            ai.update(tpf);
        }

        visuals.viewUpdate(tpf);
    }

    public void giveSound(AudioNode audio) {
        visuals.giveSound(audio);
    }
    
    @Override
    public void prePhysicsTick(PhysicsSpace space, float tpf) {
        if (rbEnabled()) {
            rotation = rbc.getPhysicsRotation();
            vel = rbc.getLinearVelocity();
            forward = rotation.mult(Vector3f.UNIT_Z);
            up = rotation.mult(Vector3f.UNIT_Y);
            left = rotation.mult(Vector3f.UNIT_X);
            right = rotation.mult(Vector3f.UNIT_X.negate());
            angularVel = rbc.getAngularVelocity();
            location = rbc.getPhysicsLocation();
            
            //wheel turning logic
            steeringCurrent = 0;
            if (steerLeft != 0) //left
                steeringCurrent += getBestTurnAngle(steerLeft, 1);
            if (steerRight != 0) //right
                steeringCurrent -= getBestTurnAngle(steerRight, -1);
            steeringCurrent = ignoreSpeedFactor ? steeringCurrent : steeringAverager.get(steeringCurrent, tpf);
            steeringCurrent = FastMath.clamp(steeringCurrent, -carData.w_steerAngle, carData.w_steerAngle);

            updateControlInputs(steeringCurrent, brakeCurrent, handbrakeCurrent);
        }
        
        super.prePhysicsTick(space, tpf);

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

        if (local_vel.length() < 8) //prevent being able to turn at slow speeds
            return trySteerAngle;

        // this is magic, but: minimum should be bast pjk lat, but it doesn't catch up to the turning angle required
        // so we just add some of the angular vel value to it
        return this.wheels[0].data.maxLat + angularVel.length()*0.25f; //constant needs minor adjustments
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
        
        visuals.cleanup(app);
        
        space.removeTickListener(this);
        space.remove(this.rootNode);
    }
    

    public Node getRootNode() {
        return rootNode;
    }
    public CarDataConst getCarData() {
        return carData;
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
    public RayWheel getWheel(int w_id) {
        return this.wheels[w_id];
    }
    public List<RayWheel> getDriveWheels() {
        List<RayWheel> results = new LinkedList<>();

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
    
    public PhysicsRigidBody getPhysicsObject() {
        return rbc;
    }
    
    /**
     * Set Physics state for the RayCar, basically a helper method for:
     * PhysicsRigidBody.setPhysicsLocation(position)
     * PhysicsRigidBody.setLinearVelocity(velocity)
     * PhysicsRigidBody.setPhysicsRotation(rotation)
     * PhysicsRigidBody.setAngularVelocity(angularVel)
     */
    public void setPhysicsProperties(Vector3f position, Vector3f velocity, Quaternion rotation, Vector3f angularVel) {
        if (position != null)
            rbc.setPhysicsLocation(position);
        if (velocity != null)
            rbc.setLinearVelocity(velocity);
        if (rotation != null)
            rbc.setPhysicsRotation(rotation);
        if (angularVel != null)
            rbc.setAngularVelocity(angularVel);
    }
    
    public float getCurrentVehicleSpeedKmHour() {
        if (vel == null)
            return 0;
        return vel.length() * 3.6f;
    }
    
    //enabled functions
    public void setEnabled(boolean enabled) {
        if (this.space != null) {
            this.rbc.setEnabled(enabled);
            visuals.enableSound(enabled);
            
            if (enabled && !added) {
                space.add(this.rootNode);
                added = true;
            } else if (!enabled && added) {
                space.remove(this.rootNode);
                added = false;
            }
        }
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
        rbc.setPhysicsRotation(new Quaternion());
        rbc.setLinearVelocity(new Vector3f());
        rbc.setAngularVelocity(new Vector3f());

        this.curRPM = 1000;
        for (RayWheel w : this.wheels) {
            w.radSec = 0; // stop rotation of the wheels
            w.inContact = false; // stop any forces for at least one physics frame
        }
        visuals.reset();

        Transform transform = getResetPosition(this.app.getStateManager(), this);
        rbc.setPhysicsLocation(transform.getTranslation());
        rbc.setPhysicsRotation(transform.getRotation());
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
    

    
    private static Transform getResetPosition(AppStateManager stateManager, RayCarControl car) {
        IDrive drive = stateManager.getState(IDrive.class);
        drive.resetWorld();
        return drive.resetPosition(car);
    }
}
