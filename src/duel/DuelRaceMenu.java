package duel;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.scene.Node;
import com.simsilica.lemur.Container;

import helper.H;

public class DuelRaceMenu extends BaseAppState {
    
    protected final Node rootNode;
    protected final AppState raceState;
    protected final Runnable quit;

    private Container pauseMenu;

    public DuelRaceMenu(AppState raceState, Runnable quit) {
        this.raceState = raceState;
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
        pauseMenu.setLocalTranslation(
                H.screenMiddle(app.getContext().getSettings()).add(pauseMenu.getPreferredSize().mult(-0.5f)));
    }

    @Override
    protected void cleanup(Application app) {
        InputManager i = app.getInputManager();
        i.deleteMapping("Pause");
        i.removeListener(actionListener);

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
}
