package duel;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import car.CarBuilder;

public class DuelRaceEnd extends BaseAppState {

    private final IDuelFlow flow;
    private CarBuilder cb;
    private Container window;

    public DuelRaceEnd(IDuelFlow flow) {
        this.flow = flow;
    }

    @SuppressWarnings("unchecked") // button checked vargs
    @Override
    protected void initialize(Application app) {
        cb = getState(CarBuilder.class);

        window = new Container();

        window.setLocalTranslation(300, 300, 0);
        window.addChild(new Label("Race End"));
        Button b = window.addChild(new Button("Go"));
        b.addClickCommands((source) -> {
            flow.nextState(this, false);
        });

        // TODO show car(s) and stats from IDuelFlow
        ((DuelApp) app).getGuiNode().attachChild(window);
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
