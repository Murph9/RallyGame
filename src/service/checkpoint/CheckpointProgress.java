package service.checkpoint;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Function;

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
import service.GhostObjectCollisionListener;

// TODO figure out checkpoint rotation
// TODO dynamic point to point races? (it already handles static point to point)
public class CheckpointProgress extends BaseAppState {

    private final Vector3f[] checkpointPositions;
    private final RayCarControl player;

    private final CheckpointListener listener;

    private Checkpoint firstCheckpoint;
    private final List<Checkpoint> checkpoints;
    private final Node rootNode;
    private final Map<RayCarControl, RacerState> racers;
    private final Map<Integer, Instant> timeAtCheckpoints;

    private Spatial baseSpat;
    private boolean attachModels;

    public CheckpointProgress(Vector3f[] checkpoints, Collection<RayCarControl> cars, RayCarControl player) {
        this.checkpointPositions = checkpoints;
        this.checkpoints = new ArrayList<Checkpoint>(checkpoints.length);
        
        this.attachModels = true;
        this.player = player;
        this.rootNode = new Node("checkpoint progress root");

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
                    // update to next checkpoint
                    int nextNum = (racer.nextCheckpoint.num + 1 % this.checkpoints.size());
                    if (nextNum == 0)
                        racer.lap++;
                    racer.lastCheckpoint = racer.nextCheckpoint;
                    racer.nextCheckpoint = this.checkpoints.get(nextNum);

                    // update last time
                    int fakeCheckpointHash = racer.lap * 10000 + racer.lastCheckpoint.num;
                    if (!timeAtCheckpoints.containsKey(fakeCheckpointHash)) {
                        timeAtCheckpoints.put(fakeCheckpointHash, Instant.now());
                        racer.duration = Duration.ZERO;
                    } else {
                        racer.duration = Duration.between(timeAtCheckpoints.get(fakeCheckpointHash), Instant.now());
                    }
                });
    }

    public void attachVisualModel(boolean attach) {
        if (this.isInitialized()) throw new IllegalStateException("This must be only called before initialization.");
        this.attachModels = attach;
    }
    public void setCheckpointModel(Spatial spat) {
        if (this.isInitialized()) throw new IllegalStateException("This must be only called before initialization.");
        this.baseSpat = spat;
    }

    @Override
    protected void initialize(Application app) {
        ((SimpleApplication) app).getRootNode().attachChild(rootNode);

        PhysicsSpace physicsSpace = getState(BulletAppState.class).getPhysicsSpace();

        // generate the checkpoint objects
        CollisionShape colShape = CollisionShapeFactory.createBoxShape(baseSpat);

        for (int i = 0; i < checkpointPositions.length; i++) {
            GhostControl ghost = new GhostControl(colShape);

            Spatial box = baseSpat.clone();
            box.setLocalTranslation(checkpointPositions[i]);
            box.addControl(ghost);
            if (attachModels)
                rootNode.attachChild(box);
            physicsSpace.add(ghost);

            this.checkpoints.add(new Checkpoint(i, checkpointPositions[i], ghost));
        }
        this.firstCheckpoint = this.checkpoints.get(0);

        //set progress values
        for (RacerState racer : this.racers.values()) {
            racer.lastCheckpoint = this.firstCheckpoint;
            racer.nextCheckpoint = this.firstCheckpoint;
        }

        listener.startListening(physicsSpace);
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
        // this is intentionally blank
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
        return GetDefaultCheckpointModel(app, scale, new ColorRGBA(0, 1, 0, 0.4f));
    }
    public static Spatial GetDefaultCheckpointModel(Application app, float scale, ColorRGBA colour) {
        Vector3f checkpointSize = Vector3f.UNIT_XYZ.mult(scale);
        Spatial baseSpat = new Geometry("checkpoint", new Box(checkpointSize.negate(), checkpointSize));
        return LoadModelWrapper.create(app.getAssetManager(), baseSpat, colour);
    }

    protected class CheckpointListener implements GhostObjectCollisionListener.IListener {
        
        private final Function<GhostControl, Checkpoint> ifCheckpoint;
        private final Function<RigidBodyControl, RacerState> ifCar;
        private final Consumer<RacerState> nextCheckpoint;

        private final GhostObjectCollisionListener checkpointCollisionListener;

        public CheckpointListener(Function<GhostControl, Checkpoint> ifCheckpoint,
            Function<RigidBodyControl, RacerState> ifCar,
            Consumer<RacerState> nextCheckpoint) {
            this.ifCheckpoint = ifCheckpoint;
            this.ifCar = ifCar;
            this.nextCheckpoint = nextCheckpoint;

            this.checkpointCollisionListener = new GhostObjectCollisionListener(this);
        }

        public void startListening(PhysicsSpace space) {
            space.addCollisionListener(checkpointCollisionListener);
        }
        public void stopListening(PhysicsSpace space) {
            space.removeCollisionListener(checkpointCollisionListener);
        }

        @Override
        public void ghostCollision(GhostControl ghost, RigidBodyControl obj) {
            Checkpoint checkpoint = ifCheckpoint.apply(ghost);
            RacerState racer = ifCar.apply(obj);
            if (checkpoint == null || racer == null)
                return;

            if (racer.nextCheckpoint == checkpoint) {
                nextCheckpoint.accept(racer);
            }
        }
    }
}
