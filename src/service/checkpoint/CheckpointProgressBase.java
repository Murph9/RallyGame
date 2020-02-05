package service.checkpoint;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
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
import helper.Duo;
import service.GhostObjectCollisionListener;

/**
 * CheckpointProgressBase
 */
public abstract class CheckpointProgressBase extends BaseAppState implements GhostObjectCollisionListener.IListener {
    
    protected final RayCarControl player;
    protected final GhostObjectCollisionListener checkpointCollisionListener;

    protected final Node rootNode;
    protected final Map<RayCarControl, RacerState> racers;
    protected final Map<Integer, Instant> timeAtCheckpoints;

    protected Checkpoint[] checkpoints;

    protected Spatial baseSpat;
    protected float checkpointScale;
    protected ColorRGBA checkpointColour;
    protected boolean attachModels;
    protected CollisionShape colShape;

    public CheckpointProgressBase(Collection<RayCarControl> cars, RayCarControl player) {
        this.checkpointScale = 2;
        this.checkpointColour = new ColorRGBA(0, 1, 0, 0.4f);
        this.attachModels = true;

        this.rootNode = new Node("progress root node");

        this.racers = new HashMap<>();
        for (RayCarControl car : cars) {
            this.racers.put(car, new RacerState(car));
        }
        this.timeAtCheckpoints = new HashMap<>();

        this.checkpointCollisionListener = new GhostObjectCollisionListener(this);

        this.player = player;
    }

    public void setBoxCheckpointSize(float size) {
        if (this.isInitialized())
            throw new IllegalStateException("This must be called before initialization.");
        this.checkpointScale = size;
    }

    public void setBoxCheckpointColour(ColorRGBA colour) {
        if (this.isInitialized())
            throw new IllegalStateException("This must be called before initialization.");
        this.checkpointColour = colour;
    }

    public void attachVisualModel(boolean attach) {
        if (this.isInitialized())
            throw new IllegalStateException("This must be called before initialization.");
        this.attachModels = attach;
    }

    public void setCheckpointModel(Spatial spat) {
        if (this.isInitialized())
            throw new IllegalStateException("This must be called before initialization.");
        this.baseSpat = spat;
        // TODO figure out checkpoint rotating
    }

    @Override
    protected void initialize(Application app) {
        ((SimpleApplication) app).getRootNode().attachChild(rootNode);

        PhysicsSpace physicsSpace = getState(BulletAppState.class).getPhysicsSpace();

        // generate the checkpoint objects
        if (baseSpat == null) {
            Vector3f checkpointSize = Vector3f.UNIT_XYZ.mult(checkpointScale);
            baseSpat = new Geometry("checkpoint", new Box(checkpointSize.negate(), checkpointSize));
            baseSpat = LoadModelWrapper.create(app.getAssetManager(), baseSpat, checkpointColour);
        }
        this.colShape = CollisionShapeFactory.createBoxShape(baseSpat);

        physicsSpace.addCollisionListener(checkpointCollisionListener);
    }

    @Override
    protected void onDisable() {
    }

    @Override
    protected void onEnable() {
    }

    @Override
    public void update(float tpf) {
        // this is intentionally blank
    }

    public abstract RayCarControl isThereAWinner(int laps, int checkpoints);

    @Override
    public void ghostCollision(GhostControl ghost, RigidBodyControl obj) {
        Checkpoint checkpoint = getIfCheckpoint(ghost);
        RacerState racer = getIfCar(obj);
        if (checkpoint == null || racer == null)
            return;

        if (racer.nextCheckpoint == checkpoint) {
            // update checkpoints
            Duo<Integer, Integer> nextCheckpoint = calcNextCheckpoint(racer, checkpoints.length);
            racer.lastCheckpoint = racer.nextCheckpoint;
            racer.nextCheckpoint = checkpoints[nextCheckpoint.second];
            racer.lap = nextCheckpoint.first;

            // update last time
            int fakeCheckpointHash = racer.lap * 10000 + checkpoint.num;
            if (!timeAtCheckpoints.containsKey(fakeCheckpointHash)) {
                timeAtCheckpoints.put(fakeCheckpointHash, Instant.now());
                racer.duration = Duration.ZERO;
            } else {
                racer.duration = Duration.between(timeAtCheckpoints.get(fakeCheckpointHash), Instant.now());
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

    public Vector3f getNextCheckpoint(RayCarControl car) {
        Checkpoint check = racers.get(car).nextCheckpoint;
        if (check == null)
            return null;
        return check.position;
    }

    public Vector3f getLastCheckpoint(RayCarControl car) {
        Checkpoint check = racers.get(car).lastCheckpoint;
        if (check == null)
            return null;
        return check.position;
    }

    /** Lap,checkpoint */
    public static Duo<Integer, Integer> calcNextCheckpoint(RacerState racer, int checkpointCount) {
        int nextNum = racer.nextCheckpoint.num + 1;
        int lap = racer.lap;
        if (nextNum >= checkpointCount) {
            nextNum = 0;
            lap++;
        }
        return new Duo<>(lap, nextNum);
    }
}
