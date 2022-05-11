package rallygame.service.checkpoint;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Function;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

import rallygame.car.ray.RayCarControl;
import rallygame.effects.LoadModelWrapper;
import rallygame.service.GhostObjectCollisionListener;

// should only have one checkpoint active
public class BasicWaypointProgress extends BaseAppState {

    private final BasicCheckpointListener listener;
    private final Queue<Vector3f> checkpointBuffer = new LinkedList<>();
    
    private Checkpoint curCheckpoint;
    private int hitCount;
    private int totalHitCount;

    public BasicWaypointProgress(RayCarControl player) {
        this.listener = new BasicCheckpointListener(this, (x) -> player.getPhysicsObject() == x);
    }

    @Override
    protected void initialize(Application app) {
        this.listener.startListening(getState(BulletAppState.class).getPhysicsSpace());
    }

    @Override
    protected void cleanup(Application app) {
        this.listener.stopListening(getState(BulletAppState.class).getPhysicsSpace());
    }

    public void addCheckpointAt(Vector3f pos) {
        if (!isInitialized()) return;

        if (curCheckpoint != null) {
            checkpointBuffer.add(pos);
        } else {
            addCheckpoint(pos);
        }
    }

    private void addCheckpoint(Vector3f pos) {
        Vector3f checkpointSize = Vector3f.UNIT_XYZ.mult(10);
        Spatial baseSpat = new Geometry("checkpoint", new Box(checkpointSize.negate(), checkpointSize));
        baseSpat = LoadModelWrapper.createWithColour(getApplication().getAssetManager(), baseSpat, ColorRGBA.Blue);
        baseSpat.setLocalTranslation(pos);

        GhostControl ghost = new GhostControl(CollisionShapeFactory.createBoxShape(baseSpat));
        ghost.setCollisionGroup(PhysicsCollisionObject.COLLISION_GROUP_02);
        ghost.setCollideWithGroups(PhysicsCollisionObject.COLLISION_GROUP_02);
        baseSpat.addControl(ghost);

        curCheckpoint = new Checkpoint(totalHitCount, pos, ghost, baseSpat);

        ((SimpleApplication)getApplication()).getRootNode().attachChild(baseSpat);
        getState(BulletAppState.class).getPhysicsSpace().add(ghost);
    }

    public void playerHitCheckpoint() {
        hitCount++;
        totalHitCount++;

        ((SimpleApplication)getApplication()).getRootNode().detachChild(curCheckpoint.visualModel);
        getState(BulletAppState.class).getPhysicsSpace().remove(curCheckpoint.ghost);

        curCheckpoint = null;

        var newPos = checkpointBuffer.poll();
        if (newPos != null) {
            addCheckpoint(newPos);
        }
    }

    public boolean hitACheckpoint() {
        if (hitCount > 0) {
            hitCount--;
            return true;
        }

        return false;
    }
    public boolean noCheckpoint() {
        return curCheckpoint == null;
    }

    public int totalCheckpoints() {
        return totalHitCount;
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }
    
}


class BasicCheckpointListener implements GhostObjectCollisionListener.IListener {

    private final BasicWaypointProgress basic;
    private final Function<RigidBodyControl, Boolean> ifCar;
    private final GhostObjectCollisionListener checkpointCollisionListener;

    public BasicCheckpointListener(BasicWaypointProgress basic, Function<RigidBodyControl, Boolean> ifCar) {
        this.basic = basic;
        this.ifCar = ifCar;
        this.checkpointCollisionListener = new GhostObjectCollisionListener(this);
    }

    public void startListening(PhysicsSpace space) {
        space.addCollisionListener(checkpointCollisionListener);
    }
    public void stopListening(PhysicsSpace space) {
        space.removeCollisionListener(checkpointCollisionListener);
    }

    @Override
    public void ghostCollision(GhostControl control, RigidBodyControl obj) {
        if (ifCar.apply(obj)) {
            basic.playerHitCheckpoint();
        }
    }
}
