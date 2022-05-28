package survival;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.style.ElementId;

import rallygame.car.data.CarDataAdjuster;
import rallygame.car.data.CarDataAdjustment;
import rallygame.helper.H;
import rallygame.service.Screen;
import rallygame.service.checkpoint.CheckpointProgress;
import survival.upgrade.UpgradeType;

/*
Drive next x km to next checkpoint
main problem is Fuel
Drag gets worse as you drive (requiring more fuel)
- time to next checkpoint might decrease slowly

upgrade options are related to
- improving general performance
- fuel economy
- buying fuel
- buying new car type (which adds new fuel?)

After every 10x kms there are races which you must win, ie these are the boss milestones
or maybe just a mandatory fuel sacrifice
*/

public class RoadCheckpointGameManager extends BaseAppState {

    private static final int CHECK_COUNT = 5;//25;
    private static final float CHECK_TIME = 60;

    private final Drive drive;
    private final CheckpointProgress progress;

    private int currentCheckTarget;
    private float time;

    private Container currentStateWindow;
    private Label currentTime;
    private Label currentPercent;

    public RoadCheckpointGameManager(Drive drive, CheckpointProgress progress) {
        this.drive = drive;
        this.progress = progress;
    }

    @Override
    protected void initialize(Application app) {
        time = CHECK_TIME;
        currentCheckTarget = CHECK_COUNT;

        // init the timer at the top
        currentStateWindow = new Container();
        currentStateWindow.addChild(new Label("Time: "));
        currentTime = currentStateWindow.addChild(new Label("0.00sec"), 1);
        currentStateWindow.addChild(new Label("Progress: "));
        currentPercent = currentStateWindow.addChild(new Label("0%"), 1);

        ((SimpleApplication) getApplication()).getGuiNode().attachChild(currentStateWindow);
        new Screen(getApplication().getContext().getSettings()).topCenterMe(currentStateWindow);
    }

    @Override
    protected void cleanup(Application app) {
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        if (drive.isEnabled())
            time -= tpf;
        currentTime.setText(H.roundDecimal(time, 2)+"sec");

        var checkCount = progress.getPlayerRacerState().lastCheckpoint.num;
        currentPercent.setText(H.roundDecimal(calcCheckPointProgress(checkCount),2)+"%");

        if (checkCount == currentCheckTarget) {
            currentCheckTarget += CHECK_COUNT;
            time = CHECK_TIME;
            drive.setEnabled(false);
            openOptions();
        }

        if (time < 0) {
            // you lose :(
        }
    }

    @Override
    protected void onEnable() {
    }
    @Override
    protected void onDisable() {
    }

    @SuppressWarnings("unchecked") // button checked vargs
    private void openOptions() {
        var dialog = new Container();
        
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(dialog);
        
        Label l = dialog.addChild(new Label("Choose an upgrade", new ElementId("titleAlt")));
        l.setTextHAlignment(HAlignment.Center);

        var b1 = dialog.addChild(new Button("Improve Grip by 15%"));
        b1.setTextHAlignment(HAlignment.Center);
        var b2 = dialog.addChild(new Button("Improve Power by 15%"));
        b2.setTextHAlignment(HAlignment.Center);

        b1.addClickCommands((source) -> {
            closeOptions(dialog, "Grip");
        });

        b2.addClickCommands((source) -> {
            closeOptions(dialog, "Power");
        });

        new Screen(getApplication().getContext().getSettings()).centerMe(dialog);
    }

    
    private void closeOptions(Container dialog, String selected) {
        ((SimpleApplication) getApplication()).getGuiNode().detachChild(dialog);
        UpgradeType type = null;
        switch (selected) {
            case "Grip":
                type = UpgradeType.ImproveEngine;//UpgradeType.ImproveGrip;
                break;
            case "Power":
                type = UpgradeType.ImproveEngine;
                break;
            default:
                break;
        }

        drive.applyChange(new CarDataAdjuster(CarDataAdjustment.asFunc(type.carFunc)));
        drive.setEnabled(true);
    }

    private float calcCheckPointProgress(int currentCheck) {
        var fraction = (currentCheckTarget-currentCheck) % CHECK_COUNT;

        var result = (1-((float)fraction)/CHECK_COUNT)*100;

        if (result > 99.9f)
            return 0; // no 100%'s please
        return result;
    }
}
