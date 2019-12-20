package duel;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.HAlignment;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.style.ElementId;

import car.CarBuilder;
import car.CarStatsUI;
import car.data.CarDataConst;
import helper.Screen;

public class DuelMainMenu extends BaseAppState implements RawInputListener {

    private final IDuelFlow flow;
    private final DuelData duelData;
    private Container mainWindow;
    private Container quitWindow;

    public DuelMainMenu(IDuelFlow flow, DuelData gameOverData) {
        this.flow = flow;
        this.duelData = gameOverData;
    }

    @SuppressWarnings("unchecked") // button checked vargs
    @Override
    protected void initialize(Application app) {
        mainWindow = new Container();
        mainWindow.setBackground(DuelUiStyle.getBorderedNoBackground());

        Label l = mainWindow.addChild(new Label("Duel", new ElementId("title")));
        l = mainWindow.addChild(new Label("Press any key to start"));
        l.setTextHAlignment(HAlignment.Center);

        if (duelData != null) {
            mainWindow.addChild(new Label("Wins: " + duelData.wins));
            CarBuilder cb = getState(CarBuilder.class);
            CarDataConst data = cb.loadData(duelData.yourCar, duelData.yourAdjuster);
            mainWindow.addChild(new CarStatsUI(app.getAssetManager(), data), 2, 0);
        }

        ((SimpleApplication) app).getGuiNode().attachChild(mainWindow);
        app.getInputManager().addRawInputListener(this);

        Screen screen = new Screen(app.getContext().getSettings());
        screen.centerMe(mainWindow);

        quitWindow = new Container();
        Button b = quitWindow.addChild(new Button("Quit"));
        b.addClickCommands((source) -> {
            DuelResultData d = new DuelResultData();
            d.quitGame = true;
            flow.nextState(this, d);
        });
        ((SimpleApplication) app).getGuiNode().attachChild(quitWindow);
        screen.topCenterMe(quitWindow);
    }

    @Override
    protected void cleanup(Application app) {
        ((SimpleApplication) app).getGuiNode().detachChild(mainWindow);
        app.getInputManager().removeRawInputListener(this);
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
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
            start();
    }
    @Override
    public void onMouseMotionEvent(MouseMotionEvent evt) {}
    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt) {
        if (evt.isReleased())
            start();
    }
    @Override
    public void onKeyEvent(KeyInputEvent evt) {
        if (!evt.isRepeating() && evt.isReleased()) 
            start();
    }
    @Override
    public void onTouchEvent(TouchEvent evt) {}
    
    private void start() {
        if (isEnabled()) {
            DuelResultData d = new DuelResultData();
            flow.nextState(this, d);
        }
    }
    //#endregion
}
