package rallygame.service;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.app.state.BaseAppState;
import com.jme3.scene.Node;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.DefaultRangedValueModel;
import com.simsilica.lemur.ProgressBar;
import com.simsilica.lemur.RangedValueModel;

import rallygame.helper.H;

public class LoadingState extends BaseAppState {

    private final ILoadable[] loadingStates;
    private final AppState[] toPause;
    private final List<AppState> enabledStates;

    private Consumer<ILoadable[]> callback;
    private AppState[] loadAfter;
    private Node rootNode;
    private Container loadingContainer;
    private ProgressBar progressBar;

    /**states = states that we want to wait for. toPause = states to pause */
    public LoadingState(ILoadable state, AppState... toPause) {
        this(new ILoadable[] {state}, toPause);
    }

    /**states = states that we want to wait for. toPause = states to pause */
    public LoadingState(ILoadable[] states, AppState[] toPause) {
        if (states == null || states.length < 1)
            throw new IllegalArgumentException("Please give me an actual state to load on.");
        if (toPause == null)
            toPause = new AppState[0];

        this.loadingStates = states;
        this.toPause = toPause;
        this.enabledStates = new LinkedList<AppState>();
    }
    
    public void setAfterLoad(AppState... states) {
        loadAfter = states;
    }
    public void setCallback(Consumer<ILoadable[]> callback) {
        this.callback = callback;
    }

    @Override
    protected void initialize(Application app) {
        this.rootNode = new Node("laoding state root node");
        SimpleApplication sm = (SimpleApplication) app;
        sm.getGuiNode().attachChild(rootNode);

        loadingContainer = new Container();
        RangedValueModel model = new DefaultRangedValueModel(0, 1, 0);
        progressBar = loadingContainer.addChild(new ProgressBar(model));
        progressBar.setMessage("Loading...");
        rootNode.attachChild(loadingContainer);

        //Pause all appstates and show loading
        for (AppState state: toPause)
            if (state.isEnabled()) {
                state.setEnabled(false);
                enabledStates.add(state);
            }
        
        //make sure the states we are waiting on are added and will load
        for (AppState state: loadingStates) {
            getStateManager().attach(state);
            state.setEnabled(true);
        }
    }

    @Override
    protected void cleanup(Application app) {
        //unpause them all
        for (AppState state: enabledStates)
            state.setEnabled(true);
        for (AppState state: loadingStates)
            state.setEnabled(true);

        SimpleApplication sm = (SimpleApplication) app;
        sm.getGuiNode().detachChild(rootNode);

        //init any load after states
        if (loadAfter != null)
            for (AppState state: loadAfter)
                app.getStateManager().attach(state);

        if (callback != null)
            callback.accept(loadingStates);
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }

    @Override
    public void update(float tpf) {
        if (H.allTrue((state) -> state.loadPercent() >= 1, loadingStates)) {
            getStateManager().detach(this);
            return;
        }
        
        float total = 0;
        for (ILoadable state: loadingStates) {
            float percent = state.loadPercent();
            if (state.loadPercent() >= 1) {
                state.setEnabled(false);
            }
            total += percent;
        }

        total /= loadingStates.length;

        // update the loading bar
        progressBar.setProgressValue(total);
        progressBar.setMessage("Loading... " + H.roundDecimal(total * 100, 1) + "%");
        Screen screen = new Screen(getApplication().getContext().getSettings());
        screen.centerMe(loadingContainer);
    }
}
