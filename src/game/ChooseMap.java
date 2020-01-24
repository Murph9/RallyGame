package game;

import java.lang.reflect.InvocationTargetException;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import helper.Log;
import helper.Screen;
import world.*;
import world.highway.HighwayWorld;
import world.lsystem.LSystemWorld;
import world.osm.OsmWorld;
import world.track.TrackWorld;
import world.wp.WP.DynamicType;

public class ChooseMap extends BaseAppState {

	private static WorldType worldType = WorldType.NONE;
	private static World world = null;
	
	private final IChooseStuff choose;

	private BasicCamera camera;

	public ChooseMap(IChooseStuff choose) {
		this.choose = choose;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Application app) {
		getState(BulletAppState.class).setEnabled(true);
		
		camera = new BasicCamera("Camera", app.getCamera(), new Vector3f(-70,50,0), new Vector3f(20,1,0));
		getStateManager().attach(camera);
		
		//init gui
		Container myWindow = new Container();
        ((SimpleApplication)app).getGuiNode().attachChild(myWindow);
        
        Screen screen = new Screen(app.getContext().getSettings());
		myWindow.setLocalTranslation(screen.topLeft());
		
		//these values are not x and y because they are causing confusion
		int i = 0;
		int j = 0;
		myWindow.addChild(new Label("Choose Map"), j, i);
		j++;
		myWindow.addChild(new Label("Static"), j, i);
		j++;
		for (StaticWorld s : StaticWorld.values()) {
			addButton(myWindow, WorldType.STATIC, s.name(), j, i);
			i++;
		}
		i = 0;
		j++;
		myWindow.addChild(new Label("Dynamic"), j, i);
		j++;
		for (DynamicType d : DynamicType.values()) {
			addButton(myWindow, WorldType.DYNAMIC, d.name(), j, i);
			i++;
		}
		i = 0;
		j++;
		myWindow.addChild(new Label("Other"), j, i);
		j++;
		for (WorldType t: WorldType.values()) {
			if (t != WorldType.STATIC && t != WorldType.DYNAMIC && t != WorldType.NONE) {
				addButton(myWindow, t, t.name(), j, i);
				i++;
			}
		}
		i = 0;
		j++;
		Button button = myWindow.addChild(new Button("Choose"), j, i);
		button.addClickCommands(new Command<Button>() {
            @Override
            public void execute( Button source ) {
            	if (worldType == WorldType.NONE)
            		return; //do not select it
				((SimpleApplication)app).getGuiNode().detachChild(myWindow);
            	chooseMap();
            }
        });
	}
	
	@SuppressWarnings("unchecked")
	private void addButton(Container myWindow, WorldType world, String s, int j, int i) {
		Button button = myWindow.addChild(new Button(s), j, i);
		button.addClickCommands(new Command<Button>() {
            @Override
            public void execute( Button source ) {
            	setWorld(world.name(), s);
            }
        });
	}

	public void update(float tpf) {
		if (!isEnabled()) return;
		super.update(tpf);
	}

	@Override
	public void cleanup(Application app) {
		getStateManager().detach(camera);
		camera = null;
		getStateManager().detach(world);
		world = null;
	}

	@Override
	protected void onEnable() {
	}
	@Override
	protected void onDisable() {
	}

	////////////////////////
	//UI stuff
	public void chooseMap() {
		if (world == null) { Log.e("no return value for ChooseMap()"); return; }
		choose.chooseMap(getWorld());
	}
	private World getWorld() {
		World newWorld = null;
		try {
            newWorld = world.copy();
			getStateManager().detach(world);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
			 | NoSuchMethodException | SecurityException | InvocationTargetException e) {
			e.printStackTrace();
		}
		world = null;
		return newWorld;
	}

	public void setWorld(String typeStr, String subType) {
		if (world != null && world.isInitialized()) {
			getStateManager().detach(world);
			world = null;
		}
		worldType = WorldType.valueOf(WorldType.class, typeStr);
		
		switch (worldType) {
			case STATIC:
				StaticWorld sworld = StaticWorld.valueOf(StaticWorld.class, subType);
				world = new StaticWorldBuilder(sworld);
				break;
			case DYNAMIC:
				DynamicType dworld = DynamicType.valueOf(DynamicType.class, subType);
				world = dworld.getBuilder();
				break;
			case OBJECT:
				world = new ObjectWorld();
				break;
			case FULLCITY:
				world = new FullCityWorld();
				break;
			case LSYSTEM:
				world = new LSystemWorld();
				break;
			case HIGHWAY:
				world = new HighwayWorld();
				break;
			case FLAT:
				world = new FlatWorld();
				break;
			case MOVING:
				world = new MovingWorld();
				break;
			case TRACK:
				world = new TrackWorld();
				break;
			case OSM:
				world = new OsmWorld();
				break;
				
			default:
				Log.p("Non valid world type in ChooseMap.setWorld() method: " + worldType.name());
				return;
		}
		
		getStateManager().attach(world);
	}
}
