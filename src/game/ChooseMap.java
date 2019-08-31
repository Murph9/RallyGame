package game;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import helper.H;
import helper.Log;
import world.*;
import world.highway.HighwayWorld;
import world.lsystem.LSystemWorld;
import world.track.TrackWorld;
import world.wp.WP.DynamicType;

public class ChooseMap extends BaseAppState {

	private SimpleApplication app;

	private static WorldType worldType = WorldType.NONE;
	private static World world = null;
	
	private BasicCamera camera;

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Application app) {
		getState(BulletAppState.class).setEnabled(true);
		
		camera = new BasicCamera("Camera", this.app.getCamera(), new Vector3f(-70,50,0), new Vector3f(20,1,0));
		this.app.getStateManager().attach(camera);
		
		//init gui
		Container myWindow = new Container();
		this.app.getGuiNode().attachChild(myWindow);
		myWindow.setLocalTranslation(H.screenTopLeft(app.getContext().getSettings()));
		
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
		this.app.getStateManager().detach(camera);
		camera = null;
		this.app.getStateManager().detach(world);
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
		((App)this.app).next(this);
	}
	public World getWorld() {
		World newWorld = null;
		try {
			newWorld = world.copy();
			this.app.getStateManager().detach(world);
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
			System.exit(1);
		}
		world = null;
		return newWorld;
	}

	public void setWorld(String typeStr, String subType) {
		if (world != null && world.isInitialized()) {
			this.app.getStateManager().detach(world);
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
			case BASIC:
				world = new BasicWorld();
				break;
			case TRACK:
				world = new TrackWorld();
				break;
				
			default:
				Log.p("Non valid world type in ChooseMap.setWorld() method: " + worldType.name());
				return;
		}
		
		this.app.getStateManager().attach(world);
	}
}
