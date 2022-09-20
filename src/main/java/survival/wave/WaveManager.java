package survival.wave;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

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
import survival.StateManager;
import survival.controls.BaseControl;
import survival.controls.Damager;
import survival.controls.Explode;

public class WaveManager extends BaseAppState {

    private final Set<Geometry> geoms = new HashSet<>();
    private final Node rootNode = new Node("Wave root");
    private final RayCarControl player;

    private StateManager stateManager;
    private PhysicsSpace physicsSpace;
    private WaveCollisionListener colListener;

    public static final float KILL_DIST = 250;
    private float time;

    private float frozenTimer;

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

        this.colListener = new WaveCollisionListener(this, player);
        physicsSpace.addCollisionListener(colListener);

        stateManager = getState(StateManager.class);
        time = stateManager.getState().WaveSpeed;
    }

    @Override
    protected void cleanup(Application app) {
        rootNode.removeFromParent();
        rootNode.detachAllChildren();

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
        frozenTimer -= tpf;
        if (frozenTimer > 0) {
            for (var geom: this.geoms) {
                geom.getControl(BaseControl.class).setEnabled(false);
            }
        } else {
            for (var geom: this.geoms) {
                geom.getControl(BaseControl.class).setEnabled(true);
            }
        }

        time -= tpf;
        if (time < 0) {
            var state = stateManager.getState();
            time = state.WaveSpeed;
            var type = Rand.randFromArray(WaveType.values());
            if (state.EntityCount > this.geoms.size())
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

            var pos = control.getPhysicsLocation();
            applyForceFrom(pos, 50, 35);
        }

        var damager = control.getBehaviour(Damager.class);
        if (damager != null) {
            stateManager.getState().PlayerHealth -= damager.amount;
            removeControl = true;
        }

        if (removeControl) {
            control.getSpatial().removeFromParent();
            physicsSpace.remove(control.getSpatial());
        }
    }

    public void applyForceFrom(Vector3f pos, float strength, float distance) {
        // force all other boxes away
        for (var geom: this.geoms) {
            var baseControl = geom.getControl(BaseControl.class);
            var dir = baseControl.getPhysicsLocation().subtract(pos);
            if (dir.length() < distance) {
                baseControl.applyImpulse(dir.normalize().mult(baseControl.getMass() * strength), Vector3f.ZERO);
            }
        }
    }

    public void freezeAll(float time) {
        frozenTimer = time;
    }
}
