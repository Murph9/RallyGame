package game;

import java.util.Iterator;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.objects.VehicleWheel;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.RenderManager;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;

//Copied from:
//https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-bullet/src/common/java/com/jme3/bullet/control/VehicleControl.java


//just so i can type this |  (yes, i really mean just the extended class)
//                        v
public class MyVC extends MyPhysicsVehicle implements PhysicsControl {

	//There should be no need to touch this file (unless the pass through parameters change)
	protected Spatial spatial;
    protected boolean enabled = true;
    protected PhysicsSpace space = null;
    protected boolean added = false;
	
    public MyVC(CollisionShape compoundShape, FancyVT car, Node carNode, Rally rally) {
    	super(compoundShape, car, carNode, rally);
	}

	public boolean isApplyPhysicsLocal() {
        return motionState.isApplyPhysicsLocal();
    }

    /**When set to true, the physics coordinates will be applied to the local
     * translation of the Spatial
     * @param applyPhysicsLocal
     */
    public void setApplyPhysicsLocal(boolean applyPhysicsLocal) {
        motionState.setApplyPhysicsLocal(applyPhysicsLocal);
        for (Iterator<VehicleWheel> it = wheels.iterator(); it.hasNext();) {
            VehicleWheel vehicleWheel = it.next();
            vehicleWheel.setApplyLocal(applyPhysicsLocal);
        }
    }

    private Vector3f getSpatialTranslation(){
        if(motionState.isApplyPhysicsLocal()){
            return spatial.getLocalTranslation();
        }
        return spatial.getWorldTranslation();
    }

    private Quaternion getSpatialRotation(){
        if(motionState.isApplyPhysicsLocal()){
            return spatial.getLocalRotation();
        }
        return spatial.getWorldRotation();
    }

    public Control cloneForSpatial(Spatial spatial) {
        MyVC control = new MyVC(collisionShape, car, carNode, rally);
        control.setAngularFactor(getAngularFactor());
        control.setAngularSleepingThreshold(getAngularSleepingThreshold());
        control.setAngularVelocity(getAngularVelocity());
        control.setCcdMotionThreshold(getCcdMotionThreshold());
        control.setCcdSweptSphereRadius(getCcdSweptSphereRadius());
        control.setCollideWithGroups(getCollideWithGroups());
        control.setCollisionGroup(getCollisionGroup());
        control.setDamping(getLinearDamping(), getAngularDamping());
        control.setFriction(getFriction());
        control.setGravity(getGravity());
        control.setKinematic(isKinematic());
        control.setLinearSleepingThreshold(getLinearSleepingThreshold());
        control.setLinearVelocity(getLinearVelocity());
        control.setPhysicsLocation(getPhysicsLocation());
        control.setPhysicsRotation(getPhysicsRotationMatrix());
        control.setRestitution(getRestitution());

        control.setFrictionSlip(getFrictionSlip());
        control.setMaxSuspensionTravelCm(getMaxSuspensionTravelCm());
        control.setSuspensionStiffness(getSuspensionStiffness());
        control.setSuspensionCompression(tuning.suspensionCompression);
        control.setSuspensionDamping(tuning.suspensionDamping);
        control.setMaxSuspensionForce(getMaxSuspensionForce());

        for (Iterator<VehicleWheel> it = wheels.iterator(); it.hasNext();) {
            VehicleWheel wheel = it.next();
            VehicleWheel newWheel = control.addWheel(wheel.getLocation(), wheel.getDirection(), wheel.getAxle(), wheel.getRestLength(), wheel.getRadius(), wheel.isFrontWheel());
            newWheel.setFrictionSlip(wheel.getFrictionSlip());
            newWheel.setMaxSuspensionTravelCm(wheel.getMaxSuspensionTravelCm());
            newWheel.setSuspensionStiffness(wheel.getSuspensionStiffness());
            newWheel.setWheelsDampingCompression(wheel.getWheelsDampingCompression());
            newWheel.setWheelsDampingRelaxation(wheel.getWheelsDampingRelaxation());
            newWheel.setMaxSuspensionForce(wheel.getMaxSuspensionForce());

            //TODO: bad way finding children!
            if (spatial instanceof Node) {
                Node node = (Node) spatial;
                Spatial wheelSpat = node.getChild(wheel.getWheelSpatial().getName());
                if (wheelSpat != null) {
                    newWheel.setWheelSpatial(wheelSpat);
                }
            }
        }
        control.setApplyPhysicsLocal(isApplyPhysicsLocal());
        return control;
    }

    public void setSpatial(Spatial spatial) {
        this.spatial = spatial;
        setUserObject(spatial);
        if (spatial == null) {
            return;
        }
        setPhysicsLocation(getSpatialTranslation());
        setPhysicsRotation(getSpatialRotation());
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (space != null) {
            if (enabled && !added) {
                if(spatial!=null){
                    setPhysicsLocation(getSpatialTranslation());
                    setPhysicsRotation(getSpatialRotation());
                }
                space.addCollisionObject(this);
                added = true;
            } else if (!enabled && added) {
                space.removeCollisionObject(this);
                added = false;
            }
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void update(float tpf) {
        if (enabled && spatial != null) {
            if (getMotionState().applyTransform(spatial)) {
                spatial.getWorldTransform();
                applyWheelTransforms();
            }
        } else if (enabled) {
            applyWheelTransforms();
        }
    }

    public void render(RenderManager rm, ViewPort vp) {
    }

    public void setPhysicsSpace(PhysicsSpace space) {
        createVehicle(space);
        if (space == null) {
            if (this.space != null) {
                this.space.removeCollisionObject(this);
                added = false;
            }
        } else {
            if(this.space==space) return;
            space.addCollisionObject(this);
            added = true;
        }
        this.space = space;
    }

    public PhysicsSpace getPhysicsSpace() {
        return space;
    }
}
