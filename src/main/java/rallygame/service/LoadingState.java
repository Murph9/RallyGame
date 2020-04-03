package rallygame.service;

import java.util.LinkedList;
import java.util.List;

import com.google.common.collect.Lists;
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

    private final List<ILoadable> loadingStates;
    private final List<AppState> toPause;
    private final List<AppState> enabledStates;

    private int loadedStates = 0;
    private Node rootNode;
    private Container loadingContainer;
    private ProgressBar progressBar;

    /**states = states that we want to wait for. toPause = states to pause */
    public LoadingState(ILoadable state, AppState... toPause) {
        this(new ILoadable[] {state}, toPause);
    }

    /**states = states that we want to wait for. toPause = states to pause */
    public LoadingState(ILoadable[] states, AppState[] toPause) {
        this.loadingStates = Lists.newArrayList(states);
        this.toPause = Lists.newArrayList(toPause);
        this.enabledStates = new LinkedList<AppState>();
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

        for (ILoadable state: loadingStates) {
            app.getStateManager().attach(state);
        }
    }

    @Override
    protected void cleanup(Application app) {
        //unpause them
        for (AppState state: enabledStates)
            state.setEnabled(true);

        SimpleApplication sm = (SimpleApplication) app;
        sm.getGuiNode().detachChild(rootNode);
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }

    @Override
    public void update(float tpf) {
        float total = 0;
        for (ILoadable state: new LinkedList<>(loadingStates)) {
            float percent = state.loadPercent();
            if (state.loadPercent() >= 1) {
                loadedStates++;
                loadingStates.remove(state);
                state.setEnabled(false);
                continue;
            }

            total += percent;
        }

        total += loadedStates;
        if (loadingStates.isEmpty()) {
            getStateManager().detach(this);
        } else
            total /= (loadedStates + loadingStates.size());

        // update the loading bar
        progressBar.setProgressValue(total);
        progressBar.setMessage("Loading... " + H.roundDecimal(total * 100, 1) + "%");
        Screen screen = new Screen(getApplication().getContext().getSettings());
        screen.centerMe(loadingContainer);
    }
}
