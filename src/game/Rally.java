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
import settings.Configuration;
import world.StaticWorld;
import world.StaticWorldBuilder;
import world.World;
import world.wp.WP.DynamicType;

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
//stop the car sound on the menus

public class Rally extends SimpleApplication {

	public Start start;
	public ChooseCar chooseCar;
	public ChooseMap chooseMap;
	
	public Drive drive;
	public DriveMenu menu;
	public SkyState sky;
	
	private final CarData defaultCar = Car.Runner.get();
	private final StaticWorld defaultsworld = null;//StaticWorld.track2;
	private final DynamicType defaultdworld = DynamicType.Valley;
	
	private CarData car;
	private StaticWorld sworld;
	private DynamicType dworld;
	
	public static void main(String[] args) {
		Configuration config = Configuration.Read();
		
		Rally app = new Rally();
		AppSettings settings = new AppSettings(true);
		settings.setResolution(config.getWidth(),config.getHeight());
		settings.setFrameRate(config.getFrameRate());
		settings.setUseJoysticks(true);
		settings.setTitle(config.getTitle());
		settings.setVSync(config.ifVsnyc());

		app.setSettings(settings);
		app.setTimer(new NanoTN(config.getFrameRate()));
		app.setShowSettings(false);
		app.setDisplayStatView(false);
		app.start();

		
		//Just getting numbers for rotations
		Quaternion q = new Quaternion();
		q = q.fromAngleAxis(FastMath.DEG_TO_RAD*(-8), new Vector3f(0,0,1));
		H.p(q);
		 
		//note in the 0,0,0 -> a,b,c
		//b = turning right and left
		//a = barrel roll
		//c = up and down a hill
	}

	@Override
	public void simpleInitApp() {
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
		
		sky = new SkyState(); //lighting and shadow stuff is global
		getStateManager().attach(sky);
		
		start = new Start();
		getStateManager().attach(start);
		
		nifty.fromXml("assets/ui/nifty.xml", "start", start);
		guiViewPort.addProcessor(niftyDisplay);
		inputManager.setCursorVisible(true);
		flyCam.setEnabled(false);

		//set the default option
		car = defaultCar;
		sworld = defaultsworld;
		dworld = defaultdworld;
	}
	
	public void startFast() {
		//use the default option and just init straight away
		getStateManager().detach(start);
		
		if (car == null || (sworld == null && dworld == null)) {
			System.err.println("Defaults not set.");
		}
		World world = null;
		if (sworld != null)
			world = new StaticWorldBuilder(sworld);
		else if (dworld != null)
			world = dworld.getBuilder();
		else
			H.e("World generation error occured.");
		
		startDrive(car, world);
		App.nifty.gotoScreen("drive-noop");
	}
	
	//HERE is the logic for the app progress.
	// its the thing you call when the app state is done with itself
	public void next(AppState app) {
		AppStateManager state = getStateManager();
		
		if (app instanceof Start) {
			state.detach(start);
			
			startChooseCar();
			
		} else if (app instanceof ChooseCar) {
			state.detach(chooseCar);
			startChooseMap();
			
		} else if (app instanceof ChooseMap) {
			state.detach(chooseMap);
			
			startDrive(chooseCar.getCarData(), chooseMap.getWorld());
			
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
			H.p("Unexpected state called me '" + app + "' - rally.next()");
		}
	}
	
	private void startChooseCar() {
		chooseCar = new ChooseCar();
		getStateManager().attach(chooseCar);
		App.nifty.gotoScreen("chooseCar");
	}
	
	private void startChooseMap() {
		chooseMap = new ChooseMap();
		getStateManager().attach(chooseMap);
		App.nifty.gotoScreen("chooseMap");
	}
	
	private void startDrive(CarData car, World world) {
		if (menu != null || drive != null) return; //not sure what this is actually hoping to stop
		
		menu = new DriveMenu();
		getStateManager().attach(menu);
		
		H.p(world.getType() + " " + sworld + " " + dworld);
		
		drive = new Drive(car, world);
		getStateManager().attach(drive);
		
		App.nifty.gotoScreen("drive-noop");
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
