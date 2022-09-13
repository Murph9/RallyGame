package duel;

import java.util.LinkedList;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.style.ElementId;

import rallygame.car.CarManager;
import rallygame.car.RotatesCarCamera;
import rallygame.car.ai.DriveAlongAI;
import rallygame.car.data.Car;
import rallygame.car.data.CarDataConst;
import rallygame.car.ray.RayCarControl;
import rallygame.car.ui.CarStatsUI;
import rallygame.service.Screen;
import rallygame.service.checkpoint.CheckpointProgress;
import rallygame.service.checkpoint.CheckpointModelFactory;
import rallygame.world.wp.DefaultBuilder;
import rallygame.world.wp.WP.DynamicType;

public class DuelMainMenu extends BaseAppState implements RawInputListener {

    private final IDuelFlow flow;
    private final DuelData duelData;
    private final String version;

    private final Car carType;
    private final DefaultBuilder world;

    private AppState camera;
    private CarManager cm;
    private CheckpointProgress progress;

    private Container mainWindow;
    private Container altWindow;
    
    private final LinkedList<Vector3f> initCheckpointBuffer;

    public DuelMainMenu(IDuelFlow flow, DuelData gameOverData, String version) {
        this.flow = flow;
        this.duelData = gameOverData;
        this.version = version;

        carType = Car.Runner;
        world = DynamicType.Valley.getBuilder();

        initCheckpointBuffer = new LinkedList<>();
    }

    @Override
    protected void initialize(Application app) {
        this.cm = getState(CarManager.class);

        initMenu((SimpleApplication) app);
        initBackground((SimpleApplication) app);
    }

    @SuppressWarnings("unchecked") // button checked vargs
    private void initMenu(SimpleApplication app) {
        
        mainWindow = new Container();
        mainWindow.setBackground(DuelUiStyle.getBorderedNoBackground());

        mainWindow.addChild(new Label("Duel", new ElementId("title")));

        if (duelData != null) {
            Button b = mainWindow.addChild(new Button("Retry"));
            b.addClickCommands((source) -> {
                startRace();
            });

            mainWindow.addChild(new Label("Wins: " + duelData.wins));
            CarDataConst data = cm.loadData(duelData.yourCar, duelData.yourAdjuster);
            mainWindow.addChild(new CarStatsUI(app.getAssetManager(), data), 2, 0);
        } else {
            Label l = mainWindow.addChild(new Label("Press any key to start"));
            l.setTextHAlignment(HAlignment.Center);

            app.getInputManager().addRawInputListener(this);
        }

        app.getGuiNode().attachChild(mainWindow);

        altWindow = new Container();
        Button b = altWindow.addChild(new Button("Controls"), 0, 0);
        Label l = new Label("move: wasd and arrows\nflip: f\nhandbrake: space\n"
                + "reverse: leftshift (hold)\npause: esc\nnitro: either control\ntelemetry: home");
        b.addClickCommands((source) -> {
            if (l.getParent() == null)
                altWindow.addChild(l, 1, 0);
            else
                altWindow.removeChild(l);
        });

        b = altWindow.addChild(new Button("Quit"), 0, 1);
        b.addClickCommands((source) -> {
            DuelResultData d = new DuelResultData();
            d.quitGame = true;
            flow.nextState(this, d);
        });

        altWindow.addChild(new Label(this.version), 0, 2);
        app.getGuiNode().attachChild(altWindow);
    }

    private void initBackground(SimpleApplication app) {
        getStateManager().attach(world);

        // build player
        RayCarControl car = cm.addCar(this.carType, world.getStart(), false);

        Vector3f[] checkpoints = new Vector3f[] {
            new Vector3f(0, 0, 0),
            new Vector3f(10, 0, 0) // starting vector to get the cars to drive forward
        };

        // init checkpoints
        progress = new CheckpointProgress(world, checkpoints, cm.getAll(), cm.getPlayer());
        progress.setCheckpointModel(CheckpointModelFactory.GetDefaultCheckpointModel(app, 0, new ColorRGBA(0, 1, 0, 0.4f)));
        getStateManager().attach(progress);
        

        // attach basic ai, for the view
        DriveAlongAI ai = new DriveAlongAI(car, (vec) -> world.getNextPieceClosestTo(vec));
        ai.setMaxSpeed(27.7778f); // 27.7 = 100 km/h
        car.attachAI(ai, true);

        this.camera = new RotatesCarCamera(app.getCamera(), car);
        getStateManager().attach(this.camera);

        this.cm.setEnabled(true);
        getState(BulletAppState.class).setEnabled(true);
    }

    @Override
    protected void cleanup(Application app) {
        getStateManager().detach(progress);
        progress = null;

        Node n = ((SimpleApplication) app).getGuiNode();
        n.detachChild(mainWindow);
        n.detachChild(altWindow);
        app.getInputManager().removeRawInputListener(this);

        getStateManager().detach(world);
        getState(BulletAppState.class).setEnabled(false);

        getStateManager().detach(camera);
        camera = null;

        cm.removeAll();
        cm.setEnabled(false);
        cm = null;
    }

    @Override
    protected void onEnable() { }
    @Override
    protected void onDisable() { }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        Screen screen = new Screen(getApplication().getContext().getSettings());
        screen.centerMe(mainWindow);
        screen.topCenterMe(altWindow);

        //add any buffered checkpoints
        if (progress.isInitialized() && !initCheckpointBuffer.isEmpty()) {
            for (Vector3f p : initCheckpointBuffer)
                progress.addCheckpoint(p);
            initCheckpointBuffer.clear();
        }
    }

    //#region input events
    @Override
    public void beginInput() {}
    @Override
    public void endInput() {}
    @Override
    public void onJoyAxisEvent(JoyAxisEvent evt) {}
    @Override
    public void onJoyButtonEvent(JoyButtonEvent evt) {
        if (evt.isPressed())
            startRace();
    }
    @Override
    public void onMouseMotionEvent(MouseMotionEvent evt) {}
    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt) {
        if (evt.isReleased())
            startRace();
    }
    @Override
    public void onKeyEvent(KeyInputEvent evt) {
        if (!evt.isRepeating() && evt.isReleased()) 
            startRace();
    }
    @Override
    public void onTouchEvent(TouchEvent evt) {}
    //#endregion

    private void startRace() {
        if (isEnabled()) {
            DuelResultData d = new DuelResultData();
            flow.nextState(this, d);
            this.setEnabled(false); //to prevent this being called twice
        }
    }
}
