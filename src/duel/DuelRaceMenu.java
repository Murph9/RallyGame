package duel;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.style.ElementId;

import car.data.CarDataConst;
import drive.PauseState;
import helper.H;
import service.Screen;

public class DuelRaceMenu extends BaseAppState implements PauseState.ICallback {
    
    protected final DuelRace raceState;
    protected final Runnable quit;

    private final CarDataConst your;
    private final CarDataConst their;

    private PauseState pauseState;

    private Container startWindow;
    private Container endWindow;

    private Container currentStateWindow;
    private Label currentTime;

    public DuelRaceMenu(DuelRace raceState, CarDataConst your, CarDataConst their, Runnable quit) {
        this.raceState = raceState;
        this.your = your;
        this.their = their;
        this.quit = quit;
    }

    @Override
    protected void initialize(Application app) {
        pauseState = new PauseState(this);
        getStateManager().attach(pauseState);
        
        //init the timer at the top
        currentStateWindow = new Container();
        currentStateWindow.addChild(new Label("Time: "));
        currentTime = currentStateWindow.addChild(new Label("0.00sec"), 1);

        new Screen(getApplication().getContext().getSettings()).topCenterMe(currentStateWindow);

        //init starting menu
        loadStartUi();
    }

    @SuppressWarnings("unchecked") // button checked vargs
    private void loadStartUi() {
        startWindow = new Container();
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(startWindow);

        Label l = startWindow.addChild(new Label("Race Start", new ElementId("title")));
        l.setTextHAlignment(HAlignment.Center);

        Button b = startWindow.addChild(new Button("Go"));
        b.setTextHAlignment(HAlignment.Center);
        b.addClickCommands((source) -> {
            raceState.startRace();
            ((SimpleApplication) getApplication()).getGuiNode().detachChild(startWindow);
            ((SimpleApplication) getApplication()).getGuiNode().attachChild(currentStateWindow);
        });

        startWindow.addChild(DuelUiElements.DuelCarStats(getApplication().getAssetManager(), your, their));

        new Screen(getApplication().getContext().getSettings()).topCenterMe(startWindow);
    }

    void raceStopped(boolean playerWon) {
        ((SimpleApplication) getApplication()).getGuiNode().detachChild(currentStateWindow);
        loadEndUi(playerWon);
    }

    @SuppressWarnings("unchecked") // button checked vargs
    private void loadEndUi(boolean playerWon) {
        endWindow = new Container();
        ((SimpleApplication) getApplication()).getGuiNode().attachChild(endWindow);

        if (playerWon) {
            Label l = endWindow.addChild(new Label("Winner @ " + this.currentTime.getText(), new ElementId("title")));
            l.setTextHAlignment(HAlignment.Center);
        } else {
            Label l = endWindow.addChild(new Label("Loser", new ElementId("titleAlt")));
            l.setTextHAlignment(HAlignment.Center);
        }

        Button b;
        if (playerWon) {
            b = endWindow.addChild(new Button("Next Race"));
        } else {
            b = endWindow.addChild(new Button("Close"));
        }
        b.setTextHAlignment(HAlignment.Center);
        b.addClickCommands((source) -> {
            ((SimpleApplication) getApplication()).getGuiNode().detachChild(endWindow);
            raceState.quit();
        });

        endWindow.addChild(DuelUiElements.DuelCarStats(getApplication().getAssetManager(), your, their));
        new Screen(getApplication().getContext().getSettings()).topCenterMe(endWindow);
    }

	public void setState(float raceTimer) {
        if (currentTime != null) //if statement required because this is initialised one frame after the race object
            currentTime.setText(H.roundDecimal(raceTimer, 2) + "sec");
	}

    @Override
    public void pauseState(boolean value) {
        raceState.setEnabled(value);
    }

    @Override
    public void quit() {
        quit.run();
    }

    @Override
    protected void cleanup(Application app) {
        getStateManager().detach(pauseState);
        pauseState = null;

        ((SimpleApplication) app).getGuiNode().detachChild(currentStateWindow);
    }

    @Override
    protected void onDisable() {
    }

    @Override
    protected void onEnable() {
    }

    @Override
    public void update(float tpf) {
        super.update(tpf);

        Screen screen = new Screen(getApplication().getContext().getSettings());
        if (startWindow != null)
            screen.centerMe(startWindow);
        if (endWindow != null)
            screen.centerMe(endWindow);
    }
}
