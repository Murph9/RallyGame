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
import de.lessvoid.nifty.controls.DropDown;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

public class MenuState extends AbstractAppState implements ScreenController {

	private Node localRootNode = new Node("Pause Screen RootNode");
	private Node localGuiNode = new Node("Pause Screen GuiNode");
	
	public MenuState() {
		super();
	}

	private ActionListener actionListener = new ActionListener() {
		public void onAction(String name, boolean keyPressed, float tpf) {
			if (keyPressed) return; 
			if (name.equals("Pause")) {
				togglePause();
			}
			if (name.equals("TabMenu")) {
				toggleMenu();
			}
		}
	};

	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		InputManager i = App.rally.getInputManager();
		i.addMapping("Pause", new KeyTrigger(KeyInput.KEY_ESCAPE));
		i.addMapping("TabMenu", new KeyTrigger(KeyInput.KEY_TAB));
		i.addListener(actionListener, "Pause");
		i.addListener(actionListener, "TabMenu");
		
		Rally r = App.rally;
		r.getRootNode().attachChild(localRootNode);
		r.getGuiNode().attachChild(localGuiNode);
	}

	public void togglePause() {
		Screen cur = App.nifty.getCurrentScreen();
		if (cur.getScreenId().equals("paused")) {
			//then un pause
			App.nifty.gotoScreen("noop");
			App.rally.drive.setEnabled(true);
		} else {
			//then pause
			App.nifty.gotoScreen("paused");
			App.rally.drive.setEnabled(false);
		}
	}
	public void toggleMenu() {
		Screen cur = App.nifty.getCurrentScreen();
		if (cur.getScreenId().equals("pause")) return; //can't open the menu on the pause screen
		
		if (cur.getScreenId().equals("tabmenu")) {
			App.nifty.gotoScreen("noop");
		} else {
			App.nifty.gotoScreen("tabmenu");
		}
	}

	public void update(float tpf) {
		super.update(tpf);
		//useless i know..
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

	private DropDown<String> findDropDownControl(Screen screen, final String id) {
		return screen.findNiftyControl(id, DropDown.class);
	}
	
	//////////////////
	//test
	public String speed() {
		MyPhysicsVehicle car = App.rally.drive.cb.get(0);
		if (car == null) return "0";
		return car.getLinearVelocity().length()+"";
	}
}
