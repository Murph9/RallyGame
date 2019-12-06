package duel;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import car.CarBuilder;
import car.data.CarDataConst;
import game.BasicCamera;
import helper.H;
import world.StaticWorld;
import world.StaticWorldBuilder;
import world.World;

public class DuelRaceStart extends BaseAppState {

    private final IDuelFlow flow;
    
    private World world;
    private CarBuilder cb;
    private Container window;
    private BasicCamera camera;

    private static final Vector3f LookAt = new Vector3f(0, 3f, 0);
    private static final Vector3f PosOffset = new Vector3f(4, 3, 10);

    public DuelRaceStart(IDuelFlow flow) {
        this.flow = flow;
    }

    @SuppressWarnings("unchecked") // button checked vargs
    @Override
    protected void initialize(Application app) {
        cb = getState(CarBuilder.class);

        window = new Container();
        ((SimpleApplication) app).getGuiNode().attachChild(window);

        window.addChild(new Label("Race Start"), 0, 0);
        Button b = window.addChild(new Button("Go"), 1);
        b.addClickCommands((source) -> {
            flow.nextState(this, new DuelResultData());
        });
        
        DuelData data = flow.getData();
        CarDataConst data1 = cb.loadData(data.yourCar, data.yourAdjuster);
        CarDataConst data2 = cb.loadData(data.theirCar, data.theirAdjuster);
        window.addChild(new DuelCarStatsUI(app.getAssetManager(), data1, data2), 1, 0);

        Vector3f middle = H.screenTopCenterMe(app.getContext().getSettings(), window.getPreferredSize());
        window.setLocalTranslation(middle);

        //show the cars
        world = new StaticWorldBuilder(StaticWorld.duct); // a boring world
        getStateManager().attach(world);
        Vector3f worldStart = world.getStartPos();

        camera = new BasicCamera("Camera", app.getCamera(), worldStart.add(LookAt), worldStart.add(PosOffset));
        getStateManager().attach(camera);

        cb.addCar(data1, worldStart.add(-1.8f, 0, 0), world.getStartRot(), false);
        cb.addCar(data2, worldStart.add(1.8f, 0, 0), world.getStartRot(), false);
    }

    @Override
    protected void cleanup(Application app) {
        ((SimpleApplication) app).getGuiNode().detachChild(window);

        getStateManager().detach(camera);
        camera = null;

        getStateManager().detach(world);
        world = null;

        cb.removeAll();
        cb = null;
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

        Vector3f pos = cb.get(0).getPhysicsLocation().add(cb.get(1).getPhysicsLocation()).mult(1/2f);
        camera.updatePosition(pos.add(PosOffset), pos.add(LookAt));
    }
}
