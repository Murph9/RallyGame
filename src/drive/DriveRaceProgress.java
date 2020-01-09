package drive;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

import car.ray.RayCarControl;
import effects.LoadModelWrapper;
import helper.H;

public class DriveRaceProgress {

    private static final ColorRGBA CHECKPOINT_COLOUR = new ColorRGBA(0, 1, 0, 0.2f);
    private static final Vector3f CHECKPOINT_SIZE = new Vector3f(1.5f, 1.5f, 1.5f);

    private final AssetManager am;
    private final Checkpoint[] checkpoints;
    private final Node rootNode;
    private final HashMap<RayCarControl, RacerState> racers;
    private final boolean ifDebug;

    private final GhostObjectCollisionListener checkpointCollisionListener;

    // debug things
    private Node debugNode;

    protected DriveRaceProgress(Application app, Vector3f[] checkpointPositions, Collection<RayCarControl> cars, boolean ifDebug) {
        this.checkpoints = new Checkpoint[checkpointPositions.length];
        this.am = app.getAssetManager();
        this.rootNode = new Node("progress root node");
        ((SimpleApplication) app).getRootNode().attachChild(rootNode);
        this.ifDebug = ifDebug;

        PhysicsSpace physicsSpace = app.getStateManager().getState(BulletAppState.class).getPhysicsSpace();

        for (int i = 0; i < checkpointPositions.length; i++) {
            Spatial box = new Geometry("checkpoint box " + i, new Box(CHECKPOINT_SIZE.negate(), CHECKPOINT_SIZE));
            box = LoadModelWrapper.create(app.getAssetManager(), box, CHECKPOINT_COLOUR);

            GhostControl ghost = new GhostControl(CollisionShapeFactory.createBoxShape(box));
            box.setLocalTranslation(checkpointPositions[i]);
            box.addControl(ghost);
            rootNode.attachChild(box);
            physicsSpace.add(ghost);

            this.checkpoints[i] = new Checkpoint(i, checkpointPositions[i], ghost);
        }

        this.racers = new HashMap<>();
        for (RayCarControl car : cars) {
            this.racers.put(car, new RacerState(this.checkpoints[0]));
        }

        checkpointCollisionListener = new GhostObjectCollisionListener(this);
        physicsSpace.addCollisionListener((PhysicsCollisionListener) checkpointCollisionListener);
    }

    protected void update(float tpf) {
        if (checkpoints == null || checkpoints.length < 1)
            return;

        if (ifDebug) {
            // update the checkpoint arrows
            if (debugNode != null)
                rootNode.detachChild(debugNode);
            debugNode = new Node("debugnode");
            for (Entry<RayCarControl, RacerState> entry : racers.entrySet()) {
                Vector3f pos = entry.getKey().getPhysicsLocation().add(0, 2, 0);
                Vector3f dir = entry.getValue().nextCheckpoint.position.subtract(pos);
                entry.getValue().arrow = H.makeShapeArrow(am, ColorRGBA.Cyan, dir, pos);
                debugNode.attachChild(entry.getValue().arrow);
            }
            rootNode.attachChild(debugNode);
        }
    }

    public void cleanup(Application app) {
        ((SimpleApplication) app).getRootNode().detachChild(rootNode);

        PhysicsSpace physicsSpace = app.getStateManager().getState(BulletAppState.class).getPhysicsSpace();
        for (Checkpoint checkpoint : checkpoints) {
            physicsSpace.remove(checkpoint.ghost);
        }
        physicsSpace.removeCollisionListener((PhysicsCollisionListener) checkpointCollisionListener);
    }

    public Vector3f getNextCheckpoint(RayCarControl car, Vector3f pos) {
        return racers.get(car).nextCheckpoint.position;
    }

    public Vector3f getCurrentCheckpoint(RayCarControl car) {
        int index = this.racers.get(car).nextCheckpoint.num - 1;
        if (index < 0)
            index = this.checkpoints.length - 1;
        return this.checkpoints[index].position;
    }

    public String getCheckpointAsStr() {
        List<String> result = new LinkedList<String>();
        for (Entry<RayCarControl, RacerState> a: racers.entrySet()) {
            result.add(a.getKey().getCarData().name + " " + a.getValue().nextCheckpoint.num);
        }
        return H.str(result.toArray(), "\n");
    }

    protected void ghostCollision(GhostControl ghost, RigidBodyControl obj) {
        Checkpoint checkpoint = getIfCheckpoint(ghost);
        RacerState racer = getIfCar(obj);
        if (checkpoint == null || racer == null)
            return;

        if (racer.nextCheckpoint.num == checkpoint.num) {
            // then finally, update checkpoint
            int nextNum = (racer.nextCheckpoint.num + 1) % checkpoints.length;
            racer.nextCheckpoint = checkpoints[nextNum];
        }
    }

    private RacerState getIfCar(RigidBodyControl pObject) {
        for (Entry<RayCarControl, RacerState> racer : racers.entrySet())
            if (pObject == racer.getKey().getPhysicsObject())
                return racer.getValue();
        return null;
    }

    private Checkpoint getIfCheckpoint(GhostControl ghost) {
        for (Checkpoint checkpoint : checkpoints)
            if (checkpoint.ghost == ghost)
                return checkpoint;
        return null;
    }


    class RacerState {
        public int lap;
        public Checkpoint nextCheckpoint;
        public Geometry arrow;

        public RacerState(Checkpoint check) {
            this.nextCheckpoint = check;
        }
    }

    class Checkpoint {
        public final int num;
        public final Vector3f position;
        public final GhostControl ghost;

        Checkpoint(int num, Vector3f pos, GhostControl ghost) {
            this.num = num;
            this.position = pos;
            this.ghost = ghost;
        }
    }

    class GhostObjectCollisionListener implements PhysicsCollisionListener {

        private final DriveRaceProgress progress;
        public GhostObjectCollisionListener(DriveRaceProgress progress) {
            this.progress = progress;
        }

        @Override
        public void collision(PhysicsCollisionEvent event) {
            Spatial a = event.getNodeA();
            Spatial b = event.getNodeB();

            if (a.getControl(GhostControl.class) != null && b.getControl(GhostControl.class) != null) {
                return; // ignore and ghost,ghost collisions
            }
            
            if (a.getControl(GhostControl.class) == null && b.getControl(GhostControl.class) == null) {
                return; // ignore and non-ghost, non-ghost collisions
            }
            
            if (a.getControl(GhostControl.class) != null && isMovingBody(event.getObjectB())) {
                progress.ghostCollision((GhostControl) event.getObjectA(), (RigidBodyControl)event.getObjectB());
            }
            
            if (b.getControl(GhostControl.class) != null && isMovingBody(event.getObjectA())) {
                progress.ghostCollision((GhostControl) event.getObjectB(), (RigidBodyControl)event.getObjectA());
            }
        }

        private boolean isMovingBody(PhysicsCollisionObject obj) {
            if (!(obj instanceof RigidBodyControl)) {
                return false; // if its not a rigid body, no idea what to do
            }

            RigidBodyControl control = (RigidBodyControl) obj;
            if (control.isKinematic() || control.getMass() == 0)
                return false; // only concerned about 'moving' collisions

            return true;
        }
    }
}
