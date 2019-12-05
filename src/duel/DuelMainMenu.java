package duel;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import car.CarBuilder;
import car.CarStatsUI;
import car.data.CarDataConst;
import helper.H;

public class DuelMainMenu extends BaseAppState {

    private final IDuelFlow flow;
    private final DuelData duelData;
    private Container window;

    public DuelMainMenu(IDuelFlow flow, DuelData gameOverData) {
        this.flow = flow;
        this.duelData = gameOverData;
    }

    @SuppressWarnings("unchecked") //button checked vargs
    @Override
    protected void initialize(Application app) {
        window = new Container();
        
        window.addChild(new Label("Main Menu"), 0);

        Button b = window.addChild(new Button("Start"), 1, 0);
        b.addClickCommands((source) -> {
            DuelResultData d = new DuelResultData();
            flow.nextState(this, d);
        });
        b = window.addChild(new Button("Quit"), 1);
        b.addClickCommands((source) -> {
            DuelResultData d = new DuelResultData();
            d.quitGame = true;
            flow.nextState(this, d);
        });
        
        if (duelData != null) {
            CarBuilder cb = getState(CarBuilder.class);
            CarDataConst data = cb.loadData(duelData.yourCar, duelData.yourAdjuster);
            window.addChild(new CarStatsUI(app.getAssetManager(), data), 2, 0);
        }

        ((SimpleApplication) app).getGuiNode().attachChild(window);

        Vector3f middle = H.screenCenterMe(app.getContext().getSettings(), window.getPreferredSize());
        window.setLocalTranslation(middle);
    }

    @Override
    protected void cleanup(Application app) {
        ((SimpleApplication) app).getGuiNode().detachChild(window);
    }

    @Override
    protected void onEnable() {}
    @Override
    protected void onDisable() {}
}
