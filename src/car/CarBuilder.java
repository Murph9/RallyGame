package car;

import java.util.HashMap;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.audio.AudioData;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import car.ai.DriveAtAI;
import game.App;
import game.Main;
import helper.H;

public class CarBuilder extends AbstractAppState {

	HashMap<Integer, MyVC> cars;
	Node rootNode;

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
			cars.get(i).enableSound(state);
		}
	}
	
	//TODO this should be giving the ai
	public MyVC addCar(int id, CarData car, Vector3f start, Matrix3f rot, boolean aPlayer) {
		if (cars.containsKey(id)) {
			try {
				throw new Exception("A car already has that Id");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		Main r = App.rally;
		AssetManager am = r.getAssetManager();

		Spatial carmodel = am.loadModel(car.carModel);
		if (carmodel instanceof Geometry) {

		} else {
			carmodel = (Node) am.loadModel(car.carModel);

			//TODO car reflection
			for (Geometry g: H.getGeomList((Node)carmodel)) {  
				Material m = g.getMaterial();
				if (!m.getMaterialDef().getName().equals("Unshaded")) { //this material type not correct for these settings
					m.setBoolean("UseMaterialColors", true);
					m.setVector3("FresnelParams", new Vector3f(0.05f, 0.18f, 0.11f));
				}
				g.setMaterial(m);
			}
		}

		//its possible to shift the center of gravity offset (TODO add to CarData)
		//Convex collision shape or hull might be faster here)
		CompoundCollisionShape compoundShape = new CompoundCollisionShape();
		compoundShape.addChildShape(CollisionShapeFactory.createDynamicMeshShape(carmodel), new Vector3f(0,0,0));
		
		Node carNode = new Node(id+"");
		MyVC player = new MyVC(compoundShape, car, carNode);
		
		//TODO player.addRawInputListener(new JoystickEventListner(this));
		
		carNode.addControl(player);
		carNode.attachChild(carmodel);

		if (aPlayer) { //player gets a shadow
			carNode.setShadowMode(ShadowMode.CastAndReceive);
		} else {
			carNode.setShadowMode(ShadowMode.Receive);
		}

		rootNode.attachChild(carNode);
		rootNode.attachChild(player.skidNode);
		player.setPhysicsLocation(start);
		player.setPhysicsRotation(rot);

		if (aPlayer) { //players get the keyboard
			player.attachKeyControl();
		} else {
			player.attachAI(new DriveAtAI(player, get(0)));
		}
		
		if (aPlayer) {
			player.giveSound(new AudioNode(am, "assets/sound/engine.wav", AudioData.DataType.Buffer));
		}
		
		cars.put(id, player);

		App.rally.getPhysicsSpace().add(player);
		return player;
	}

	public void removePlayer(int id) {
		if (!cars.containsKey(id)) {
			try {
				throw new Exception("A car doesn't have that Id");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		rootNode.detachChildNamed(id+"");
		MyVC car = cars.get(id);
		
		rootNode.detachChild(car.skidNode);
		
		App.rally.getPhysicsSpace().remove(car);
		car.cleanup();
		cars.remove(id);
	}
	
	public void update(float tpf) {
		if (!isEnabled())
			return;
		
		if (!cars.isEmpty() && this.isEnabled()) {
			for (Integer i : cars.keySet()) {
				cars.get(i).myUpdate(tpf);
			}
		}
	}

	public MyPhysicsVehicle get(int a) {
		if (cars.containsKey(a))
			return cars.get(a);
		return null;
	}

	public void cleanup() {
		for (int key : cars.keySet()) {
			MyVC car = cars.get(key);
			App.rally.getPhysicsSpace().remove(car);
			car.cleanup();
		}
		App.rally.getRootNode().detachChild(rootNode);
		H.p("carbuilder cleanup");
	}
}
