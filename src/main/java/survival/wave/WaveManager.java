package survival.wave;

import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;

import rallygame.car.ray.RayCarControl;
import rallygame.helper.Rand;
import survival.DodgeGameManager;
import survival.controls.BaseControl;
import survival.controls.Damager;
import survival.controls.Explode;

public class WaveManager extends BaseAppState {

    private final List<Geometry> geoms = new LinkedList<>();
    private final Node rootNode = new Node("Wave root");
    private final DodgeGameManager manager;
    private final RayCarControl player;

    private PhysicsSpace physicsSpace;
    private WaveCollisionListener colListener;

    public static final float KILL_DIST = 350;
    private float time;

    public WaveManager(DodgeGameManager manager, RayCarControl player) {
        this.manager = manager;
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

        this.colListener = new WaveCollisionListener(this, player);
        physicsSpace.addCollisionListener(colListener);

        time = manager.getGameRules().WaveSpeed;
    }

    @Override
    protected void cleanup(Application app) {
        rootNode.removeFromParent();

        physicsSpace.removeCollisionListener(colListener);
        colListener = null;
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
            time = manager.getGameRules().WaveSpeed;
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

    public void controlCollision(BaseControl control) {
        boolean removeControl = false;

        if (control.getBehaviour(Explode.class) != null) {
            var speedDiff = control.getLinearVelocity().subtract(player.vel);
            if (speedDiff.length() < Explode.MIN_FORCE) {
                speedDiff.normalizeLocal().multLocal(Explode.MIN_FORCE); // increase to minimum force
            }

            removeControl = true;

            var playerObj = player.getPhysicsObject();
            playerObj.applyImpulse(speedDiff.mult(playerObj.getMass()), new Vector3f());
        }

        var damager = control.getBehaviour(Damager.class);
        if (damager != null) {
            this.manager.updateState(x -> x.PlayerHealth -= damager.amount);
            removeControl = true;
        }

        if (removeControl) {
            control.getSpatial().removeFromParent();
            physicsSpace.remove(control.getSpatial());
        }
    }
}
