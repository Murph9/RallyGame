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
        final float minForce = 4;

        var speedDiff = control.getLinearVelocity().subtract(player.vel);
        if (speedDiff.length() < minForce) {
            speedDiff.normalizeLocal().multLocal(minForce); // increase to minimum force
        }
        if (control.hasBehaviour(BaseControl.Explode())) { // this is so gross
            control.getSpatial().removeFromParent();
            physicsSpace.remove(control.getSpatial());

            var playerObj = player.getPhysicsObject();
            playerObj.applyImpulse(speedDiff.mult(playerObj.getMass()), new Vector3f());
        }
    }
}
