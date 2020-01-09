package duel;

import java.util.Collection;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

import car.CarBuilder;
import car.CarCamera;
import car.CarUI;
import car.ai.DriveAlongAI;
import car.data.CarDataConst;
import car.ray.RayCarControl;
import effects.LoadModelWrapper;
import helper.Log;
import world.StaticWorld;
import world.StaticWorldBuilder;
import world.World;

public class DuelRace extends BaseAppState {

    private static final float distanceApart = 2;

    private final IDuelFlow flow;

    private CarBuilder cb;
    private DuelRaceMenu menu;

    private World world;
    private CarCamera camera;
    private CarUI uiNode;

    private float raceTimer;
    private RayCarControl winner;

    private GhostControl finishLine;

    public DuelRace(IDuelFlow flow) {
        this.flow = flow;
    }

    @Override
    protected void initialize(Application app) {
        world = new StaticWorldBuilder(StaticWorld.dragstrip); //TODO use DuelWorld
        getStateManager().attach(world);

        this.cb = getState(CarBuilder.class);

        Vector3f worldSpawn = world.getStartPos();

        DuelData data = flow.getData();

        CarDataConst yourCarData = cb.loadData(data.yourCar, data.yourAdjuster);
        RayCarControl rayCar = cb.addCar(yourCarData, worldSpawn.add(distanceApart, 0, 0), world.getStartRot(), true);

        uiNode = new CarUI(rayCar);
        getStateManager().attach(uiNode);

        CarDataConst theirCarData = cb.loadData(data.theirCar, data.theirAdjuster);
        RayCarControl car = this.cb.addCar(theirCarData, worldSpawn.add(-distanceApart, 0, 0), world.getStartRot(), false);
        car.attachAI(new DriveAlongAI(car, (vec) -> {
            return new Vector3f(-distanceApart, 0, vec.z + 20); // next pos math
        }), true);

        // initCamera
        camera = new CarCamera(app.getCamera(), rayCar);
        getStateManager().attach(camera);
        app.getInputManager().addRawInputListener(camera);

        //wait until race start
        cb.setEnabled(false);

        //init menu
        this.menu = new DuelRaceMenu(this, yourCarData, theirCarData, () -> {
            DuelResultData drd = new DuelResultData();
            drd.quitGame = true;
            flow.nextState(this, drd);
        });
        getStateManager().attach(menu);


        //test finish line collision (also maybe checkpoint collision)
        Spatial box = new Geometry("Finish line box", new Box(10, 10, 1));
        box = LoadModelWrapper.create(app.getAssetManager(), box, new ColorRGBA(0, 1, 0, 0.5f));

        finishLine = new GhostControl(CollisionShapeFactory.createBoxShape(box));
        box.setLocalTranslation(new Vector3f(0, 0, 300));
        box.addControl(finishLine);
        ((SimpleApplication)app).getRootNode().attachChild(box);
        getState(BulletAppState.class).getPhysicsSpace().add(finishLine);
    }

    @Override
    protected void cleanup(Application app) {
        getStateManager().detach(world);
        world = null;

        getStateManager().detach(uiNode);
        uiNode = null;

        this.cb.setEnabled(false);
        this.cb.removeAll();
        this.cb = null;

        getStateManager().detach(camera);
        app.getInputManager().removeRawInputListener(camera);
        camera = null;
    }

    @Override
    protected void onEnable() {
        getState(BulletAppState.class).setEnabled(true);
    }
    @Override
    protected void onDisable() {
        getState(BulletAppState.class).setEnabled(false);
    }
    
    @Override
    public void update(float tpf) {
        super.update(tpf);

        if (!isEnabled())
            return;
        if (winner != null)
            return;

        raceTimer += Math.min(tpf, 1/30f); //limit time step being large due to loading
        menu.setState(raceTimer);

        RayCarControl maybeWinner = DuelRace.detectWinner(finishLine, this.cb);
        if (maybeWinner != null) {
            this.winner = maybeWinner;
            this.cb.setEnabled(false);
            this.menu.raceStopped(this.winner == this.cb.get(0));
        }
    }

    private static RayCarControl detectWinner(GhostControl finishLine, CarBuilder cb) {
        List<PhysicsCollisionObject> objects = finishLine.getOverlappingObjects();
        // detect collider of finish line box
        if (objects.size() > 0) {
            Collection<RayCarControl> cars = cb.getAll();
            for (RayCarControl car : cars) {
                for (PhysicsCollisionObject pco : objects) {
                    if (pco == car.getPhysicsObject()) {
                        return car;
                    }
                }
            }
        }
        return null;
    }

    void startRace() {
        cb.setEnabled(true);
        raceTimer = 0;
    }

    void quit() {
        DuelResultData d = new DuelResultData();
        d.raceResult = new DuelRaceResult();
        d.raceResult.playerWon = winner == this.cb.get(0);
        d.raceResult.mills = (long) (raceTimer * 1000f);

        Log.p("Winner: " + winner.getCarData().name);
        
        cb.setEnabled(false);
        flow.nextState(this, d);
    }
}
