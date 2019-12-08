package duel;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import car.CarBuilder;
import car.CarCamera;
import car.CarUI;
import car.ai.DriveAlongAI;
import car.data.CarDataConst;
import car.ray.RayCarControl;
import helper.H;
import helper.Log;
import world.StaticWorld;
import world.StaticWorldBuilder;
import world.World;

public class DuelRace extends BaseAppState {

    private static final float distanceApart = 2;

    private CarBuilder cb;
    private IDuelFlow flow;

    private World world;
    private CarCamera camera;
    private CarUI uiNode;

    private Container currentStateWindow;
    private Label currentTime;

    private Container startWindow;
    private Container endWindow;
    
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

        loadRaceUI();
        loadStartUi(flow.getData());

        // initCamera
        camera = new CarCamera("Camera", app.getCamera(), rayCar);
        getStateManager().attach(camera);
        app.getInputManager().addRawInputListener(camera);

        //wait until race start
        cb.setEnabled(false);
    }

    private void loadRaceUI() {
        currentStateWindow = new Container();
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(currentStateWindow);

        currentStateWindow.addChild(new Label("Times"));
        currentTime = currentStateWindow.addChild(new Label("0"), 1);

        Vector3f middle = H.screenTopCenterMe(getApplication().getContext().getSettings(),
                currentStateWindow.getPreferredSize());
        currentStateWindow.setLocalTranslation(middle);
    }

    @SuppressWarnings("unchecked") // button checked vargs
    private void loadStartUi(DuelData data) {
        startWindow = new Container();
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(startWindow);

        startWindow.addChild(new Label("Race Start"), 0, 0);
        Button b = startWindow.addChild(new Button("Go"), 1);
        b.addClickCommands((source) -> {
            cb.setEnabled(true);
            raceTimer = 0;
            ((SimpleApplication) getApplication()).getGuiNode().detachChild(startWindow);
        });

        CarDataConst data1 = cb.loadData(data.theirCar, data.theirAdjuster);
        CarDataConst data2 = cb.loadData(data.theirCar, data.theirAdjuster);
        startWindow.addChild(new DuelCarStatsUI(getApplication().getAssetManager(), data1, data2), 1, 0);

        Vector3f middle = H.screenTopCenterMe(getApplication().getContext().getSettings(), startWindow.getPreferredSize());
        startWindow.setLocalTranslation(middle);
    }

    @SuppressWarnings("unchecked") // button checked vargs
    private void loadEndUi(DuelData data, long time) {
        endWindow = new Container();
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(endWindow);

        endWindow.addChild(new Label("Race end"), 0, 0);
        Button b = endWindow.addChild(new Button("Close"), 1);
        b.addClickCommands((source) -> {
            cb.setEnabled(false);
            ((SimpleApplication) getApplication()).getGuiNode().detachChild(endWindow);
            
            DuelResultData d = new DuelResultData();
            d.raceResult = new DuelRaceResult();
            d.raceResult.playerWon = winner == this.cb.get(0);
            d.raceResult.mills = (long) (raceTimer * 1000f);
            
            Log.p("Winner: " + winner.getCarData().name);
            flow.nextState(this, d);
        });

        CarDataConst data1 = cb.loadData(data.theirCar, data.theirAdjuster);
        CarDataConst data2 = cb.loadData(data.theirCar, data.theirAdjuster);
        endWindow.addChild(new DuelCarStatsUI(getApplication().getAssetManager(), data1, data2), 1, 0);

        Vector3f middle = H.screenTopCenterMe(getApplication().getContext().getSettings(),
                startWindow.getPreferredSize());
        endWindow.setLocalTranslation(middle);
    }

    @Override
    protected void cleanup(Application app) {
        ((SimpleApplication) app).getGuiNode().detachChild(currentStateWindow);
        if (startWindow != null)
            ((SimpleApplication) app).getGuiNode().detachChild(startWindow);
        if (endWindow != null)
            ((SimpleApplication) app).getGuiNode().detachChild(endWindow);

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
        if (winner != null)
            return;

        raceTimer += tpf;

        currentTime.setText(H.roundDecimal(raceTimer, 2) + "sec");

        for (RayCarControl car: this.cb.getAll()) {
            if (car.getPhysicsLocation().z > 300) {
                loadEndUi(flow.getData(), (long)(raceTimer*1000f));
                winner = car;
            }
        }
    }
}
