package game;

import java.util.HashMap;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.audio.AudioNode;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.input.RawInputListener;
import com.jme3.material.Material;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.texture.Texture;

public class CarBuilder extends Node {

	HashMap<Integer, CarEntry> cars;

	CarBuilder() { //TODO maybe some state stuff, like menu or game type. something please
		cars = new HashMap<>();
		
		App.rally.getRootNode().attachChild(this);
	}

	public void removePlayer(PhysicsSpace space, int id) {
		if (!cars.containsKey(id)) {
			try {
				throw new Exception("A car doesn't have that Id");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		this.detachChildNamed(id+"");
		CarEntry e = cars.get(id);
		
		if (e.audio != null)
			this.detachChild(e.audio);
		this.detachChild(e.car.skidNode);
		
		e.car.cleanup();
		space.remove(e.car);
		cars.remove(id);
	}

	public void addPlayer(PhysicsSpace space, int id, CarData car, Vector3f start, Matrix3f rot, boolean aPlayer) {
		if (cars.containsKey(id)) {
			try {
				throw new Exception("A car already has that Id");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		Rally r = App.rally;
		AssetManager am = r.getAssetManager();
		

		Spatial carmodel = am.loadModel(car.carModel);
		if (carmodel instanceof Geometry) {

		} else {
			carmodel = (Node) am.loadModel(car.carModel);

			TextureKey key = new TextureKey("Textures/Sky/Bright/BrightSky.dds", true);
			key.setGenerateMips(true);
			key.setAsCube(true);
			final Texture tex = am.loadTexture(key);

			for (Geometry g: H.getGeomList((Node)carmodel)) {
				Material m = g.getMaterial();
				m.setBoolean("UseMaterialColors", true);
				if (aPlayer) //player gets reflections
					m.setTexture("EnvMap", tex);
				m.setVector3("FresnelParams", new Vector3f(0.05f, 0.18f, 0.11f));
				g.setMaterial(m);
			}
		}

		//create a compound shape and attach CollisionShape for the car body at 0,1,0
		//this shifts the effective center of mass of the BoxCollisionShape to 0,-1,0
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

		this.attachChild(carNode);
		this.attachChild(player.skidNode);
		player.setPhysicsLocation(start);
		player.setPhysicsRotation(rot);

		if (aPlayer) { //players get the keyboard
			player.makeControl();
		} else {
			player.makeAI();
		}
		
		if (aPlayer) {
			player.giveSound(new AudioNode(am, "assets/sound/engine.wav"));
		}
		
		CarEntry entry = new CarEntry(player, null);
		cars.put(id, entry);

		space.add(player);
	}

	public void update(float tpf) {
		if (cars.isEmpty()) 
			return;

		for (Integer i : cars.keySet()) {
			cars.get(i).car.myUpdate(tpf);
		}

		if (App.rally.drive.dynamicWorld) { //TODO this logic should probably not be here
			App.rally.drive.worldB.update(cars.get(0).car.getPhysicsLocation());
		}

		if (App.rally.drive.ifDebug) {
			H.p(cars.get(0).car.getPhysicsLocation() + "distance:"+cars.get(0).car.distance);
		}
	}

	public MyPhysicsVehicle get(int a) {
		if (cars.containsKey(a))
			return cars.get(a).car;
		return null;
	}

	public void cleanup() {
		for (int key : cars.keySet()) {
			CarEntry car = cars.get(key);
			
			car.car.cleanup();
		}
	}
	
	private class CarEntry {
		
		MyVC car;
		AudioNode audio;
		
		CarEntry(MyVC car, AudioNode audio) {
			this.car = car;
			this.audio = audio;
		}
	}
}
