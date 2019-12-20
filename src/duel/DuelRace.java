package duel;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.Vector3f;

import car.CarBuilder;
import car.CarCamera;
import car.CarUI;
import car.ai.DriveAlongAI;
import car.data.CarDataConst;
import car.ray.RayCarControl;
import helper.Log;
import world.StaticWorld;
import world.StaticWorldBuilder;
import world.World;

public class DuelRace extends BaseAppState {

    private static final float distanceApart = 2;

    private CarBuilder cb;
    private IDuelFlow flow;

    private DuelRaceMenu menu;

    private World world;
    private CarCamera camera;
    private CarUI uiNode;

    private float raceTimer;

    private RayCarControl winner;

    public DuelRace(IDuelFlow flow) {
        this.flow = flow;
    }

    @Override
    protected void initialize(Application app) {
        world = new StaticWorldBuilder(StaticWorld.dragstrip);
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

        raceTimer += tpf;
        menu.setState(raceTimer);

        for (RayCarControl car: this.cb.getAll()) {
            if (car.getPhysicsLocation().z > 300) {
                winner = car;
                this.cb.setEnabled(false);
                menu.raceStopped(winner == this.cb.get(0));
                break;
            }
        }
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
