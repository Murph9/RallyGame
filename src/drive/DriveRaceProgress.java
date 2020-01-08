package drive;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
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

    private final Application app;
    private final CheckpointClass[] checkpoints;
    private final Node rootNode;
    private final HashMap<RayCarControl, RacerState> racers;

    // debug things
    private Node debugNode;

    protected DriveRaceProgress(Application app, Vector3f[] checkpoints, Collection<RayCarControl> cars) {
        this.checkpoints = new CheckpointClass[checkpoints.length];
        this.app = app;
        this.rootNode = new Node("progress root node");
        ((SimpleApplication) app).getRootNode().attachChild(rootNode);

        racers = new HashMap<>();
        for (RayCarControl car : cars) {
            racers.put(car, new RacerState());
        }

        PhysicsSpace physicsSpace = app.getStateManager().getState(BulletAppState.class).getPhysicsSpace();

        for (int i = 0; i < checkpoints.length; i++) {
            this.checkpoints[i] = new CheckpointClass(i, checkpoints[i]);

            Spatial box = new Geometry("checkpoint box" + i, new Box(3, 3, 3));
            box = LoadModelWrapper.create(app.getAssetManager(), box, new ColorRGBA(0, 1, 0, 0.5f));

            this.checkpoints[i].ghost = new GhostControl(CollisionShapeFactory.createBoxShape(box));
            this.checkpoints[i].ghost.setUserObject("DriveRace checkpoint");
            box.setLocalTranslation(this.checkpoints[i].checkpoint);
            box.addControl(this.checkpoints[i].ghost);
            rootNode.attachChild(box);
            physicsSpace.add(this.checkpoints[i].ghost);
        }

        CheckpointCollisionListener checkpointCollisionListener = new CheckpointCollisionListener();
        physicsSpace.addCollisionListener((PhysicsCollisionListener) checkpointCollisionListener);
    }

    protected void update(float tpf) {
        if (checkpoints == null || checkpoints.length < 1)
            return;

        // update the checkpoint arrows
        if (debugNode != null)
            rootNode.detachChild(debugNode);
        debugNode = new Node("debugnode");
        for (Entry<RayCarControl, RacerState> entry : racers.entrySet()) {
            Vector3f pos = entry.getKey().getPhysicsLocation().add(0, 3, 0);
            Vector3f dir = checkpoints[entry.getValue().nextCheckpoint].checkpoint.subtract(pos);
            entry.getValue().arrow = H.makeShapeArrow(((SimpleApplication) app).getAssetManager(), ColorRGBA.Cyan, dir,
                    pos);
            debugNode.attachChild(entry.getValue().arrow);
        }
        rootNode.attachChild(debugNode);
    }

    public void cleanup() {
        ((SimpleApplication) app).getRootNode().detachChild(rootNode);

        for (CheckpointClass checkpoint : checkpoints) {
            app.getStateManager().getState(BulletAppState.class).getPhysicsSpace().remove(checkpoint.ghost);
        }
    }

    public Vector3f getNextCheckpoint(RayCarControl car, Vector3f pos) {
        if (checkpoints.length < 1)
            return null;
        return checkpoints[racers.get(car).nextCheckpoint].checkpoint;
    }

    public Vector3f getLastCheckpoint(RayCarControl car) {
        int index = this.racers.get(car).nextCheckpoint - 1;
        if (index < 0)
            index = this.checkpoints.length - 1;
        return this.checkpoints[index].checkpoint;
    }

    public String getCheckpointAsStr() {
        return H.str(racers.values().stream().map(c -> c.nextCheckpoint).toArray(), ",");
    }

    class RacerState {
        public int nextCheckpoint;
        public Geometry arrow;
    }

    class CheckpointClass { // better name please
        public final int num;
        public final Vector3f checkpoint;
        public GhostControl ghost;

        CheckpointClass(int num, Vector3f pos) {
            this.num = num;
            this.checkpoint = pos;
        }
    }

    class CheckpointCollisionListener implements PhysicsCollisionListener {

        public CheckpointCollisionListener() {
            //TODO take in list of ghost checkpoints and carnodes to decouple from progress class
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
            
            CheckpointClass checkpoint;
            RayCarControl aRayCar;
            if (a.getControl(GhostControl.class) != null) {
                checkpoint = getCheckPoint((GhostControl) event.getObjectA(), a);
                aRayCar = detectCar(event.getObjectB(), b);
            } else if (b.getControl(GhostControl.class) != null) {
                checkpoint = getCheckPoint((GhostControl) event.getObjectB(), b);
                aRayCar = detectCar(event.getObjectA(), a);
            } else {
                return;
            }

            if (checkpoint == null || aRayCar == null)
                return;

            RacerState racer = racers.get(aRayCar);
            if (racer.nextCheckpoint == checkpoint.num) {
                // then finally, update checkpoint
                racer.nextCheckpoint++;
                racer.nextCheckpoint = racer.nextCheckpoint % checkpoints.length;
            }
        }
        
        private RayCarControl detectCar(PhysicsCollisionObject pObject, Spatial spatial) {
            if (!(pObject instanceof RigidBodyControl)) {
                return null; //if its not a rigid body, no idea what to do
            }
            RigidBodyControl control = (RigidBodyControl)pObject;

            if (control.isKinematic() || control.getMass() == 0)
                return null; //only concerned about 'moving' collisions
            
            Collection<RayCarControl> cars = racers.keySet();
            for (RayCarControl car : cars)
                if (pObject == car.getPhysicsObject())
                    return car;
            return null;
        }

        private CheckpointClass getCheckPoint(GhostControl ghost, Spatial spatial) {
            for (CheckpointClass checkpoint : checkpoints)
                if (checkpoint.ghost == ghost)
                    return checkpoint;
            return null;
        }
    }
}
