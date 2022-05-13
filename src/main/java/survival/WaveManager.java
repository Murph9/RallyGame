package survival;

import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;

import rallygame.car.ray.RayCarControl;
import rallygame.helper.Geo;

public class WaveManager extends BaseAppState {

    private final List<Geometry> geoms = new LinkedList<>();
    private final Node rootNode = new Node("Wave root");
    private PhysicsSpace physicsSpace;

    public void addType(WaveType type, RayCarControl target) {
        
        Geometry[] geoms = null;
        switch (type) {
            case ManyLines:
                break;
            case SingleFollow:
                geoms = WaveGenerator.generateSingleProjectile(getApplication(), target);
            default:
                break;
        }
        
        if (geoms != null) {
            for (var geom : geoms) {
                rootNode.attachChild(geom);
                physicsSpace.add(geom);

                this.geoms.add(geom);
            }
        }
    }

    @Override
    protected void initialize(Application app) {
        ((SimpleApplication)app).getRootNode().attachChild(rootNode);
        this.physicsSpace = getState(BulletAppState.class).getPhysicsSpace();
    }

    @Override
    protected void cleanup(Application app) {
        rootNode.removeFromParent();
    }

    @Override
    protected void onEnable() {
        for (var geom: geoms) {
            geom.getControl(FollowControl.class).setEnabled(true);
        }
    }

    @Override
    protected void onDisable() {
        for (var geom: geoms) {
            geom.getControl(FollowControl.class).setEnabled(false);
        }
    }
    
}


enum WaveType {
    SingleFollow,
    ManyLines,
    ;
}

class WaveGenerator {
    public static Geometry[] generateSingleProjectile(Application app, RayCarControl target) {
        var box = Geo.makeShapeBox(app.getAssetManager(), ColorRGBA.DarkGray, Vector3f.ZERO.add(0, 2, 0), 1);
        box.getMaterial().getAdditionalRenderState().setWireframe(false);
        var control = new FollowControl(target, 2, 30);
        box.addControl(control);

        return new Geometry[] { box };
    }
}


class FollowControl extends RigidBodyControl {

    private static final float MASS = 1000;
    private final RayCarControl target;
    private final float height;
    private final float strength;
    private final float maxSpeed;

    public FollowControl(RayCarControl target, float height) {
        this(target, height, 10, 30);
    }

    public FollowControl(RayCarControl target, float height, float strength) {
        this(target, height, strength, 30);
    }

    public FollowControl(RayCarControl target, float height, float strength, float maxSpeed) {
        super(MASS);
        this.target = target;
        this.height = height;
        this.strength = strength;
        this.maxSpeed = maxSpeed;
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        this.setGravity(Vector3f.ZERO);

        Vector3f targetDir = target.location.subtract(this.spatial.getLocalTranslation());
        this.applyImpulse(targetDir.normalize().mult(MASS*this.strength*tpf), Vector3f.ZERO);

        var loc = this.getPhysicsLocation().clone();
        loc.y = this.height;
        this.setPhysicsLocation(loc);

        
        var vel = this.getLinearVelocity();
        vel.y = 0; // prevent weird gravity mess
        if (vel.length() > this.maxSpeed) {
            vel.normalizeLocal().multLocal(this.maxSpeed);
        }
        this.setLinearVelocity(vel);
    }
}
