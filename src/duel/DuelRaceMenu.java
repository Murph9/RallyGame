package duel;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.scene.Node;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.style.ElementId;

import car.data.CarDataConst;
import helper.H;
import helper.Screen;

public class DuelRaceMenu extends BaseAppState {
    
    protected final Node rootNode;
    protected final DuelRace raceState;
    protected final Runnable quit;

    private final CarDataConst your;
    private final CarDataConst their;

    private Container pauseMenu;
    private Container startWindow;
    private Container endWindow;

    private Container currentStateWindow;
    private Label currentTime;

    public DuelRaceMenu(DuelRace raceState, CarDataConst your, CarDataConst their, Runnable quit) {
        this.raceState = raceState;
        this.your = your;
        this.their = their;
        this.rootNode = new Node("Root Duel node");
        this.quit = quit;
    }

    @Override
    protected void initialize(Application app) {
        SimpleApplication sm = (SimpleApplication) app;
        sm.getGuiNode().attachChild(rootNode);
        
        app.getInputManager().addMapping("Pause", new KeyTrigger(KeyInput.KEY_ESCAPE));
        app.getInputManager().addListener(actionListener, "Pause");

        pauseMenu = DuelUiElements.pauseMenu(() -> { togglePause(); }, () -> { quit.run(); });
        new Screen(app.getContext().getSettings()).centerMe(pauseMenu);
        
        //init the timer at the top
        currentStateWindow = new Container();
        currentStateWindow.addChild(new Label("Times"));
        currentTime = currentStateWindow.addChild(new Label("0.00sec"), 1);

        new Screen(getApplication().getContext().getSettings()).topCenterMe(currentStateWindow);

        //init starting menu
        loadStartUi();
    }

    @Override
    protected void cleanup(Application app) {
        InputManager i = app.getInputManager();
        i.deleteMapping("Pause");
        i.removeListener(actionListener);

        ((SimpleApplication) app).getGuiNode().detachChild(currentStateWindow);
        ((SimpleApplication) app).getGuiNode().detachChild(rootNode);
    }

    @Override
    protected void onDisable() { }
    @Override
    protected void onEnable() { }

    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean keyPressed, float tpf) {
            if (keyPressed)
                return;
            if (name.equals("Pause")) {
                togglePause();
            }
        }
    };

    private void togglePause() {
        if (rootNode.hasChild(pauseMenu)) {
            rootNode.detachChild(pauseMenu);
            raceState.setEnabled(true);
        } else {
            rootNode.attachChild(pauseMenu);
            raceState.setEnabled(false);
        }
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
            Label l = endWindow.addChild(new Label("Winner", new ElementId("title")));
            l.setTextHAlignment(HAlignment.Center);
        } else {
            Label l = endWindow.addChild(new Label("Loser", new ElementId("titleAlt")));
            l.setTextHAlignment(HAlignment.Center);
        }

        Button b = endWindow.addChild(new Button("Close"));
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
}
