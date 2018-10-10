package car;

import java.util.Collection;
import java.util.HashMap;
import java.util.function.BiFunction;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import car.ai.CarAI;
import car.ai.DriveAtAI;
import car.ray.CarDataConst;
import car.ray.RayCarControl;
import game.App;
import game.Main;
import helper.H;

public class CarBuilder extends AbstractAppState {

	private HashMap<Integer, RayCarControl> cars;
	private Node rootNode;

	public CarBuilder() {
		cars = new HashMap<>();
		rootNode = new Node("Car Builder Root");
		
		App.rally.getRootNode().attachChild(rootNode);
	}
	
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		H.p("carbuilder init");
	}
	
	public void setEnabled(boolean state) {
		super.setEnabled(state);
		for (Integer i : cars.keySet()) {
			cars.get(i).setEnabled(state);
			cars.get(i).enableSound(state);
			
			/* TODO this should be defined by the car, right?
			for (int j = 0; j < 4; j++) {
				cars.get(i).getWheel(j).setEnabled(state);
			}
			*/
		}
	}
	
	//TODO this should be giving the ai
	public RayCarControl addCar(int id, CarDataConst carData, Vector3f start, Matrix3f rot, boolean aPlayer, BiFunction<RayCarControl, RayCarControl, CarAI> ai) {
		if (cars.containsKey(id)) {
			try {
				throw new Exception("A car already has that id: " + id);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		//clone CarData so if its edited, only this one changes not the global one
		carData = carData.cloneWithSerialization(); //TODO should be removable at some point
		
		Main r = App.rally;
		AssetManager am = r.getAssetManager();
		
		Spatial carModel = am.loadModel(carData.carModel);
		if (carModel instanceof Geometry) {

		} else {
			carModel = (Node) am.loadModel(carData.carModel);

			//TODO car reflections in material def
			
			for (Geometry g: H.getGeomList((Node)carModel)) {  
				Material m = g.getMaterial();
				if (!m.getMaterialDef().getName().equals("Unshaded")) { //this material type not correct for these settings
					m.setBoolean("UseMaterialColors", true);
					m.setVector3("FresnelParams", new Vector3f(0.05f, 0.18f, 0.11f));
				}
				g.setMaterial(m);
			}
		}
		
		Node carNode = new Node(id+"");
		
		//update the collision shape, NOTE: a convex collision shape or hull might be faster here
		CollisionShape colShape = CollisionShapeFactory.createDynamicMeshShape(carModel);
		RayCarControl player = new RayCarControl(App.rally.getPhysicsSpace(), colShape, carData, carNode);
		carNode.attachChild(carModel);

		if (aPlayer) { //player gets a shadow
			carNode.setShadowMode(ShadowMode.CastAndReceive);
		} else {
			carNode.setShadowMode(ShadowMode.Receive);
		}

		rootNode.attachChild(carNode);
		player.setPhysicsLocation(start);
		player.setPhysicsRotation(rot);

		if (aPlayer) { //players get the keyboard
			player.attachControls();
		} else {
			CarAI _ai;
			if (ai != null)
				_ai = ai.apply(player, get(0));
			else
				_ai = new DriveAtAI(player, get(0).getPhysicsObject());
			player.attachAI(_ai);
		}
		
		if (aPlayer) {
			player.giveSound(new AudioNode(am, "assets/sound/engine.wav", AudioData.DataType.Buffer));
		}
		
		cars.put(id, player);
		return player;
	}

	public void removePlayer(int id) {
		if (!cars.containsKey(id)) {
			try {
				throw new Exception("A car doesn't have that id: " + id);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		rootNode.detachChildNamed(id+"");
		RayCarControl car = cars.get(id);
		car.cleanup();
		cars.remove(id);
	}
	public void removePlayer(RayCarControl mpv) {
		for (int key: cars.keySet()) {
			RayCarControl car = cars.get(key);
			if (car == mpv) {
				rootNode.detachChildNamed(key+"");
				car.cleanup();
				cars.remove(key);
				return;
			}
		}
		
		try {
			throw new Exception("That car is not in my records, shrug.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void update(float tpf) {
		if (!isEnabled())
			return;
		
		for (RayCarControl rcc: cars.values()) {
			if (rcc.isEnabled())
				rcc.update(tpf);
		}
	}

	public RayCarControl get(int a) {
		if (cars.containsKey(a))
			return cars.get(a);
		return null;
	}
	public Collection<? extends RayCarControl> getAll() {
		return cars.values();
	}
	public int getCount() {
		return cars.size();
	}

	public void cleanup() {
		for (int key : cars.keySet()) {
			RayCarControl car = cars.get(key);
			car.cleanup();
		}
		App.rally.getRootNode().detachChild(rootNode);
		H.p("carbuilder cleanup");
	}
}
