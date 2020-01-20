package drive.race;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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

    private final AssetManager am;
    private final Checkpoint firstCheckpoint;
    private final Checkpoint[] checkpoints;
    private final Node rootNode;
    private final HashMap<RayCarControl, RacerState> racers;

    private final GhostObjectCollisionListener checkpointCollisionListener;

    private RayCarControl player;

    // debug things
    private boolean ifDebug;
    private Node debugNode;

    protected DriveRaceProgress(Application app, Vector3f[] checkpointPositions, Collection<RayCarControl> cars) {
        this(app, checkpointPositions, cars, 2, new ColorRGBA(0, 1, 0, 0.4f));
    }

    protected DriveRaceProgress(Application app, Vector3f[] checkpointPositions, Collection<RayCarControl> cars, float checkpointScale, ColorRGBA checkpointColour) {
        this.checkpoints = new Checkpoint[checkpointPositions.length];

        this.am = app.getAssetManager();
        this.rootNode = new Node("progress root node");
        ((SimpleApplication) app).getRootNode().attachChild(rootNode);

        PhysicsSpace physicsSpace = app.getStateManager().getState(BulletAppState.class).getPhysicsSpace();

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

        this.racers = new HashMap<>();
        for (RayCarControl car : cars) {
            this.racers.put(car, new RacerState(this.checkpoints[0]));
        }

        checkpointCollisionListener = new GhostObjectCollisionListener(this);
        physicsSpace.addCollisionListener((PhysicsCollisionListener) checkpointCollisionListener);
    }

    protected void setDebug(boolean ifDebug) {
        this.ifDebug = ifDebug;
        
        //when turning off remove debug node
        if (!ifDebug && debugNode != null) {
            rootNode.detachChild(debugNode);
        }
    }
    protected void setPlayer(RayCarControl player) {
        this.player = player;
    }

    protected void update(float tpf) {
        if (checkpoints == null || checkpoints.length < 1)
            return;

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
                entry.getValue().arrow = H.makeShapeLine(am, ColorRGBA.Cyan, 
                        entry.getKey().getPhysicsLocation(), entry.getValue().nextCheckpoint.position, 3);
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

    public Vector3f getNextCheckpoint(RayCarControl car) {
        return racers.get(car).nextCheckpoint.position;
    }

    public Vector3f getCurrentCheckpoint(RayCarControl car) {
        int index = this.racers.get(car).nextCheckpoint.num - 1;
        if (index < 0)
            index = this.checkpoints.length - 1;
        return this.checkpoints[index].position;
    }

    @Override
    public String toString() {
        List<Entry<RayCarControl, RacerState>> list = new ArrayList<Entry<RayCarControl, RacerState>>(racers.entrySet());
        Collections.sort(list, new Comparator<Entry<RayCarControl, RacerState>>() {
            @Override
            public int compare(Entry<RayCarControl, RacerState> o1, Entry<RayCarControl, RacerState> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        StringBuilder sb = new StringBuilder();
        int count = 1;
        int checkpointCount = this.checkpoints.length;
        for (Entry<RayCarControl, RacerState> a: list) {
            RacerState state = a.getValue();
            sb.append(count + " | " + a.getKey().getCarData().name
                    + " l:" + state.lap
                    + " ch:" + state.nextCheckpoint.num + "/" + checkpointCount
                    + " | " + H.roundDecimal(state.distanceToNextCheckpoint, 1) + "m"
                    + (player == a.getKey() ? "---" : "") + "\n");
            count++;
        }
        return sb.toString();
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
