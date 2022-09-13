package rallygame.drive;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import rallygame.car.CarManager;
import rallygame.car.ai.RaceAI;
import rallygame.car.data.Car;
import rallygame.game.IDriveDone;
import rallygame.helper.H;
import rallygame.helper.Log;
import rallygame.service.GridPositions;
import rallygame.service.Screen;
import rallygame.service.checkpoint.CheckpointModelFactory;
import rallygame.service.checkpoint.CheckpointProgressUI;
import rallygame.service.checkpoint.StaticCheckpointProgress;
import rallygame.world.ICheckpointWorld;
import rallygame.world.wp.StaticBuilt;

public class DriveLapRace extends DriveBase {

    private CheckpointProgressUI progressMenu;
    private StaticCheckpointProgress progress;

    private RaceAI ai;

    private Container container;
    private Label timer;
    private float difficulty = 0;

    public DriveLapRace(IDriveDone done, ICheckpointWorld world) {
        super(done, Car.Runner, world);

        if (!(world instanceof StaticBuilt))
            Log.e("!! StaticBuilt world only please");
    }
    
    @Override
    public void initialize(Application app) {
        super.initialize(app);

        var w = (ICheckpointWorld)world;
        var checkpoints = w.checkpoints();

        var worldRot = new Quaternion();
        worldRot.fromAngleAxis(FastMath.DEG_TO_RAD * 90, new Vector3f(0, 1, 0));
        var worldStarts = new GridPositions(1.3f, 10).generate(checkpoints[0], 
                checkpoints[0].subtract(checkpoints[checkpoints.length - 1]).negate())
                .limit(2).toArray(Vector3f[]::new);

        var cm = getState(CarManager.class);
        cm.getPlayer().setPhysicsProperties(worldStarts[0], null, worldRot, null);
        var c = cm.addCar(Car.Runner, worldStarts[1], worldRot, false);

        // Checkpoint detection and stuff
        progress = new StaticCheckpointProgress(checkpoints, cm.getAll(), cm.getPlayer());
        progress.setCheckpointModel(CheckpointModelFactory.GetDefaultCheckpointModel(app, 10));
        getStateManager().attach(progress);

        progressMenu = new CheckpointProgressUI(progress);
        getStateManager().attach(progressMenu);

        // other ai stuff
        ai = new RaceAI(c, progress, false);
        ai.setRoadWidth(12);
        c.attachAI(ai, true);

        //show message
        this.container = new Container();
        container.addChild(new Label("The AI gets harder slowly"));
        this.timer = container.addChild(new Label("???"));
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(container);
        new Screen(getApplication().getContext().getSettings()).topLeftMe(container);
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        this.difficulty += tpf/300f;
        this.timer.setText("Difficulty: " + H.roundDecimal(this.difficulty, 2));

        ai.useCatchUp(this.difficulty);
    }

    @Override
    public void cleanup(Application app) {
        Log.p("cleaning drive lap race class");
        progress.cleanup(app);
        
        getStateManager().detach(progressMenu);
        progressMenu = null;
        getStateManager().detach(progress);
        progress = null;

        super.cleanup(app);
    }
}
