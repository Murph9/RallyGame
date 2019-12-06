package duel;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
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

    private CarBuilder cb;
    private IDuelFlow flow;

    private World world;
    private CarCamera camera;
    private CarUI uiNode;

    public DuelRace(IDuelFlow flow) {
        this.flow = flow;
    }

    private static final float distanceApart = 2;

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
        camera = new CarCamera("Camera", app.getCamera(), rayCar);
        getStateManager().attach(camera);
        app.getInputManager().addRawInputListener(camera);
    }

    @Override
    protected void cleanup(Application app) {
        getStateManager().detach(world);
        world = null;

        getStateManager().detach(uiNode);
        uiNode = null;

        this.cb.removeAll();
        this.cb = null;

        getStateManager().detach(camera);
        app.getInputManager().removeRawInputListener(camera);
        camera = null;
    }

    @Override
    protected void onEnable() {
    }
    @Override
    protected void onDisable() {
    }
    
    @Override
    public void update(float tpf) {
        super.update(tpf);

        if (!isEnabled())
            return;

        for (RayCarControl car: this.cb.getAll()) {
            if (car.getPhysicsLocation().z > 300) {
                DuelResultData d = new DuelResultData();
                d.raceResult = new DuelRaceResult();
                d.raceResult.playerWon = car == this.cb.get(0);
                flow.nextState(this, d);
                Log.p("Winner: " + car.getCarData().carModel);
                return;
            }
        }
    }
}
