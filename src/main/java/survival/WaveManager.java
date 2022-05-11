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
        var box = Geo.makeShapeBox(app.getAssetManager(), ColorRGBA.Black, Vector3f.ZERO.add(0, 2, 0), 1);
        var control = new FollowControl(target);
        box.addControl(control);

        return new Geometry[] { box };
    }
}


class FollowControl extends RigidBodyControl {

    private static final float MASS = 1000;
    private final RayCarControl target;

    public FollowControl(RayCarControl target) {
        super(MASS);
        this.target = target;
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        Vector3f targetDir = target.location.subtract(this.spatial.getLocalTranslation());
        this.applyImpulse(targetDir.normalize().mult(MASS*10*tpf), Vector3f.ZERO);
    }
}
