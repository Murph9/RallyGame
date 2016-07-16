package game;

//import java.util.logging.Logger;

import com.jme3.app.SimpleApplication;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.font.BitmapFont;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.niftygui.NiftyJmeDisplay;
import com.jme3.system.AppSettings;
import com.jme3.system.NanoTimer;

import car.Car;
import car.CarData;
import de.lessvoid.nifty.Nifty;
import world.StaticWorld;
import world.Underground;
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
//  overtake as many as you can
//  like the crew
//    get away from the start
//    stay on road at speed thing


//Using eclipse: why isn't this a default?
//http://stackoverflow.com/questions/3915961/how-to-view-hierarchical-package-structure-in-eclipse-package-explorer

//TODO
//set a nifty new default style
//still need to get fog working..
//at night time or something because loading looks easier

public class Rally extends SimpleApplication {

	private Settings set;
	
	public Start start;
	public ChooseCar chooseCar;
	public ChooseMap chooseMap;
	
	public Drive drive;
	public DriveMenu menu;
	public SkyState sky;
	
	public Settings defaultSet = new Settings();
	private CarData defaultCar = Car.Runner.get();
	private StaticWorld defaultsworld = null;//StaticWorld.track2;
	private WP[] defaultdworld = Underground.values();
	
	public static void main(String[] args) {
		int fps = 60; //default is 60

		Rally app = new Rally();
		AppSettings settings = new AppSettings(true);
		settings.setResolution(1280,720); //1280,720 //1920,1080
		settings.setFrameRate(fps);
		settings.setUseJoysticks(true);
		settings.setVSync(false);

		app.setSettings(settings);
		app.setTimer(new NanoTN(fps));
		app.setShowSettings(false);
		app.setDisplayStatView(false);
		app.start();

		 //Just getting numbers for rotations
		 Quaternion q = new Quaternion();
		 q = q.fromAngleAxis(FastMath.PI, new Vector3f(0,1,0));
		 H.p(q);
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
		
		start = new Start();
		getStateManager().attach(start);
		
		sky = new SkyState(); //lighting and shadow stuff is global
		getStateManager().attach(sky);
		
		nifty.fromXml("assets/ui/nifty.xml", "start", start);
		guiViewPort.addProcessor(niftyDisplay);
		inputManager.setCursorVisible(true);
		flyCam.setEnabled(false);

		//set the default option
		defaultSet.car = defaultCar;
		defaultSet.sworld = defaultsworld;
		defaultSet.dworld = defaultdworld;
	}
	
	public void startFast() {
		//use the default option and just init straight away
		getStateManager().detach(start);
		
		if (defaultSet.car == null || (defaultSet.sworld == null && defaultSet.dworld == null)) {
			System.err.println("Defaults not set.");
		}
		
		startDrive(defaultSet.car, defaultSet.sworld, defaultSet.dworld);
		App.nifty.gotoScreen("drive-noop");
	}
	
	//HERE is the logic for the app progress.
	// its the thing you call when the app state is done
	// TODO: need a toMainMenu() at some point 
	public void next(AppState app) {
		AppStateManager state = getStateManager();
		
		if (app instanceof Start) {
			state.detach(start);
			
			startChooseCar();
			App.nifty.gotoScreen("chooseCar");
			
		} else if (app instanceof ChooseCar) {
			state.detach(chooseCar);
			
			startChooseMap();
			App.nifty.gotoScreen("chooseMapType");
			
		} else if (app instanceof ChooseMap) {
			state.detach(chooseMap);
			
			startDrive(chooseCar.getCarData(), chooseMap.getMapS(), chooseMap.getMapD());
			App.nifty.gotoScreen("drive-noop");
			
		} else if (app instanceof DriveMenu) {
			state.detach(drive); //no need to call drive.cleanup because it can do that itself
			drive = null;
			state.detach(menu);
			menu.cleanup();
			menu = null;
			
			start = new Start();
			state.attach(start);
			App.nifty.gotoScreen("start");
			
		} else {
			H.p("not done yet. - rally.next()");
		}
	}
	
	private void startChooseCar() {
		chooseCar = new ChooseCar();
		getStateManager().attach(chooseCar);
	}
	
	private void startChooseMap() {
		chooseMap = new ChooseMap();
		getStateManager().attach(chooseMap);
	}
	
	private void startDrive(CarData car, StaticWorld world, WP[] dworld) {
		if (menu != null || drive != null) return; //not sure what this is actually hoping to stop
		
		set.car = car;
		set.sworld = world;
		set.dworld = dworld;
		
		menu = new DriveMenu();
		getStateManager().attach(menu);
		
		drive = new Drive(set);
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

class NanoTN extends NanoTimer {

	private float frames;
	NanoTN(float frames) {
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
