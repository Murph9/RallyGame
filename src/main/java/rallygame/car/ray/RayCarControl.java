package rallygame.car.ray;

import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppStateManager;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.input.InputManager;
import com.jme3.input.Joystick;
import com.jme3.input.RawInputListener;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import rallygame.car.JoystickEventListener;
import rallygame.car.MyKeyListener;
import rallygame.car.ai.ICarAI;
import rallygame.car.data.CarDataConst;
import rallygame.car.data.SurfaceType;
import rallygame.drive.IDrive;
import rallygame.helper.Geo;
import rallygame.helper.Log;
import rallygame.service.averager.AverageFloatFramerate;
import rallygame.service.averager.IAverager;
import rallygame.service.ray.PhysicsRaycaster;
import rallygame.service.ray.SceneRaycaster;

// data/input things
public class RayCarControl implements ICarPowered, ICarControlled {

    private final Application app;
    private final RayCarControlInput input;

    protected RayCarPowered rayCar;
    public RayCarPowered getRayCar() { return rayCar; }
    private RayCarVisuals visuals;

    // Steering averager (to get smooth left <-> right)
    private final AverageFloatFramerate steeringAverager;

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
    
    private final PhysicsSpace space;
    private boolean addedToSpace = false;
    
    //some fields to prevent needing to call GetPhysicsObject a lot
    public Vector3f vel, forward, up, left;
    public Vector3f angularVel;
    public Quaternion rotation;
    public Vector3f location;
    
    public RayCarControl(SimpleApplication app, Spatial initalCarModel, RayCarPowered rayCar) {
        this.app = app;
        this.space = app.getStateManager().getState(BulletAppState.class).getPhysicsSpace();
        this.input = new RayCarControlInput(this);

        vel = forward = up = left = location = angularVel = new Vector3f();
        rotation = new Quaternion();

        this.steeringAverager = new AverageFloatFramerate(0.2f, IAverager.Type.Weighted);

        this.controls = new LinkedList<RawInputListener>();
        
        init(initalCarModel, rayCar);
    }

    private void init(Spatial carModel, RayCarPowered rayCar) {
        this.rayCar = rayCar;
        this.visuals = new RayCarVisuals((SimpleApplication)app, carModel, this);
        space.addTickListener(rayCar);
    }

    public void changeRayCar(Spatial carModel, RayCarPowered rayCar) {
        space.removeTickListener(this.rayCar);
        space.remove(this.visuals.getRootNode());
        visuals.cleanup(app);
        addedToSpace = false;

        init(carModel, rayCar);

        // set physics state correctly
        setPhysicsProperties(this.location, this.vel, this.rotation, this.angularVel);
        // update inputs, to survive the copy
        rayCar.updateControlInputs(steeringCurrent, brakeCurrent, handbrakeCurrent);
    }
    
    public void update(float tpf) {
        if (!rayCar.rbEnabled())
            return; //don't update if the rigid body isn't

        if (ai != null) {
            ai.update(tpf);
        }
        // set many view fields
        rotation = rayCar.rbc.getPhysicsRotation();
        vel = rayCar.rbc.getLinearVelocity();
        forward = rotation.mult(Vector3f.UNIT_Z);
        up = rotation.mult(Vector3f.UNIT_Y);
        left = rotation.mult(Vector3f.UNIT_X);
        angularVel = rayCar.rbc.getAngularVelocity();
        location = rayCar.rbc.getPhysicsLocation();
        
        // wheel turning logic
        steeringCurrent = 0;
        if (steerLeft != 0) //left
            steeringCurrent += getBestTurnAngle(steerLeft, 1);
        if (steerRight != 0) //right
            steeringCurrent -= getBestTurnAngle(steerRight, -1);
        steeringCurrent = ignoreSpeedFactor ? steeringCurrent : steeringAverager.get(steeringCurrent, tpf);
        steeringCurrent = FastMath.clamp(steeringCurrent, -rayCar.carData.w_steerAngle, rayCar.carData.w_steerAngle);

        rayCar.updateControlInputs(steeringCurrent, brakeCurrent, handbrakeCurrent);

        updateGroundType();

        visuals.viewUpdate(tpf);
    }

    private void updateGroundType() {
        // update the ground type for the wheels (shouldn't this be a physics concern?)
        var caster = app.getStateManager().getState(SceneRaycaster.class);
        for (int i = 0; i < 4; i++) {
            var wheel = getWheel(i);
            var pos = wheel.rayStartWorld;
            var dir = wheel.rayDirWorld;
            if (pos == null || dir == null) {
                continue;
            }
            
            var results = caster.castRay(pos, dir);
            
            for (var result: results) {
                if (result.getDistance() > dir.length())
                    continue;

                if (Geo.getGeomList(this.getRootNode()).contains(result.getGeometry())) { //perf?
                    // ignore anything on the car
                    continue;
                }

                wheel.lastGroundType = SurfaceType.fromMaterialName(result.getGeometry().getMaterial());
                break; //only use the closest one
            }
        }
    }

    public void giveSound(AudioNode audio) {
        visuals.giveSound(audio);
    }
    
    private float getBestTurnAngle(float trySteerAngle, float sign) {
        if (ignoreSpeedFactor)
            return trySteerAngle;
        
        Vector3f local_vel = rotation.inverse().mult(vel);
        if (local_vel.z < 0 || ((-sign * rayCar.driftAngle) < 0 && Math.abs(rayCar.driftAngle) > rayCar.carData.minDriftAngle * FastMath.DEG_TO_RAD)) {
            // TODO turn to this real slow, no snapping pls
            return trySteerAngle;
            //when going backwards, slow or needing to turning against drift, you get no speed factor
            //eg: car is pointing more left than velocity, and is also turning left
            //and drift angle needs to be large enough to matter
        }

        if (local_vel.length() < 8) //prevent not being able to turn at slow speeds
            return trySteerAngle;

        // this is magic, but: minimum should be bast pjk lat, but it doesn't catch up to the turning angle required
        // so we just add some of the angular vel value to it
        return rayCar.wheels[0].data.traction.pjk_lat.max + angularVel.length()*0.125f; //constant needs minor adjustments
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
        
        space.removeTickListener(rayCar);
        space.remove(this.visuals.getRootNode());
    }
    

    public Node getRootNode() {
        return visuals.getRootNode();
    }
    public CarDataConst getCarData() {
        return rayCar.carData;
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
        Joystick[] joysticks = im.getJoysticks();
        if (joysticks != null)
            this.controls.add(new JoystickEventListener(this.getInput()));
        
        for (RawInputListener ril: this.controls)
            im.addRawInputListener(ril);
    }
    
    public boolean noWheelsInContact() {
        return rayCar.noWheelsInContact();
    }
    public RayWheel getWheel(int w_id) {
        return rayCar.wheels[w_id];
    }
    public List<RayWheel> getDriveWheels() {
        List<RayWheel> results = new LinkedList<>();

        if (rayCar.carData.driveFront) {
            results.add(getWheel(0));
            results.add(getWheel(1));
        }
        if (rayCar.carData.driveRear) {
            results.add(getWheel(2));
            results.add(getWheel(3));
        }

        return results;
    }
    
    /**
     * Get base physics object. To easily get properties try: vel, forward, up, left,
     * location, angularVel and rotation */
    public PhysicsRigidBody getPhysicsObject() {
        return rayCar.rbc;
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
            rayCar.rbc.setPhysicsLocation(position);
        if (velocity != null)
            rayCar.rbc.setLinearVelocity(velocity);
        if (rotation != null)
            rayCar.rbc.setPhysicsRotation(rotation);
        if (angularVel != null)
            rayCar.rbc.setAngularVelocity(angularVel);
    }
    
    public float getCurrentVehicleSpeedKmHour() {
        if (vel == null)
            return 0;
        return vel.length() * 3.6f;
    }
    
    //enabled functions
    public void setEnabled(boolean enabled) {
        if (this.space != null) {
            rayCar.rbc.setEnabled(enabled);
            visuals.enableSound(enabled);
            
            if (enabled && !addedToSpace) {
                space.add(this.visuals.getRootNode());
                addedToSpace = true;
            } else if (!enabled && addedToSpace) {
                space.remove(this.visuals.getRootNode());
                addedToSpace = false;
            }
        }
    }
    
    // #region ICarControlled
    public ICarControlled getControlledState() { return (ICarControlled)this; }
    public void setSteerLeft(float value) { steerLeft = value; }
    public void setSteerRight(float value) { steerRight = value; }
    public void setAccel(float value) { rayCar.accelCurrent = value; }
    public void setBraking(float value) { brakeCurrent = value; }
    public void setHandbrake(boolean value) { handbrakeCurrent = value; }
    public void setNitro(boolean value) { rayCar.ifNitro = value; }
    public void jump() { 
        rayCar.rbc.applyImpulse(rayCar.carData.JUMP_FORCE, new Vector3f()); // push up
        Vector3f old = location.clone();
        old.y += 2; // and move up
        Vector3f vel = this.vel.clone();
        vel.y = 0; // reset vertical speed
        this.setPhysicsProperties(old, vel, null, null);
    }
    public void flip() {
        float angleF = this.forward.angleBetween(Vector3f.UNIT_Z); // absolute angle between
        float angOther = this.left.angleBetween(Vector3f.UNIT_Z); // calc if its pos or neg

        // new angle in the correct direction
        float nowTurn = -angleF * Math.signum(FastMath.HALF_PI - angOther);

        this.setPhysicsProperties(location.add(new Vector3f(0, 1, 0)), null, new Quaternion().fromAngleAxis(nowTurn, Vector3f.UNIT_Y), new Vector3f());
    }
    public void reset() {
        rayCar.reset();
        visuals.reset();

        Transform transform = getResetPosition(this.app.getStateManager(), this);
        this.setPhysicsProperties(transform.getTranslation(), new Vector3f(), transform.getRotation(), new Vector3f());
    }
    public void reverse(boolean value) {
        if (value)
            rayCar.curGear = RayCarPowered.REVERSE_GEAR_INDEX;
        else
            rayCar.curGear = 1;
    }
    public void ignoreSpeedFactor(boolean value) { ignoreSpeedFactor = value; }
    public void ignoreTractionModel(boolean value) { 
        if (value)
            rayCar.tractionEnabled = !rayCar.tractionEnabled;
    }
    // #endregion

    // #region ICarPowered
    public ICarPowered getPoweredState() { return (ICarPowered)this; }
    public float accelCurrent() { return rayCar.accelCurrent; }
    public float brakeCurrent() { return brakeCurrent; }
    public int curGear() { return rayCar.curGear; }
    public float nitro() { return rayCar.getNitroRemaining(); }
    public float fuel() { return rayCar.fuelRemaining(); }
    public void setFuel(float value) { rayCar.setFuelRemaining(value); }
    public float steeringCurrent() { return this.steeringCurrent; }
    public int curRPM() { return rayCar.curRPM; }
    public boolean ifHandbrake() { return this.handbrakeCurrent; }
    public float driftAngle() { return rayCar.driftAngle; }
    public Vector3f getPlanarGForce() { return rayCar.planarGForce; }
    public float getWheelTorque(int w_id) { return rayCar.getWheelTorque(w_id); }
    // #endregion
    
    
    private static Transform getResetPosition(AppStateManager stateManager, RayCarControl car) {
        IDrive drive = stateManager.getState(IDrive.class);
        drive.resetWorld();
        return drive.resetPosition(car);
    }

    public final String statsString() {
        RigidBodyControl rbc = this.rayCar.rbc;
        return rallygame.helper.H.round3f(rbc.getPhysicsLocation(), 2) + "\nspeed:"
                + rallygame.helper.H.round3f(rbc.getLinearVelocity(), 2) + "m/s\nRPM:" + rayCar.curRPM + "\nengine:"
                + rayCar.engineTorque + "\ndrag:" + rallygame.helper.H.roundDecimal(
                        rayCar.dragDir.length(), 3) + " rr("
                + rallygame.helper.H.roundDecimal(rollingResistanceTotal(), 3) + ")N\ndistanceTravelled:"
                + rallygame.helper.H.roundDecimal(rayCar.travelledDistance, 1) + "m";
    }

    private float rollingResistanceTotal() {
        float total = 0;
        for (RayWheel w: rayCar.wheels) {
            total += w.rollingResistance;
        }
        return total;
    }
}
