package duel;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

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

        window.setLocalTranslation(300, 300, 0);
        window.addChild(new Label("Race Start"), 0, 0);
        Button b = window.addChild(new Button("Go"), 1);
        b.addClickCommands((source) -> {
            flow.nextState(this, new DuelResultData());
        });
        
        Vector3f grav = new Vector3f(0, -9.81f, 0);
        DuelData data = flow.getData();
        window.addChild(new DuelCarStatsUI(app.getAssetManager(), data.yourCar, data.theirCar, grav), 1, 0);
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
