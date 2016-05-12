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
import world.WP;

////TODO Ideas for game modes:
//being chased. (with them spawning all lightning sci-fi like?)
//  time based
//  score based (closeness to them)
//  touch all of them in one run
//    or get them all the same colour (like the pads in mario galaxy)
//  get them to fall in a hole
//  follow points for being close
//the infinite road thing
//  at night time or something because loadings easier
//  overtake as many as you can

//Using eclipse: why isn't this a default?
//http://stackoverflow.com/questions/3915961/how-to-view-hierarchical-package-structure-in-eclipse-package-explorer

public class Rally extends SimpleApplication {

	private Settings set;
	
	public StartState start;
	public ChooseCar chooseCar;
	public ChooseMap chooseMap;
	
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

		 //Just getting numbers for rotations
//		 Quaternion q = new Quaternion();
//		 q = q.fromAngleAxis(-3*FastMath.HALF_PI/2, new Vector3f(0,1,0));
//		 H.p(q);
	}

	@Override
	public void simpleInitApp() {
		this.set = new Settings();
		set.car = null; //get rid of warning
		App.rally = this;
		
//		Logger.getLogger("").setLevel(Level.WARNING); //remove warnings here
		inputManager.deleteMapping(SimpleApplication.INPUT_MAPPING_EXIT);
		
		NiftyJmeDisplay niftyDisplay = new NiftyJmeDisplay(assetManager, inputManager, audioRenderer, guiViewPort);
		Nifty nifty = niftyDisplay.getNifty();
		App.nifty = nifty;
		try { //check if its valid, very important (why doesn't it do this by itself?)
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
	
	//HERE is the logic for the app progress., TODO: need a toMainMenu() at some point
	//the thing you call when the app state is done
	public void next(AppState app) {
		if (app instanceof StartState) {
			startChooseCar();
			App.nifty.gotoScreen("chooseCar");
			
		} else if (app instanceof ChooseCar) {
			getStateManager().detach(chooseCar);
			
			startChooseMap();
			App.nifty.gotoScreen("chooseMapType");
			
		} else if (app instanceof ChooseMap) {
			getStateManager().detach(chooseMap);
			
			startDrive(chooseCar.getCarData(), chooseMap.getMapS(), chooseMap.getMapD());
			App.nifty.gotoScreen("drive-noop");
			
		} else {
			H.p("not done yet. - rally.next()");
		}
	}
	
	private void startChooseCar() {
		getStateManager().detach(start);
		
		chooseCar = new ChooseCar();
		getStateManager().attach(chooseCar);
	}
	
	private void startChooseMap() {
		getStateManager().detach(chooseCar);
		
		chooseMap = new ChooseMap();
		getStateManager().attach(chooseMap);
	}
	
	private void startDrive(CarData car, StaticWorld world, WP[] dworld) {
		if (menu != null || drive != null) return; //no sure what this is actually hoping to stop
		
		set.car = car;
		set.sworld = world;
		set.dworld = dworld;
		
		menu = new MenuState();
		getStateManager().attach(menu);
		
		drive = new DriveState(set);
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
