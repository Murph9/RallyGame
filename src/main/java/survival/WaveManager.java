package survival;

import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;

import rallygame.car.ray.RayCarControl;
import rallygame.helper.Geo;
import rallygame.helper.Log;
import survival.controls.BaseControl;

public class WaveManager extends BaseAppState {

    private final List<Geometry> geoms = new LinkedList<>();
    private final Node rootNode = new Node("Wave root");
    private final RayCarControl player;

    private PhysicsSpace physicsSpace;

    private static final float WAVE_TIMER = 3;
    private float time;

    public WaveManager(RayCarControl player) {
        this.player = player;
    }

    private void addType(WaveType type, RayCarControl target) {
        var pos = target.location.clone();
        pos.x = Math.round(pos.x/10f)*10f;
        pos.y = Math.round(pos.y/10f)*10f;
        pos.z = Math.round(pos.z/10f)*10f;

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
                geom.setLocalTranslation(pos);
                rootNode.attachChild(geom);
                physicsSpace.add(geom);
                Log.p(pos);

                this.geoms.add(geom);
            }
        }
    }

    @Override
    protected void initialize(Application app) {
        ((SimpleApplication)app).getRootNode().attachChild(rootNode);
        this.physicsSpace = getState(BulletAppState.class).getPhysicsSpace();

        time = WAVE_TIMER;
    }

    @Override
    protected void cleanup(Application app) {
        rootNode.removeFromParent();
    }

    @Override
    protected void onEnable() {
        for (var geom: geoms) {
            geom.getControl(BaseControl.class).setEnabled(true);
        }
    }

    @Override
    protected void onDisable() {
        for (var geom: geoms) {
            geom.getControl(BaseControl.class).setEnabled(false);
        }
    }

    @Override
    public void update(float tpf) {
        time -= tpf;
        if (time < 0) {
            time = WAVE_TIMER;
            addType(WaveType.SingleFollow, player);
        }
        
        super.update(tpf);
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
        var c = new BaseControl(1000, BaseControl.HoverAt(2), BaseControl.MaxSpeed(35), BaseControl.Target(target, 15));
        box.addControl(c);
        return new Geometry[] { box };
    }
}
