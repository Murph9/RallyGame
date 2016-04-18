package game;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.scene.Node;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

public class MenuState extends AbstractAppState implements ScreenController, ActionListener {

	private Node localRootNode = new Node("Start Screen RootNode");
	private Node localGuiNode = new Node("Start Screen GuiNode");

	private Nifty nifty;
	private boolean paused;

	public MenuState(Nifty nifty) {
		this.nifty = nifty;

		InputManager i = App.rally.getInputManager();
		i.addMapping("Pause", new KeyTrigger(KeyInput.KEY_7));
		i.addListener(actionListener, "Pause");
	}

	private ActionListener actionListener = new ActionListener() {
		public void onAction(String name, boolean keyPressed, float tpf) {
			if (name.equals("Pause") && !keyPressed) {
				togglePause();
			}
		}
	};

	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		Rally r = App.rally;
		r.getRootNode().attachChild(localRootNode);
		r.getGuiNode().attachChild(localGuiNode);
	}

	public void togglePause() {
		paused = App.rally.togglePause();
		if (paused) {
			nifty.gotoScreen("paused");
		} else {
			nifty.gotoScreen("start");			
		}
	}

	@Override
	public void cleanup() {
		Rally r = App.rally;
		r.getRootNode().detachChild(localRootNode);
		r.getGuiNode().detachChild(localGuiNode);
	}

	public void bind(Nifty arg0, Screen arg1) { }
	public void onEndScreen() { }
	public void onStartScreen() { }

	@Override
	public void onAction(String arg0, boolean arg1, float arg2) {
		H.p(arg0);
	}
}
