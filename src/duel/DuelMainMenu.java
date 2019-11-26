package duel;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

public class DuelMainMenu extends BaseAppState {

    private final IDuelFlow flow;
    private Container window;
    public DuelMainMenu(IDuelFlow flow) {
        this.flow = flow;
    }

    @SuppressWarnings("unchecked") //button checked vargs
    @Override
    protected void initialize(Application app) {
        window = new Container();
        
        window.setLocalTranslation(300, 300, 0); //TODO centre
        window.addChild(new Label("Main Menu"));

        Button b = window.addChild(new Button("Start"));
        b.addClickCommands((source) -> {
            DuelResultData d = new DuelResultData();
            flow.nextState(this, d);
        });
        b = window.addChild(new Button("Quit"));
        b.addClickCommands((source) -> {
            DuelResultData d = new DuelResultData();
            d.quitGame = true;
            flow.nextState(this, d);
        });
        
        ((DuelApp) app).getGuiNode().attachChild(window);
    }

    @Override
    protected void cleanup(Application app) {
        ((DuelApp) app).getGuiNode().detachChild(window);
    }

    @Override
    protected void onEnable() {}
    @Override
    protected void onDisable() {}
}
