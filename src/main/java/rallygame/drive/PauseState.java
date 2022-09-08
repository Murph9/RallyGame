package rallygame.drive;

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

import rallygame.service.Screen;

public class PauseState extends BaseAppState {
    
    public interface ICallback {
        void pauseState(boolean value);
        void quit();
    }

    private static final String PAUSE_ACTION = "PauseState";

    private final ICallback callback;
    private final Node rootNode;
    private Container pauseMenu;
    
    public PauseState(ICallback callback) {
        this.callback = callback;
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
                callback.quit();
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
        if (callback == null)
            return;

        if (rootNode.hasChild(pauseMenu)) {
            rootNode.detachChild(pauseMenu);
            callback.pauseState(true);
        } else {
            rootNode.attachChild(pauseMenu);
            callback.pauseState(false);
        }
    }
}
