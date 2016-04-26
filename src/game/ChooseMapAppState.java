package game;

import java.util.HashMap;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.controls.DropDown;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;
import world.StaticWorld;
import world.StaticWorldBuilder;

public class ChooseMapAppState extends AbstractAppState implements ScreenController {

	private BulletAppState bulletAppState;

	private DirectionalLight sun;
	private AmbientLight al;

	static StaticWorld map;
	private HashMap<String, StaticWorld> worldset;

	private final boolean ifShadow = true;

	private DropDown<String> dropdown;
	private MyCamera camNode;

	public ChooseMapAppState() {
		worldset = new HashMap<>();
		StaticWorld[] list  = StaticWorld.values();
		map = list[0];
		for (StaticWorld s: list) {
			worldset.put(s.name(), s);
		}
	}

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		bulletAppState = new BulletAppState();
		app.getStateManager().attach(bulletAppState);

		dropdown = findDropDownControl(App.nifty.getCurrentScreen(), "mapdropdown");
		
		createWorld();
		initCamera();
	}

	private void createWorld() {
		StaticWorldBuilder.addStaticWorld(getPhysicsSpace(), map, ifShadow);
		
		//lights
		al = new AmbientLight();
		al.setColor(ColorRGBA.Blue.mult(0.3f));
		App.rally.getRootNode().addLight(al);

		sun = new DirectionalLight();
		sun.setColor(new ColorRGBA(0.9f, 0.9f, 1f, 1f));
		sun.setDirection(new Vector3f(-0.3f, -0.6f, -0.5f).normalizeLocal());
		App.rally.getRootNode().addLight(sun);
	}

	private void initCamera() {
		camNode = new MyCamera("Cam Node 2", App.rally.getCamera(), null);
		camNode.setLocalTranslation(100, 140, 100);
		
		camNode.lookAt(new Vector3f(0,1,0), new Vector3f(0,1,0));
		
		App.rally.getRootNode().attachChild(camNode);
	}
	
	public void update(float tpf) {
		if (!isEnabled()) return;
		super.update(tpf);
		
		if (dropdown != null) {
			StaticWorld w = worldset.get(dropdown.getSelection());
			if (w != null && w != map) {
				//remove previous map
				StaticWorldBuilder.removeStaticWorld(getPhysicsSpace(), map);
				
				map = w;
				StaticWorldBuilder.addStaticWorld(getPhysicsSpace(), map, ifShadow);
			}
		}
		
		camNode.myUpdate(tpf);
	}
	
	public PhysicsSpace getPhysicsSpace() {
		return bulletAppState.getPhysicsSpace();
	}
	
	public void cleanup() {
		StaticWorldBuilder.removeStaticWorld(getPhysicsSpace(), map);
		
		Node root = App.rally.getRootNode();
		root.detachAllChildren();
		root.removeLight(sun);
		root.removeLight(al);
		
		//TODO more
	}
	
	////////////////////////
	/////UI stuff
	public void chooseMap() {
		if (map == null) { /*err..*/ }
		//TODO this.cleanup(); //set return value
		App.rally.next(this);
	}
	public StaticWorld getMap() {
		return map;
	}

	@Override
	public void bind(Nifty arg0, Screen arg1) {
		DropDown<String> dropdown = findDropDownControl(arg1, "mapdropdown");
		if (dropdown != null) {
			//TODO get list of car types
			for (String s : worldset.keySet()) {
				dropdown.addItem(s);
			}
			dropdown.selectItemByIndex(0);
		}
	}
	private <T> DropDown<T> findDropDownControl(Screen screen, final String id) {
		return screen.findNiftyControl(id, DropDown.class);
	}

	public void onEndScreen() { }
	public void onStartScreen() { }
}
