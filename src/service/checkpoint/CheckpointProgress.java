package service.checkpoint;

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

// TODO show the player's current checkpoint highlighted in some way?
public class CheckpointProgress extends BaseAppState {

    private final RayCarControl player;

    private final CheckpointListener listener;
    private final RacePositionEngine engine;

    private final Node rootNode;
    private final List<Vector3f> preInitCheckpoints;
    private final List<Checkpoint> attachedCheckpoints;

    private CollisionShape colShape;

    private Spatial baseSpat;
    private boolean attachModels;

    private final Type type;

    public enum Type {
        Lap, Sprint
    }

    public CheckpointProgress(Type type, Vector3f[] checkpoints, Collection<RayCarControl> cars, RayCarControl player) {
        this.type = type; // TODO this should be different classes somehow

        if (type == Type.Lap && (checkpoints == null || checkpoints.length < 2))
            throw new IllegalStateException(Type.Lap + " type should set checkpoints");

        this.player = player;

        this.preInitCheckpoints = new LinkedList<>(Arrays.asList(checkpoints));
        this.engine = new RacePositionEngine(cars);
        
        this.attachModels = true;
        this.rootNode = new Node("checkpoint progress root");
        this.attachedCheckpoints = new LinkedList<>();

        this.listener = new CheckpointListener((ghost) -> {
            return engine.getIfCheckpoint(ghost);
        }, (body) -> {
            return engine.getIfRayCar(body);
        }, (racer) -> {
            engine.racerHitCheckpoint(racer, racer.nextCheckpoint);
        });
    }

    public void setVisualModels(boolean isVisual) {
        this.attachModels = isVisual;
        if (this.isInitialized()) {
            if (isVisual)
                ((SimpleApplication) getApplication()).getRootNode().attachChild(rootNode);
            else
                ((SimpleApplication) getApplication()).getRootNode().detachChild(rootNode);
        }
    }

    public void setCheckpointModel(Spatial spat) {
        if (this.isInitialized())
            throw new IllegalStateException("This must be only called before initialization.");
        this.baseSpat = spat;
    }

    @Override
    protected void initialize(Application app) {
        if (attachModels)
            ((SimpleApplication) app).getRootNode().attachChild(rootNode);

        // generate the checkpoint objects
        colShape = CollisionShapeFactory.createBoxShape(baseSpat);
        for (Vector3f checkPos : preInitCheckpoints)
            attachCheckpoint(checkPos);


        engine.init(app);

        listener.startListening(getState(BulletAppState.class).getPhysicsSpace());
    }

    /** Adds a checkpoint to the list, not applicable for lap type */
    public void addCheckpoint(Vector3f pos) {
        if (this.type == Type.Lap)
            throw new IllegalStateException("The " + Type.Sprint + " type cannot use this method");
        if (this.isInitialized())
            preInitCheckpoints.add(pos);
        attachCheckpoint(pos);
    }

    private void attachCheckpoint(Vector3f pos) {
        //due to no removing, the last checkpoint is always the previous one
        Vector3f prevCheckpointPos = engine.getLastCheckpointPos();
        if (prevCheckpointPos == null)
            prevCheckpointPos  = pos;

        Spatial box = baseSpat.clone();
        box.setLocalTranslation(pos);
        // rotate box to angle towards
        Quaternion q = new Quaternion();
        if (prevCheckpointPos != pos)
            q.lookAt(pos.subtract(prevCheckpointPos), Vector3f.UNIT_Y);
        box.rotate(q);

        GhostControl ghost = new GhostControl(colShape);
        box.addControl(ghost);
        rootNode.attachChild(box);

        Checkpoint check = new Checkpoint(engine.getCheckpointCount(), pos, ghost, box);
        engine.addCheckpoint(check);
    }

    

    public void setMinCheckpoint(Vector3f pos) {
        if (this.type == Type.Lap)
            throw new IllegalStateException("The " + Type.Sprint + " type cannot use this method");

        Checkpoint check = engine.getCheckpointFromPos(pos);
        if (check == null)
            return;

        // called by the race class so that this can remove old checkpoints
        // so that cars don't ever lose the checkpoint
        // and update anyone really behind with a better, valid one

        for (RacerState racer : this.getRaceState()) {
            if (racer.nextCheckpoint.num <= check.num) {
                engine.racerHitCheckpoint(racer, check);

                Vector3f dir = racer.nextCheckpoint.position.subtract(racer.lastCheckpoint.position).normalize();
                Quaternion q = new Quaternion();
                q.lookAt(dir, new Vector3f());
                racer.car.setPhysicsProperties(racer.lastCheckpoint.position.add(0, 1, 0), dir.mult(10), q,
                        new Vector3f());
            }
        }

        // remove all visual checkpoints
        for (Checkpoint curC: engine.getAllPreviousCheckpoints(check.num)) {
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
        return engine.getRacerState(player);
    }

    protected List<RacerState> getRaceState() {
        return engine.getAllRaceStates();
    }

    @Override
    public void update(float tpf) {
        // lazy load ghost controls until someone is up to it for physics lag reasons
        // Even though this is called every frame it isn't that slow

        // TODO change the collision channel of the ghost objects to prevent colliding with ground every update
        // https://wiki.jmonkeyengine.org/jme3/advanced/physics.html see PhysicsControl.addCollideWithGroup

        Collection<Checkpoint> ghosts = this.engine.getNextCheckpoints();
        
        for (Checkpoint check : this.attachedCheckpoints) // remove old
            getState(BulletAppState.class).getPhysicsSpace().remove(check.ghost);

        for (Checkpoint check : ghosts) // add new
            getState(BulletAppState.class).getPhysicsSpace().add(check.ghost);

        this.attachedCheckpoints.clear();
        this.attachedCheckpoints.addAll(ghosts);
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

    public Vector3f getNextCheckpoint(RayCarControl car) {
        return engine.getRacerNextCheckpoint(car);
    }

    public Vector3f getLastCheckpoint(RayCarControl car) {
        return engine.getRacerLastCheckpoint(car);
    }

    public static Spatial GetDefaultCheckpointModel(Application app, float scale) {
        return GetDefaultCheckpointModel(app, scale, new ColorRGBA(0, 0, 0, 1));
    }

    public static Spatial GetDefaultCheckpointModel(Application app, float scale, ColorRGBA colour) {
        Vector3f checkpointSize = Vector3f.UNIT_XYZ.mult(scale);
        Spatial baseSpat = new Geometry("checkpoint", new Box(checkpointSize.negate(), checkpointSize));
        Spatial out = LoadModelWrapper.create(app.getAssetManager(), baseSpat, colour);
        for (Geometry g : H.getGeomList(out)) {
            g.getMaterial().getAdditionalRenderState().setWireframe(true);
            g.getMaterial().getAdditionalRenderState().setLineWidth(5);
        }

        return out;
    }
}
