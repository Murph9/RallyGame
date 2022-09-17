package survival.wave;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.Lists;
import com.jme3.app.Application;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;

import rallygame.car.ray.RayCarControl;
import rallygame.helper.Geo;
import rallygame.helper.H;
import rallygame.helper.Rand;
import survival.controls.BaseControl;
import survival.controls.Damager;
import survival.controls.Explode;
import survival.controls.HoverAt;
import survival.controls.MaxSpeed;
import survival.controls.MoveDir;
import survival.controls.Target;

class WaveBuilder {
    private static Geometry generateSingleDir(Application app, ColorRGBA colour, Vector3f pos, Vector3f dir, float force, float maxSpeed) {
        var box = Geo.makeShapeBox(app.getAssetManager(), colour, pos, 1.3f);
        box.getMaterial().getAdditionalRenderState().setWireframe(false);
        var c = new BaseControl(500, new HoverAt(2), new MaxSpeed(maxSpeed), new MoveDir(dir, force));
        box.addControl(c);

        return box;
    }

    public static List<Geometry> generateRandomMovers(Application app, RayCarControl target) {
        
        var geoms = new Geometry[10];
        for (int i = 0; i < geoms.length; i++) {
            var dir = H.v2tov3fXZ(Rand.randV2f(WaveManager.KILL_DIST*0.75f, true));
            var pos = target.location.add(dir);
            geoms[i] = generateSingleDir(app, ColorRGBA.Blue, pos, dir.negate(), 20, 20);
        }

        return Lists.newArrayList(geoms);
    }

    public static List<Geometry> generateSingleFollow(Application app, RayCarControl target) {
        var pos = target.location.add(target.forward.negate().mult(10));
        var colour = new ColorRGBA(0.2f, 0.2f, 0.2f, 0.8f);
        var box = Geo.makeShapeBox(app.getAssetManager(), colour, pos, 1);
        box.getMaterial().getAdditionalRenderState().setWireframe(false);
        var c = new BaseControl(800, new HoverAt(2), new MaxSpeed(35), new Target(target, 15));
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
    
    private static List<Geometry> generateLine(Application app, Vector3f target, Vector3f dir, int count, boolean negate) {
        if (negate)
            dir = dir.negate();

        var offset = dir.negate().mult(WaveManager.KILL_DIST * 0.5f);
        var between = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y).mult(dir);
        var geoms = new Geometry[count];
        for (int i = 0; i < count; i++) {
            var pos = target.add(between.mult((i-(count/2))*20)).add(offset);
            var box = generateSingleDir(app, ColorRGBA.White, pos, dir, 40, 25);
            geoms[i] = box;
        }

        return Lists.newArrayList(geoms);
    }

    public static List<Geometry> generateFast(Application app, RayCarControl target) {
        final var count = 5;

        var dir = Rand.randV3f(1, true);
        var geoms = new Geometry[count];
        for (int i = 0; i < count; i++) {
            var pos = target.location.add(dir.negate().mult(WaveManager.KILL_DIST * 0.5f));
            var box = Geo.makeShapeBox(app.getAssetManager(), ColorRGBA.Pink, pos, 0.5f);
            box.getMaterial().getAdditionalRenderState().setWireframe(false);
            var c = new BaseControl(3000, new HoverAt(0.5f), new MaxSpeed(70), new MoveDir(dir, 100), new Damager());
            box.addControl(c);

            geoms[i] = box;
        }

        return Lists.newArrayList(geoms);
    }
    
    public static List<Geometry> generateExplode(Application app, RayCarControl target) {
        var pos = target.location.add(target.forward.negate().mult(30));
        var box = Geo.makeShapeBox(app.getAssetManager(), ColorRGBA.Red, pos, 1);
        box.getMaterial().getAdditionalRenderState().setWireframe(false);
        var c = new BaseControl(1500,
            new HoverAt(2),
            new MaxSpeed(40),
            new Target(target, 25), //TODO maybe prevent these from moving?
            new Explode(),
            new Damager(2));
        box.addControl(c);

        return Arrays.asList(box);
    }
}