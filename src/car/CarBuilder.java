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
import com.jme3.math.Vector4f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.shader.VarType;
import com.jme3.texture.Image.Format;
import com.jme3.texture.TextureCubeMap;

import car.ai.CarAI;
import car.ai.DriveAtAI;
import car.data.Car;
import car.ray.CarDataConst;
import car.ray.RayCarControl;
import game.App;
import game.Main;
import helper.H;
import helper.Log;

public class CarBuilder extends AbstractAppState {

	private static final boolean IF_REFLECTIONS = false;
	
	private HashMap<RayCarControl, CarReflectionMap> reflectionMaps;
	private HashMap<Integer, RayCarControl> cars;
	private Node rootNode;

	public CarBuilder() {
		cars = new HashMap<>();
		reflectionMaps = new HashMap<>();
		rootNode = new Node("Car Builder Root");
		
		App.rally.getRootNode().attachChild(rootNode);
	}
	
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		Log.p("CarBuilder init");
	}
	
	public void setEnabled(boolean state) {
		super.setEnabled(state);
		for (Integer i : cars.keySet()) {
			cars.get(i).setEnabled(state);
			cars.get(i).enableSound(state);
		}
		for (CarReflectionMap map: reflectionMaps.values()) {
			map.setEnabled(state);
		}
	}
	
	//TODO this should be giving the ai
	public RayCarControl addCar(int id, Car car, Vector3f start, Matrix3f rot, boolean aPlayer, BiFunction<RayCarControl, RayCarControl, CarAI> ai) {
		if (cars.containsKey(id)) {
			try {
				throw new Exception("A car already has that id: " + id);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		CarDataConst carData = car.get();
		
		Main r = App.rally;
		AssetManager am = r.getAssetManager();
		
		Spatial carModel = am.loadModel(carData.carModel);
		
		TextureCubeMap environmentMap = new TextureCubeMap(512, 512, Format.RGBA8);
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
				
				//add environment map
				Material newM = new Material(am, "Common/MatDefs/Light/Lighting.j3md");
				newM.setBoolean("UseMaterialColors", true);
				newM.setBoolean("VertexLighting", false);
				
				newM.setParam("EnvMap", VarType.TextureCubeMap, environmentMap);
				
				newM.setVector4("Ambient", new Vector4f(0.5f, 0.5f, 0.5f, 1));
				newM.setVector4("Diffuse", new Vector4f(1, 1, 1, 1));
				newM.setVector4("Specular", new Vector4f(1, 1, 1, 1));
				newM.setFloat("Shininess", 2);
//				newM.setVector3("FresnelParams", new Vector3f(0.05f, 0.18f, 0.11f));
				newM.setVector3("FresnelParams", new Vector3f(0.2f, 0.5f, 0.2f)); //TODO
				
				g.setMaterial(newM);
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
		
		if (aPlayer) { //players get sound
			player.giveSound(new AudioNode(am, "assets/sound/engine.wav", AudioData.DataType.Buffer));
		
			if (IF_REFLECTIONS) {
				//lastly add a reflection map
				CarReflectionMap reflectionMap = new CarReflectionMap(player, environmentMap);
				reflectionMaps.put(player, reflectionMap);
				App.rally.getStateManager().attach(reflectionMap);
			}
		}
		
		cars.put(id, player);
		
		return player;
	}

	public void setCarData(int id, CarDataConst carData) {
		RayCarControl car = this.get(0);
		if (car == null)
			return;
		
		car.setCarData(carData);
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
		
		if (reflectionMaps.containsKey(car)) {
			CarReflectionMap reflectionMap = reflectionMaps.get(car);
			App.rally.getStateManager().detach(reflectionMap);
			reflectionMaps.remove(car);
		}
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
		Log.p("carbuilder cleanup");
	}
}
