package rallygame.service;

import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.scene.Node;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;

public class PauseState extends BaseAppState {
    
    public interface ICallback {
        void pauseState(boolean value);
        void pauseQuit();
    }

    private static final String PAUSE_ACTION = "PauseState";

    private final List<ICallback> callbacks = new LinkedList<>();
    private final Node rootNode;
    private Container pauseMenu;
    
    public PauseState() {
        this.rootNode = new Node("root DriveMenu node");
    }

    private ActionListener actionListener = new ActionListener() {
		public void onAction(String name, boolean keyPressed, float tpf) {
			if (keyPressed) return; 
			if (name.equals(PAUSE_ACTION)) {
				togglePause();
            }
        }
    };

    public void register(ICallback callback) {
        this.callbacks.add(callback);
    }
    public void deregister(ICallback callback) {
        this.callbacks.remove(callback);
    }

    @SuppressWarnings("unchecked")
	@Override
	public void initialize(Application app) {
		SimpleApplication sm = (SimpleApplication)app;
        sm.getGuiNode().attachChild(rootNode);

        app.getInputManager().addMapping(PAUSE_ACTION, new KeyTrigger(KeyInput.KEY_ESCAPE));
        app.getInputManager().addListener(actionListener, PAUSE_ACTION);
        
        // init gui
        this.pauseMenu = new Container();
        Button button = pauseMenu.addChild(new Button("Resume"));
        button.addClickCommands(new Command<Button>() {
            @Override
            public void execute(Button source) {
                togglePause();
            }
        });

        Button button2 = pauseMenu.addChild(new Button("Quit"));
        button2.addClickCommands(new Command<Button>() {
            @Override
            public void execute(Button source) {
                for (var cal: callbacks)
                    cal.pauseQuit();
                rootNode.detachChild(pauseMenu);
            }
        });
        
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }

    @Override
    public void update(float tpf) {
        Screen screen = new Screen(getApplication().getContext().getSettings());
        screen.centerMe(pauseMenu);
    }

    @Override
    public void cleanup(Application app) {
        InputManager i = app.getInputManager();
        i.deleteMapping(PAUSE_ACTION);

        i.removeListener(actionListener);

        ((SimpleApplication) app).getGuiNode().detachChild(rootNode);
    }

    public void togglePause() {
        if (callbacks == null || callbacks.isEmpty())
            return;

        if (rootNode.hasChild(pauseMenu)) {
            rootNode.detachChild(pauseMenu);
            for (var cal: callbacks)
                cal.pauseState(true);
        } else {
            rootNode.attachChild(pauseMenu);
            for (var cal: callbacks)
                cal.pauseState(false);
        }
    }
}
