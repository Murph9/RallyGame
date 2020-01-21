package drive.race;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
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

public class DriveRaceProgress extends BaseAppState {

    private final float checkpointScale;
    private final ColorRGBA checkpointColour;
    private final Vector3f[] checkpointPositions;
    private final RayCarControl player;
    private final GhostObjectCollisionListener checkpointCollisionListener;

    private Checkpoint firstCheckpoint;
    private final Checkpoint[] checkpoints;
    private Node rootNode;
    private HashMap<RayCarControl, RacerState> racers;

    // debug things
    private final boolean ifDebug;
    private Node debugNode;

    protected DriveRaceProgress(Vector3f[] checkpoints, Collection<RayCarControl> cars, RayCarControl player, boolean debug) {
        this.checkpointPositions = checkpoints;
        this.checkpoints = new Checkpoint[checkpoints.length];
        this.checkpointScale = 2;
        this.checkpointColour = new ColorRGBA(0, 1, 0, 0.4f);

        this.racers = new HashMap<>();
        for (RayCarControl car : cars) {
            this.racers.put(car, new RacerState(car.getCarData().name));
        }

        this.checkpointCollisionListener = new GhostObjectCollisionListener(this);

        this.player = player;
        this.ifDebug = debug;
    }

    @Override
    protected void initialize(Application app) {
        this.rootNode = new Node("progress root node");
        ((SimpleApplication) app).getRootNode().attachChild(rootNode);

        PhysicsSpace physicsSpace = getState(BulletAppState.class).getPhysicsSpace();

        Vector3f checkpointSize = Vector3f.UNIT_XYZ.mult(checkpointScale);
        for (int i = 0; i < checkpointPositions.length; i++) {
            Spatial box = new Geometry("checkpoint box " + i, new Box(checkpointSize.negate(), checkpointSize));
            box = LoadModelWrapper.create(app.getAssetManager(), box, checkpointColour);

            GhostControl ghost = new GhostControl(CollisionShapeFactory.createBoxShape(box));
            box.setLocalTranslation(checkpointPositions[i]);
            box.addControl(ghost);
            rootNode.attachChild(box);
            physicsSpace.add(ghost);

            this.checkpoints[i] = new Checkpoint(i, checkpointPositions[i], ghost);
        }
        this.firstCheckpoint = this.checkpoints[0];

        for (RacerState racer: this.racers.values()) {
            racer.nextCheckpoint = this.firstCheckpoint;
        }

        physicsSpace.addCollisionListener((PhysicsCollisionListener) checkpointCollisionListener);
    }

    @Override
    protected void onDisable() {}
    @Override
    protected void onEnable() {}

    public RacerState getPlayerRacerState() {
        return this.racers.get(player);
    }
    protected List<RacerState> getRaceState() {
        return new ArrayList<>(this.racers.values());
    }

    @Override
    public void update(float tpf) {
        //calc distance to nextCheckpoint
        for (Entry<RayCarControl, RacerState> entry: this.racers.entrySet()) {
            Vector3f carPos = entry.getKey().getPhysicsLocation();
            entry.getValue().calcCheckpointDistance(carPos);
        }

        if (ifDebug) {
            // update the checkpoint arrows
            if (debugNode != null)
                rootNode.detachChild(debugNode);
            debugNode = new Node("debugnode");
            for (Entry<RayCarControl, RacerState> entry : racers.entrySet()) {
                entry.getValue().arrow = H.makeShapeLine(getApplication().getAssetManager(), ColorRGBA.Cyan, 
                        entry.getKey().getPhysicsLocation(), entry.getValue().nextCheckpoint.position, 3);
                debugNode.attachChild(entry.getValue().arrow);
            }
            rootNode.attachChild(debugNode);
        }
    }

    @Override
    public void cleanup(Application app) {
        ((SimpleApplication) app).getRootNode().detachChild(rootNode);

        PhysicsSpace physicsSpace = app.getStateManager().getState(BulletAppState.class).getPhysicsSpace();
        for (Checkpoint checkpoint : checkpoints) {
            physicsSpace.remove(checkpoint.ghost);
        }
        physicsSpace.removeCollisionListener((PhysicsCollisionListener) checkpointCollisionListener);
    }

    public Vector3f getNextCheckpoint(RayCarControl car) {
        Checkpoint check = racers.get(car).nextCheckpoint;
        if (check == null)
            return null;
        return check.position;
    }

    public Vector3f getCurrentCheckpoint(RayCarControl car) {
        Checkpoint check = racers.get(car).nextCheckpoint;
        if (check == null)
            return null;

        int index = check.num - 1;
        if (index < 0)
            index = this.checkpoints.length - 1;
        return this.checkpoints[index].position;
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

            if (racer.nextCheckpoint == firstCheckpoint) {
                racer.lap++;
            }
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
