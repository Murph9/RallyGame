package rallygame.service.checkpoint;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import rallygame.car.ray.RayCarControl;

public abstract class CheckpointProgressBase extends BaseAppState implements ICheckpointProgress {

    private final RayCarControl player;

    private final CheckpointListener listener;
    protected final RacePositionEngine engine;

    private final Node rootNode;
    protected final List<Vector3f> preInitCheckpoints;
    private final List<Checkpoint> attachedCheckpoints;

    private CollisionShape colShape;
    private Spatial baseSpat;

    public CheckpointProgressBase(Vector3f[] checkpoints, Collection<RayCarControl> cars, RayCarControl player) {
        this.player = player;

        this.preInitCheckpoints = new LinkedList<>(Arrays.asList(checkpoints));
        this.engine = new RacePositionEngine(cars);
        
        this.rootNode = new Node("checkpoint progress root");
        this.attachedCheckpoints = new LinkedList<>();

        this.listener = new CheckpointListener(engine::getIfCheckpoint, engine::getIfRayCar, (racer) -> {
            engine.racerHitCheckpoint(racer, racer.nextCheckpoint);
        });
    }

    public void setCheckpointModel(Spatial spat) {
        if (this.isInitialized())
            throw new IllegalStateException("This must be only called before initialization.");
        this.baseSpat = spat;
    }

    @Override
    protected void initialize(Application app) {
        ((SimpleApplication) app).getRootNode().attachChild(rootNode);

        // generate the checkpoint objects
        colShape = CollisionShapeFactory.createBoxShape(baseSpat);
        for (Vector3f checkPos : preInitCheckpoints)
            attachCheckpoint(checkPos);
        preInitCheckpoints.clear();

        engine.init(app);

        listener.startListening(getState(BulletAppState.class).getPhysicsSpace());
    }

    protected void attachCheckpoint(Vector3f pos) {
        //due to no removing, the last checkpoint is always the previous one
        Vector3f prevCheckpointPos = engine.getLastCheckpointPos();
        if (prevCheckpointPos == null)
            prevCheckpointPos = pos;

        Spatial box = baseSpat.clone();
        box.setLocalTranslation(pos);
        // rotate box to angle towards
        Quaternion q = new Quaternion();
        if (prevCheckpointPos != pos)
            q.lookAt(pos.subtract(prevCheckpointPos), Vector3f.UNIT_Y);
        box.rotate(q);

        GhostControl ghost = new GhostControl(colShape);
        ghost.setCollisionGroup(PhysicsCollisionObject.COLLISION_GROUP_02);
        ghost.setCollideWithGroups(PhysicsCollisionObject.COLLISION_GROUP_02);
        box.addControl(ghost);

        Checkpoint check = new Checkpoint(engine.getCheckpointCount(), pos, ghost, box);
        engine.addCheckpoint(check);
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
        return engine.getRacerState(player);
    }

    public List<RacerState> getRaceState() {
        return engine.getAllRaceStates();
    }

    @Override
    public void update(float tpf) {
        // lazy load ghost controls until someone is up to it for physics lag reasons
        // Even though this is called every frame it isn't that slow

        // TODO change the collision channel of the ghost objects to prevent colliding with ground every update
        // https://wiki.jmonkeyengine.org/jme3/advanced/physics.html see PhysicsControl.addCollideWithGroup
        var physicsSpace = getState(BulletAppState.class).getPhysicsSpace();

        Collection<Checkpoint> newCheckpoints = this.engine.getNextCheckpoints();
        Checkpoint[] old = this.attachedCheckpoints.stream().filter(x -> { return !newCheckpoints.contains(x); }).toArray(Checkpoint[]::new);
        for (var o : old) {
            physicsSpace.remove(o.ghost); // remove old checkpoints
        }

        Checkpoint[] _new = newCheckpoints.stream().filter(x -> { return !attachedCheckpoints.contains(x); }).toArray(Checkpoint[]::new);
        for (var n : _new) { // add new
            physicsSpace.add(n.ghost);
        }

        this.attachedCheckpoints.clear();
        this.attachedCheckpoints.addAll(newCheckpoints);

        //show only the next 2 checkpoints to the player
        for (Spatial sp : rootNode.getChildren())
            sp.removeFromParent();
        for (Spatial sp : engine.getRacerNextCheckpointsVisual(player, 2))
            rootNode.attachChild(sp);
    }

    @Override
    public void cleanup(Application app) {
        rootNode.removeFromParent();

        PhysicsSpace physicsSpace = app.getStateManager().getState(BulletAppState.class).getPhysicsSpace();
        for (Checkpoint c: engine.getAllPreviousCheckpoints(Integer.MAX_VALUE)) {
            physicsSpace.remove(c.ghost);
        }

        listener.stopListening(physicsSpace);
    }

    public Vector3f[] getNextCheckpoints(RayCarControl car, int count) {
        return engine.getRacerNextCheckpoints(car, count);
    }
    public Vector3f getNextCheckpoint(RayCarControl car) {
        var nextChekpoints = getNextCheckpoints(car, 1);
        if (nextChekpoints.length < 1)
            return null;

        return nextChekpoints[0];
    }

    public Vector3f getLastCheckpoint(RayCarControl car) {
        return engine.getRacerLastCheckpoint(car);
    }
}