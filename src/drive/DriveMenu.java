package drive;

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
import com.simsilica.lemur.Label;

import game.App;
import helper.H;

public class DriveMenu extends BaseAppState {

	//GUI objects
	private Container pauseMenu;
	private Container infoHint;
	private Container info;
	
	//random Label to print to the screen to show the user, assumed settable by 'Drive*'
	public Label randomthing;
	private Container random;
	
	DriveBase drive;
	
	public DriveMenu(DriveBase drive) {
		super();
		this.drive = drive;
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

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Application app) {
		SimpleApplication sm = (SimpleApplication)app;

		app.getInputManager().addMapping("Pause", new KeyTrigger(KeyInput.KEY_ESCAPE));
		app.getInputManager().addMapping("TabMenu", new KeyTrigger(KeyInput.KEY_TAB));
		
		app.getInputManager().addListener(actionListener, "Pause");
		app.getInputManager().addListener(actionListener, "TabMenu");
		
		//init gui
		pauseMenu = new Container();
		Button button = pauseMenu.addChild(new Button("UnPause"));
		button.addClickCommands(new Command<Button>() {
            @Override
            public void execute( Button source ) {
            	togglePause();
            }
        });
		
		Button button2 = pauseMenu.addChild(new Button("MainMenu"));
		button2.addClickCommands(new Command<Button>() {
            @Override
            public void execute( Button source ) {
            	mainMenu();
            	sm.getGuiNode().detachChild(pauseMenu);
            }
        });
		pauseMenu.setLocalTranslation(H.screenMiddle(app.getContext().getSettings()).add(pauseMenu.getPreferredSize().mult(-0.5f)));
		
		infoHint = new Container();
		infoHint.attachChild(new Label("TAB for info"));
		infoHint.setLocalTranslation(H.screenTopLeft(app.getContext().getSettings()));
		sm.getGuiNode().attachChild(infoHint);
		
		info = new Container();
		info.attachChild(new Label("Controls: move: wasd and arrows , flip: f, handbrake: space, reverse: leftshift, camera: e,z, tab: this, pause: esc, reset: enter, jump: q, nitro: leftcontrol, telemetry: home"));
		info.setLocalTranslation(H.screenTopLeft(app.getContext().getSettings()));
		
		random = new Container();
		randomthing = new Label("");
		random.attachChild(randomthing);
		random.setLocalTranslation(H.screenTopRight(app.getContext().getSettings()).add(-100, 0, 0));
		sm.getGuiNode().attachChild(random);
	}

	public void togglePause() {
		Node guiRoot = ((SimpleApplication)getApplication()).getGuiNode();
		if (guiRoot.hasChild(pauseMenu)) {
			guiRoot.detachChild(pauseMenu);
            drive.setEnabled(true);
		} else {
			guiRoot.attachChild(pauseMenu);
			drive.setEnabled(false);
		}
	}
	public void toggleMenu() {
		Node guiRoot = ((SimpleApplication)getApplication()).getGuiNode();
		if (guiRoot.hasChild(info)) {
			guiRoot.attachChild(infoHint);
			guiRoot.detachChild(info);
		} else {
			guiRoot.attachChild(info);
			guiRoot.detachChild(infoHint);
		}
	}
	
	@Override
	protected void onEnable() {
	}
	@Override
	protected void onDisable() {
	}
	
	public void mainMenu() {
		if (drive != null)
			drive.next();
		else
			((App)getApplication()).next(null);
	}

	public void update(float tpf) {
		super.update(tpf);
	}
	
	public void cleanup(Application app) {
		InputManager i = app.getInputManager();
		i.deleteMapping("Pause");
		i.deleteMapping("TabMenu");
		
		i.removeListener(actionListener);
	}
}
