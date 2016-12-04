package game;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import world.*;
import world.curve.CurveWorld;
import world.wp.WP.DynamicType;

public class ChooseMap extends AbstractAppState {

	private static BulletAppState bulletAppState;

	private static boolean worldIsSet = false;
	private static WorldType worldType = WorldType.NONE;
	private static World world = null;
	
	private MyCamera camNode;

	public ChooseMap() {
		bulletAppState = new BulletAppState();
		App.rally.getStateManager().attach(bulletAppState);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		bulletAppState = new BulletAppState();
		stateManager.attach(bulletAppState);
		
		camNode = new MyCamera("Cam Node - Choose Map", App.rally.getCamera(), null);
		camNode.setLocalTranslation(-70, 50, 0);
		camNode.lookAt(new Vector3f(20,1,0), new Vector3f(0,1,0));
		
		App.rally.getRootNode().attachChild(camNode);
		
		//init gui
		Container myWindow = new Container();
		App.rally.getGuiNode().attachChild(myWindow);
		myWindow.setLocalTranslation(H.screenTopLeft());
		
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
            	chooseMap();
            	App.rally.getGuiNode().detachChild(myWindow);
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

		if (worldType != WorldType.NONE && !worldIsSet) { //keep checking if the ui changed something
			//if the worldtype has been set, init the apprpriate world type
			worldIsSet = true;
			return;
		}
		
		if (world != null) {
			if (!world.isInit()) {
				Node n = world.init(getPhysicsSpace(), App.rally.getViewPort());
				App.rally.getRootNode().attachChild(n);
			}
			world.update(tpf, new Vector3f(0,0,0), false);
		}

		camNode.myUpdate(tpf);
	}

	public PhysicsSpace getPhysicsSpace() {
		return bulletAppState.getPhysicsSpace();
	}

	public void cleanup() {
		App.rally.getStateManager().detach(bulletAppState);
		App.rally.getRootNode().detachChild(camNode);
		App.rally.getRootNode().detachChild(world.getRootNode());
	}

	////////////////////////
	//UI stuff
	public void chooseMap() {
		if (world == null) { H.p("no return value for ChooseMap()"); }
		App.rally.next(this);
	}
	public World getWorld() {
		world.reset();
		world.cleanup();
		return world;
	}

	public void setWorld(String typeStr, String subType) {
		if (world != null && world.isInit()) {
			App.rally.getRootNode().detachChild(world.getRootNode());
			world.cleanup();
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
			case TERRAIN:
				world = new TerrainWorld();
				break;
			case OBJECT:
				world = new ObjectWorld();
				break;
			case FULLCITY:
				world = new FullCityWorld();
				break;
			case CURVE:
				world = new CurveWorld();
				break;
				
			default:
				H.e("Non valid world type in ChooseMap.setWorld() method");
				return;
		}
	}
}
