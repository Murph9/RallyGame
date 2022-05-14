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
import rallygame.helper.Rand;
import survival.controls.BaseControl;

public class WaveManager extends BaseAppState {

    private final List<Geometry> geoms = new LinkedList<>();
    private final Node rootNode = new Node("Wave root");
    private final RayCarControl player;

    private PhysicsSpace physicsSpace;

    public static final float KILL_DIST = 350;
    private static final float WAVE_TIMER = 3;
    private float time;

    public WaveManager(RayCarControl player) {
        this.player = player;
    }

    private void addType(WaveType type, RayCarControl target) {
        final float BOX_DIST = 20;
        var pos = target.location.clone();
        pos.x = Math.round(pos.x/BOX_DIST)*BOX_DIST;
        pos.y = Math.round(pos.y/BOX_DIST)*BOX_DIST;
        pos.z = Math.round(pos.z/BOX_DIST)*BOX_DIST;

        Geometry[] geoms = type.getGenerator().generate(getApplication(), pos, target);
        
        if (geoms != null) {
            for (var geom : geoms) {
                physicsSpace.add(geom);
                rootNode.attachChild(geom);

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
            var type = Rand.randFromArray(WaveType.values());
            addType(type, player);
        }

        // kill all boxes far away from player
        var pos = player.location;
        for (var geom: new LinkedList<>(this.geoms)) {
            if (geom.getLocalTranslation().distance(pos) > KILL_DIST) {
                physicsSpace.remove(geom);
                rootNode.detachChild(geom);

                this.geoms.remove(geom);
            }
        }
        
        super.update(tpf);
    }
}


enum WaveType {
    SingleFollow(WaveGenerator::generateSingleProjectile),
    ManyLines(WaveGenerator::generateLines),
    ;

    public final IWaveGenerator func;
    WaveType(IWaveGenerator func) {
        this.func = func;
    }

    public IWaveGenerator getGenerator() {
        return func;
    }
}

interface IWaveGenerator {
    Geometry[] generate(Application app, Vector3f aClosePos, RayCarControl target);
}

class WaveGenerator {
    public static Geometry[] generateSingleProjectile(Application app, Vector3f aClosePos, RayCarControl target) {
        var box = Geo.makeShapeBox(app.getAssetManager(), ColorRGBA.DarkGray, Vector3f.ZERO.add(0, 2, 0), 1);
        box.getMaterial().getAdditionalRenderState().setWireframe(false);
        var c = new BaseControl(1000, BaseControl.HoverAt(2), BaseControl.MaxSpeed(35), BaseControl.Target(target, 15));
        box.addControl(c);
        c.setPhysicsLocation(aClosePos);

        return new Geometry[] { box };
    }
    
    public static Geometry[] generateLines(Application app, Vector3f aClosePos, RayCarControl target) {
        final int count = 10;
        var geoms = new Geometry[count];
        for (int i = 0; i < count; i++) {
            var box = Geo.makeShapeBox(app.getAssetManager(), ColorRGBA.White, Vector3f.ZERO.add(0, 2, 0), 1.3f);
            box.getMaterial().getAdditionalRenderState().setWireframe(false);
            var c = new BaseControl(500, BaseControl.HoverAt(2), BaseControl.MaxSpeed(25), BaseControl.Move(Vector3f.UNIT_Z, 40));
            box.addControl(c);

            var pos = aClosePos.add((i-(count/2))*10, 0, -WaveManager.KILL_DIST/2);
            c.setPhysicsLocation(pos);
            geoms[i] = box;
        }

        return geoms;
    }
}
