package survival;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.Lists;
import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
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
        var geoms = type.getGenerator().generate(getApplication(), target);
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

        // kill all far away from player
        var pos = player.location;
        for (var geom: new LinkedList<>(this.geoms)) {
            var geomPos = geom.getControl(BaseControl.class).getPhysicsLocation();
            if (geomPos.distance(pos) > KILL_DIST) {
                physicsSpace.remove(geom);
                rootNode.detachChild(geom);

                this.geoms.remove(geom);
            }
        }
        
        super.update(tpf);
    }
}


enum WaveType {
    SingleFollow(WaveGenerator::generateSingleFollow),
    Line(WaveGenerator::generateLine),
    Fast(WaveGenerator::generateFast)
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
    List<Geometry> generate(Application app, RayCarControl target);
}

class WaveGenerator {
    public static List<Geometry> generateSingleFollow(Application app, RayCarControl target) {
        var box = Geo.makeShapeBox(app.getAssetManager(), ColorRGBA.DarkGray, target.location, 1);
        box.getMaterial().getAdditionalRenderState().setWireframe(false);
        var c = new BaseControl(1000, BaseControl.HoverAt(2), BaseControl.MaxSpeed(35), BaseControl.Target(target, 15));
        box.addControl(c);

        return Arrays.asList(box);
    }

    public static List<Geometry> generateLine(Application app, RayCarControl target) {
        int option = FastMath.rand.nextInt(3);
        boolean optionBool = FastMath.rand.nextBoolean();
        switch(option) {
            case 0:
                return generateLine(app, target.location, Vector3f.UNIT_X, 10, optionBool);
            case 1:
                return generateLine(app, target.location, Vector3f.UNIT_Z, 10, optionBool);
            case 2:
                var list = generateLine(app, target.location, Vector3f.UNIT_X, 10, optionBool);
                list.addAll(generateLine(app, target.location, Vector3f.UNIT_Z, 10, optionBool));
                return list;
            default:
                return new LinkedList<>();
        }
    }
    
    private static List<Geometry> generateLine(Application app, Vector3f aClosePos, Vector3f dir, int count, boolean negate) {
        if (negate)
            dir = dir.negate();

        var offset = dir.negate().mult(WaveManager.KILL_DIST * 0.5f);
        var between = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y).mult(dir);
        var geoms = new Geometry[count];
        for (int i = 0; i < count; i++) {
            var pos = aClosePos.add(between.mult((i-(count/2))*20)).add(offset);
            var box = Geo.makeShapeBox(app.getAssetManager(), ColorRGBA.White, pos, 1.3f);
            box.getMaterial().getAdditionalRenderState().setWireframe(false);
            var c = new BaseControl(500, BaseControl.HoverAt(2), BaseControl.MaxSpeed(25), BaseControl.Move(dir, 40));
            box.addControl(c);
            geoms[i] = box;
        }

        return Lists.newArrayList(geoms);
    }

    public static List<Geometry> generateFast(Application app, RayCarControl target) {
        var dir = Rand.randV3f(1, true);

        var pos = target.location.add(dir.negate().mult(WaveManager.KILL_DIST * 0.5f));
        var box = Geo.makeShapeBox(app.getAssetManager(), ColorRGBA.Pink, pos, 0.5f);
        box.getMaterial().getAdditionalRenderState().setWireframe(false);
        var c = new BaseControl(3000, BaseControl.HoverAt(1), BaseControl.MaxSpeed(70), BaseControl.Move(dir, 100));
        box.addControl(c);

        return Arrays.asList(box);
    }
}
