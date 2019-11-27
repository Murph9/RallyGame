package duel;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import helper.H;

public class DuelRaceStart extends BaseAppState {

    private final IDuelFlow flow;
    private Container window;

    public DuelRaceStart(IDuelFlow flow) {
        this.flow = flow;
    }

    @SuppressWarnings("unchecked") // button checked vargs
    @Override
    protected void initialize(Application app) {
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
    }

    @Override
    protected void cleanup(Application app) {
        ((DuelApp) app).getGuiNode().detachChild(window);
    }

    @Override
    protected void onEnable() {
    }
    @Override
    protected void onDisable() {
    }
}
