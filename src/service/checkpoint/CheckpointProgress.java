package service.checkpoint;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

import car.ray.RayCarControl;
import effects.LoadModelWrapper;
import helper.H;

// TODO figure out checkpoint rotation
// TODO show their current checkpoint to the player

// TODO change the collision channel of the ghost objects to prevent colliding with ground every update
// https://wiki.jmonkeyengine.org/jme3/advanced/physics.html see PhysicsControl.addCollideWithGroup
public class CheckpointProgress extends BaseAppState {

    private final Vector3f[] initCheckpointPositions;
    private final RayCarControl player;

    private final CheckpointListener listener;

    private Checkpoint firstCheckpoint;
    private final List<Checkpoint> checkpoints;
    private final Map<RayCarControl, RacerState> racers;
    private final Map<Integer, Instant> timeAtCheckpoints;

    private final Node rootNode;
    private final List<Checkpoint> attachedCheckpoints;

    private CollisionShape colShape;

    private Spatial baseSpat;
    private boolean attachModels;
    
    private final Type type;
    public enum Type {
        Lap,
        Sprint
    }

    public CheckpointProgress(Type type, Vector3f[] checkpoints, Collection<RayCarControl> cars, RayCarControl player) {
        this.type = type; //TODO this should be different classes somehow

        if (checkpoints == null || checkpoints.length < 2)
            throw new IllegalStateException(Type.Lap + " type should set checkpoints");
        this.initCheckpointPositions = checkpoints;
        this.checkpoints = new ArrayList<Checkpoint>(checkpoints.length);
        
        this.attachModels = true;
        this.player = player;
        this.rootNode = new Node("checkpoint progress root");
        this.attachedCheckpoints = new LinkedList<>();

        this.racers = new HashMap<>();
        for (RayCarControl car : cars) {
            this.racers.put(car, new RacerState(car));
        }
        this.timeAtCheckpoints = new HashMap<>();


        this.listener = new CheckpointListener(
                (ghost) -> {
                    for (Checkpoint checkpoint : this.checkpoints)
                        if (checkpoint.ghost == ghost)
                            return checkpoint;
                    return null;
                },
                (body) -> {
                    for (Entry<RayCarControl, RacerState> racer : racers.entrySet())
                        if (body == racer.getKey().getPhysicsObject())
                            return racer.getValue();
                    return null;
                },
                (racer) -> {
                    racerCompletedCheckpoint(racer, racer.nextCheckpoint.num);
                });
    }

    private void racerCompletedCheckpoint(RacerState racer, int checkNum) {
        //calc next checkpoint then
        int nextNum = (checkNum + 1 % this.checkpoints.size());
        if (nextNum == 0)
            racer.lap++;

        // update to given checkpoints
        racer.lastCheckpoint = this.checkpoints.get(checkNum);
        racer.nextCheckpoint = this.checkpoints.get(nextNum);

        updateTimingHash(racer);
    }

    private void updateTimingHash(RacerState racer) {
        // update last time
        int fakeCheckpointHash = racer.lap * 10000 + racer.lastCheckpoint.num;
        if (!timeAtCheckpoints.containsKey(fakeCheckpointHash)) {
            timeAtCheckpoints.put(fakeCheckpointHash, Instant.now());
            racer.duration = Duration.ZERO;
        } else {
            racer.duration = Duration.between(timeAtCheckpoints.get(fakeCheckpointHash), Instant.now());
        }
    }

    public void setCheckpointModel(Spatial spat, boolean isVisual) {
        if (this.isInitialized())
            throw new IllegalStateException("This must be only called before initialization.");
        this.attachModels = isVisual; // TODO we can change this to just toggle the root node, removing the init condition
        this.baseSpat = spat;
    }

    @Override
    protected void initialize(Application app) {
        ((SimpleApplication) app).getRootNode().attachChild(rootNode);

        PhysicsSpace physicsSpace = getState(BulletAppState.class).getPhysicsSpace();

        // generate the checkpoint objects
        colShape = CollisionShapeFactory.createBoxShape(baseSpat);

        for (int i = 0; i < initCheckpointPositions.length; i++) {
            addCheckpoint(initCheckpointPositions[i]);
        }
        this.firstCheckpoint = this.checkpoints.get(0);

        //set progress values
        for (RacerState racer : this.racers.values()) {
            racer.lastCheckpoint = this.firstCheckpoint;
            racer.nextCheckpoint = this.firstCheckpoint;
        }

        listener.startListening(physicsSpace);
    }

    /** Adds a checkpoint to the list, doesn't affect laps at all */
    public void addCheckpoint(Vector3f pos) {
        if (this.type == Type.Lap)
            throw new IllegalStateException("Only a " + Type.Sprint + " type should use this method");
        if (!this.isInitialized()) // TODO this can be fixed if we get them all before init and batch them
            throw new IllegalStateException("This must be only called after initialization.");

        GhostControl ghost = new GhostControl(colShape);
        Spatial box = baseSpat.clone();
        box.setLocalTranslation(pos);
        box.addControl(ghost);
        if (attachModels)
            rootNode.attachChild(box);

        int checkpointCount = this.checkpoints.size();
        this.checkpoints.add(new Checkpoint(checkpointCount, pos, ghost, box));
    }

    private Checkpoint getCheckpointFromPos(Vector3f pos) {
        Checkpoint check = null;
        for (Checkpoint c : this.checkpoints) {
            if (c.position.equals(pos)) {
                check = c;
                break;
            }
        }
        return check;
    }

    public void setMinCheckpoint(Vector3f pos) {
        if (this.type == Type.Lap)
            throw new IllegalStateException("Only a " + Type.Sprint + " type should use this method");

        Checkpoint check = getCheckpointFromPos(pos);
        if (check == null)
            return;
        
        
        // called by the race class so that this can remove old checkpoints
        // so that cars don't ever lose the checkpoint
        // and update anyone really behind with a better, valid one
        
        for (RacerState racer: this.getRaceState()) {
            if (racer.nextCheckpoint.num <= check.num + 1) {
                racerCompletedCheckpoint(racer, check.num + 1);

                Vector3f dir = racer.nextCheckpoint.position.subtract(racer.lastCheckpoint.position).normalize();
                Quaternion q = new Quaternion();
                q.lookAt(dir, new Vector3f());
                racer.car.setPhysicsProperties(racer.lastCheckpoint.position.add(0, 1, 0), dir.mult(10), q, new Vector3f());
            }
        }

        //remove all visual checkpoints
        int checkNum = check.num;
        for (int i = checkNum - 1; i > 0; i--) {
            Checkpoint curC = this.checkpoints.get(i);
            if (curC == null)
                break;
            curC.visualModel.removeFromParent();
        }
    }

    public RayCarControl isThereSomeoneAtState(int laps, int checkpoints) {
        List<RacerState> racers = getRaceState();
        Collections.sort(racers);

        RacerState racer = racers.get(0);
        if (racer.lap >= laps && racer.lastCheckpoint != null && racer.lastCheckpoint.num >= checkpoints)
            return racer.car;

        return null;
    }

    @Override
    protected void onDisable() {
    }

    @Override
    protected void onEnable() {
    }

    public RacerState getPlayerRacerState() {
        return this.racers.get(player);
    }

    protected List<RacerState> getRaceState() {
        return new ArrayList<>(this.racers.values());
    }

    @Override
    public void update(float tpf) {
        attachNextCheckpoints();
    }

    private void attachNextCheckpoints() {
        // lazy load ghost controls until someone is up to it for physics lag reasons

        List<Checkpoint> ghosts = new LinkedList<>();
        for (RacerState racer : this.racers.values())
            ghosts.add(racer.nextCheckpoint);

        for (Checkpoint check : this.attachedCheckpoints)
            getState(BulletAppState.class).getPhysicsSpace().remove(check.ghost);

        for (Checkpoint check : ghosts)
            getState(BulletAppState.class).getPhysicsSpace().add(check.ghost);

        this.attachedCheckpoints.clear();
        this.attachedCheckpoints.addAll(ghosts);
    }

    @Override
    public void cleanup(Application app) {
        ((SimpleApplication) app).getRootNode().detachChild(rootNode);

        PhysicsSpace physicsSpace = app.getStateManager().getState(BulletAppState.class).getPhysicsSpace();
        for (Checkpoint checkpoint : checkpoints) {
            physicsSpace.remove(checkpoint.ghost);
        }

        listener.stopListening(physicsSpace);
    }

    public Vector3f getNextCheckpoint(RayCarControl car) {
        Checkpoint check = racers.get(car).nextCheckpoint;
        if (check == null) return null;
        return check.position;
    }

    public Vector3f getLastCheckpoint(RayCarControl car) {
        Checkpoint check = racers.get(car).lastCheckpoint;
        if (check == null) return null;
        return check.position;
    }

    public static Spatial GetDefaultCheckpointModel(Application app, float scale) {
        return GetDefaultCheckpointModel(app, scale, new ColorRGBA(0, 0, 0, 1));
    }
    public static Spatial GetDefaultCheckpointModel(Application app, float scale, ColorRGBA colour) {
        Vector3f checkpointSize = Vector3f.UNIT_XYZ.mult(scale);
        Spatial baseSpat = new Geometry("checkpoint", new Box(checkpointSize.negate(), checkpointSize));
        Spatial out = LoadModelWrapper.create(app.getAssetManager(), baseSpat, colour);
        for (Geometry g :H.getGeomList(out)) {
            g.getMaterial().getAdditionalRenderState().setWireframe(true);
            g.getMaterial().getAdditionalRenderState().setLineWidth(5);
        }

        return out;
    }
}
