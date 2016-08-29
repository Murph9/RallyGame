package game;

import java.util.HashMap;
import java.util.LinkedHashMap;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.builder.ControlBuilder;
import de.lessvoid.nifty.elements.Element;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

import world.*;
import world.wp.WP.DynamicType;

public class ChooseMap extends AbstractAppState implements ScreenController {

	private static BulletAppState bulletAppState;

	private static boolean worldIsSet = false;
	private static WorldType worldType = WorldType.NONE;
	private static World world = null;
	
	private HashMap<String, StaticWorld> sSet;
	private HashMap<String, DynamicType> dSet;

	private static Element staticPanel;
	private static Element dynPanel;
	private static Element otherPanel;
	
	private MyCamera camNode;

	public ChooseMap() {
		//init static
		sSet = new LinkedHashMap<>();
		StaticWorld[] list  = StaticWorld.values();
		for (StaticWorld s: list) {
			sSet.put(s.name(), s);
		}

		//init dynamic
		dSet = new LinkedHashMap<>(); //ordered hashmap
		DynamicType[] worlds = DynamicType.values();
		for (DynamicType dt: worlds) {
			dSet.put(dt.name(), dt);
		}
		
		bulletAppState = new BulletAppState();
		App.rally.getStateManager().attach(bulletAppState);
	}

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		bulletAppState = new BulletAppState();
		stateManager.attach(bulletAppState);
		
		camNode = new MyCamera("Cam Node - Choose Map", App.rally.getCamera(), null);
		camNode.setLocalTranslation(-70, 50, 0);
		camNode.lookAt(new Vector3f(20,1,0), new Vector3f(0,1,0));
		
		App.rally.getRootNode().attachChild(camNode);
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
			if (world.isInit()) {
				world.update(tpf, new Vector3f(0,0,0), false);
			} else {
				Node n = world.init(getPhysicsSpace(), App.rally.getViewPort());
				App.rally.getRootNode().attachChild(n);
			}
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

	@Override
	public void bind(Nifty nifty, Screen screen) {
		if (screen.getScreenId().equals("chooseMap")) {
			staticPanel = screen.findElementByName("staticPanel");
			if (staticPanel != null) {
				for (String s : sSet.keySet()) {
					MakeButton(nifty, screen, staticPanel, WorldType.STATIC, s);
				}
			}
			
			dynPanel = screen.findElementByName("dynPanel");
			if (dynPanel != null) {
				for (String s : dSet.keySet()) {
					MakeButton(nifty, screen, dynPanel, WorldType.DYNAMIC, s);
				}
			}
			
			otherPanel = screen.findElementByName("otherPanel"); 
			if (otherPanel != null){
				MakeButton(nifty, screen, otherPanel, WorldType.TERRAIN, "Terrain based");
			}
		}
	}
	
	private void MakeButton(Nifty nifty, Screen screen, Element panel, WorldType type, String name) {
		ControlBuilder cb = new ControlBuilder("button") {{
			id(name);
			alignLeft();
			style("nifty-button");
			padding("10px");

			interactOnClick("setWorld(" + type + "," + name + ")");
			
			parameter("label", name);
		}};
		
		cb.build(nifty, screen, panel);
	}
	
	public void setWorld(String typeStr, String subType) {
		if (world != null && world.isInit()) {
			App.rally.getRootNode().detachChild(world.getRootNode());
			world.cleanup();
		}
		
		//TODO find how to get the button clicked 
		
		worldType = WorldType.valueOf(WorldType.class, typeStr);
		if (worldType == WorldType.STATIC) {
			StaticWorld sworld = StaticWorld.valueOf(StaticWorld.class, subType);
			
			world = new StaticWorldBuilder(sworld);
		} else if (worldType == WorldType.DYNAMIC) {
			DynamicType dworld = DynamicType.valueOf(DynamicType.class, subType);
			
			world = dworld.getBuilder();
		} else if (worldType == WorldType.TERRAIN) {
			world = new TerrainWorld();
		} else {
			H.e("Non valid world type in setWorld() method");
			return;
		}
		
		
		
	}

	public void onEndScreen() { }
	public void onStartScreen() { }
}
