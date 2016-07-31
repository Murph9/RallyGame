package game;

import java.util.HashMap;
import java.util.LinkedHashMap;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.DropDown;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

import world.*;
import world.wp.WP.DynamicType;

public class ChooseMap extends AbstractAppState implements ScreenController {

	private enum WorldType {
		nothing, staticW, dynamicW;
	}

	private BulletAppState bulletAppState;

	private static boolean worldIsSet = false;
	private static WorldType worldType = WorldType.nothing;

	private static StaticWorld sMap;
	private HashMap<String, StaticWorld> sSet;
	
	private static DynamicType dMap;
	private HashMap<String, DynamicType> dSet;
	private Spatial dnode;

	private static DropDown<String> dropdown;
	private MyCamera camNode;

	public ChooseMap() {
		//camera
		camNode = new MyCamera("Cam Node 2", App.rally.getCamera(), null);
		camNode.setLocalTranslation(100, 140, 100);
		camNode.lookAt(new Vector3f(0,1,0), new Vector3f(0,1,0));

		App.rally.getRootNode().attachChild(camNode);
		
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
		
		//set initial values
		sMap = list[0];
		dMap = worlds[0];
	}

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		bulletAppState = new BulletAppState();
		app.getStateManager().attach(bulletAppState);
	}

	private void createWorld() {
		if (worldType == WorldType.staticW) {
			StaticWorldBuilder.addStaticWorld(getPhysicsSpace(), sMap, App.rally.sky.ifShadow);
		} else if (worldType == WorldType.dynamicW) {
			if (dnode != null)
				App.rally.getRootNode().detachChild(dnode);
			
			dnode = dMap.PlaceDemoSet(getPhysicsSpace(), App.rally.getGuiViewPort());
			App.rally.getRootNode().attachChild(dnode);
			
			camNode.setLocalTranslation(-70, 50, 0);
			camNode.lookAt(new Vector3f(20,1,0), new Vector3f(0,1,0));
		}
	}

	public void update(float tpf) {
		if (!isEnabled()) return;
		super.update(tpf);

		if (worldType != WorldType.nothing && !worldIsSet) { //keep checking if the ui changed something
			//if the worldtype has been set, init the apprpriate world type
			createWorld();
			worldIsSet = true;
			return;
		}

		if (dropdown != null) {
			String option = dropdown.getSelection();
			if (worldType == WorldType.staticW) {
				StaticWorld w = sSet.get(option);
				if (w != null && w != sMap) {
					//remove previous map
					StaticWorldBuilder.removeStaticWorld(getPhysicsSpace(), sMap);

					sMap = w;
					StaticWorldBuilder.addStaticWorld(getPhysicsSpace(), sMap, App.rally.sky.ifShadow);
				}
				
			} else if (worldType == WorldType.dynamicW) {
				DynamicType w = dSet.get(option);
				if (w != null && w != dMap) {
					dMap = w;
					
					App.rally.getRootNode().detachChild(dnode);
					dnode = dMap.PlaceDemoSet(getPhysicsSpace(), App.rally.getGuiViewPort());
					App.rally.getRootNode().attachChild(dnode);
					H.p(dnode);
				}
			}
		}

		camNode.myUpdate(tpf);
	}

	public PhysicsSpace getPhysicsSpace() {
		return bulletAppState.getPhysicsSpace();
	}

	public void cleanup() {
		StaticWorldBuilder.removeStaticWorld(getPhysicsSpace(), sMap);
		if (dnode != null) //remove current demo
			App.rally.getRootNode().detachChild(dnode);
		
		
		App.rally.getRootNode().detachChild(camNode);
	}

	////////////////////////
	//UI stuff
	public void staticWorld() {
		worldType = WorldType.staticW;
		App.nifty.gotoScreen("chooseMap");
	}
	public void dynamicWorld() {
		worldType = WorldType.dynamicW;
		App.nifty.gotoScreen("chooseMap");
	}

	public void chooseMap() {
		if (sMap == null && dMap == null) { H.p("no return value for ChooseMap()"); }
		App.rally.next(this);
	}

	public StaticWorld getMapS() {
		if (worldType == WorldType.staticW)
			return sMap;
		else
			return null;
	}
	public DynamicType getMapD() {
		if (worldType == WorldType.dynamicW)
			return dMap;
		else 
			return null;
	}

	@Override
	public void bind(Nifty arg0, Screen arg1) {
		if (arg1.getScreenId().equals("chooseMap")) {
			dropdown = H.findDropDownControl(arg1, "mapdropdown");
			if (dropdown != null) {
				if (worldType == WorldType.staticW) {
					for (String s : sSet.keySet()) {
						dropdown.addItem(s);
					}
					dropdown.selectItemByIndex(0);
				
				} else if (worldType == WorldType.dynamicW) {
					for (String s : dSet.keySet()) {
						dropdown.addItem(s);
					}
					dropdown.selectItemByIndex(0);
				}
			}
		}
	}

	public void onEndScreen() { }
	public void onStartScreen() { }
}
