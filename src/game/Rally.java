package game;

import java.util.logging.Logger;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.font.BitmapFont;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.system.AppSettings;
import com.jme3.system.NanoTimer;

import de.lessvoid.nifty.Nifty;
import world.StaticWorld;

////TODO Ideas for game modes:
//being chased. (with them spawning all lightning sci-fi like?)
//time based
//score based (closeness to them)
//touch all of them in one run
//or get them all the same colour (like the pads in mario galaxy)
//get them to fall in the hole
//the infinite road thing
//at night time or something because loadings easier
//follow points for being close

public class Rally extends SimpleApplication {

	public StartState start;
	public ChooseCarAppState chooseCar;
	public ChooseMapAppState chooseMap;
	
	public DriveState drive;
	public MenuState menu;
	
	
	public static void main(String[] args) {
		int fps = 60; //default is 60

		Rally app = new Rally();
		AppSettings settings = new AppSettings(true);
		settings.setResolution(1280,720); //1280,720 //1920,1080
		settings.setFrameRate(fps);
		settings.setUseJoysticks(true);
		settings.setVSync(false);

		app.setSettings(settings);
		app.setTimer(new NanoT60(fps));
		app.setShowSettings(false);
		app.setDisplayStatView(false);
		app.start();

	}

	@Override
	public void simpleInitApp() {
		App.rally = this;
		
//		Logger.getLogger("").setLevel(Level.WARNING); //remove warnings here
		inputManager.deleteMapping(SimpleApplication.INPUT_MAPPING_EXIT);
		
		NiftyJmeDisplay niftyDisplay = new NiftyJmeDisplay(assetManager, inputManager, audioRenderer, guiViewPort);
		Nifty nifty = niftyDisplay.getNifty();
		App.nifty = nifty;
		try { //check if its valid, very important
			nifty.validateXml("assets/ui/nifty.xml");
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		start = new StartState();
		getStateManager().attach(start);
		
		nifty.fromXml("assets/ui/nifty.xml", "start", start);
		guiViewPort.addProcessor(niftyDisplay);
		inputManager.setCursorVisible(true);
		flyCam.setEnabled(false);
	}
	
	//the thing you call when the app state is done
	public void next(AppState app) {
		//maybe remove all the app states?
		
		
		if (app instanceof StartState) {
			startChooseCar();
			App.nifty.gotoScreen("chooseCar");
			
		} else if (app instanceof ChooseCarAppState) {
			getStateManager().detach(chooseCar);
			
			startChooseMap();
			App.nifty.gotoScreen("chooseMap");
			
		} else if (app instanceof ChooseMapAppState) {
			getStateManager().detach(chooseMap);
			
			startDrive(chooseCar.getCarData(), chooseMap.getMap());
			App.nifty.gotoScreen("noop");
			
		} else {
			H.p("not done yet. - rally.next()");
		}
	}
	
	
	
	private void startChooseCar() {
		getStateManager().detach(start);
		
		chooseCar = new ChooseCarAppState();
		getStateManager().attach(chooseCar);
	}
	
	private void startChooseMap() {
		chooseMap = new ChooseMapAppState();
		getStateManager().attach(chooseMap);
	}
	
	private void startDrive(CarData car, StaticWorld world) {
		if (menu != null || drive != null) return; //no sure what this is actually hoping to stop
		
		menu = new MenuState();
		getStateManager().attach(menu);
		
		drive = new DriveState(car, world);
		getStateManager().attach(drive);
	}

	/////////////////////
	public BitmapFont getFont() {
		return guiFont;
	}
	public AppSettings getSettings() {
		return settings;
	}

	@Override
	public void simpleUpdate(float tpf) {
		super.simpleUpdate(tpf);
		stateManager.update(tpf);
	}
	

	/////////////// menu
	public com.jme3.system.Timer getTimer() {
		return timer;
	}

}

class NanoT60 extends NanoTimer {

	private float frames;
	NanoT60(float frames) {
		super();
		this.frames = frames;
	}

	public void setFrames(float frames) {
		//for setting slow mo or something..
	}

	@Override
	public float getTimePerFrame() {
		//return tpf;
		return 1f/frames; //frame time for 60fps
	}
}
