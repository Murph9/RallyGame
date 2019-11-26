package duel;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;

import car.CarBuilder;
import car.CarCamera;
import car.ai.DriveAlongAI;
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

    public DuelRace(IDuelFlow flow) {
        this.flow = flow;
    }

    @Override
    protected void initialize(Application app) {
        world = new StaticWorldBuilder(StaticWorld.dragstrip);
        getStateManager().attach(world);

        this.cb = getState(CarBuilder.class);
        RayCarControl rayCar = cb.addCar(flow.getData().yourCar, world.getStartPos(), world.getStartRot(), true, null);
        //TODO car ui

        RayCarControl car = this.cb.addCar(flow.getData().theirCar, world.getStartPos(), world.getStartRot(), false, null);
        car.attachAI(new DriveAlongAI(car, (vec) -> {
            return new Vector3f(0, 0, vec.z + 20); // next pos math
        }), true);

        // initCamera
        camera = new CarCamera("Camera", app.getCamera(), rayCar);
        getStateManager().attach(camera);
        app.getInputManager().addRawInputListener(camera);
    }

    @Override
    protected void cleanup(Application app) {
        this.cb.removeAll();
        this.cb = null;

        getStateManager().detach(world);
        world = null;
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
