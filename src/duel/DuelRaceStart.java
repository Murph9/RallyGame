package duel;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import car.CarBuilder;
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

    public DuelRaceStart(IDuelFlow flow) {
        this.flow = flow;
    }

    @SuppressWarnings("unchecked") // button checked vargs
    @Override
    protected void initialize(Application app) {
        cb = getState(CarBuilder.class);

        window = new Container();
        ((DuelApp) app).getGuiNode().attachChild(window);

        window.addChild(new Label("Race Start"), 0, 0);
        Button b = window.addChild(new Button("Go"), 1);
        b.addClickCommands((source) -> {
            flow.nextState(this, new DuelResultData());
        });
        
        DuelData data = flow.getData();
        window.addChild(new DuelCarStatsUI(app.getAssetManager(), data.yourCar, data.theirCar), 1, 0);

        Vector3f middle = H.screenCenterMe(app.getContext().getSettings(), window.getPreferredSize());
        window.setLocalTranslation(middle);

        //show the cars
        world = new StaticWorldBuilder(StaticWorld.duct); // a boring world
        getStateManager().attach(world);
        Vector3f worldStart = world.getStartPos();

        camera = new BasicCamera("Camera", app.getCamera(), worldStart.add(0, 5, 12), worldStart);
        getStateManager().attach(camera);

        cb.addCar(data.yourCar, worldStart.add(-3, 0, 0), world.getStartRot(), false);
        cb.addCar(data.theirCar, worldStart.add(3, 0, 0), world.getStartRot(), false);
    }

    @Override
    protected void cleanup(Application app) {
        ((DuelApp) app).getGuiNode().detachChild(window);

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

}
