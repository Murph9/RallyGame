package game;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.texture.Texture;

public class CarBuilder extends Node {
	
	Rally r;
	
	HashMap<Integer, MyVC> carList;
	List<AI> AIlist;
	
	CarBuilder() {
		carList = new HashMap<>();
	}
	
	public void addPlayer(int id, Rally rally, ExtendedVT car, Vector3f start, Matrix3f rot, boolean AI) {
		if (carList.containsKey(id)) {
			try {
				throw new Exception("A car already has that Id");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if (r == null) {
			r = rally;
			r.getRootNode().attachChild(this);
		}
		
		AssetManager am = rally.getAssetManager();
		Node carmodel = (Node) am.loadModel(car.carModel);
		
		TextureKey key = new TextureKey("Textures/Sky/Bright/BrightSky.dds", true);
        key.setGenerateMips(true);
        key.setAsCube(true);
        final Texture tex = am.loadTexture(key);
        
        for (Geometry g: H.getGeomList(carmodel)) {
        	Material m = g.getMaterial();
        	m.setBoolean("UseMaterialColors", true);
            m.setTexture("EnvMap", tex);
            m.setVector3("FresnelParams", new Vector3f(0.05f, 0.18f, 0.11f));
            g.setMaterial(m);
        }
        
		//create a compound shape and attach CollisionShape for the car body at 0,1,0
		//this shifts the effective center of mass of the BoxCollisionShape to 0,-1,0
		CompoundCollisionShape compoundShape = new CompoundCollisionShape();
		compoundShape.addChildShape(CollisionShapeFactory.createDynamicMeshShape(carmodel), new Vector3f(0,0,0));
//		compoundShape.
		
		Node carNode = new Node(id+"");
		MyVC player = new MyVC(compoundShape, car, carNode, rally);
		
		carNode.addControl(player);
		carNode.attachChild(carmodel);
		
		if (AI) { //laggy
			carNode.setShadowMode(ShadowMode.Off);
		} else {
			carNode.setShadowMode(ShadowMode.CastAndReceive);
		}

		this.attachChild(carNode);
		this.attachChild(player.skidNode);
		player.setPhysicsLocation(start);
		player.setPhysicsRotation(rot);
		
		r.getPhysicsSpace().add(player);
		
		carList.put(id, player);
		
		if (AI) {
			if (this.AIlist == null){
				this.AIlist = new LinkedList<AI>();
			}
			AI a = new AI(rally, player);
			this.AIlist.add(a);
			player.giveAI(a);
		}
	}
	
	public void update(float tpf) {
		if (carList.isEmpty()) 
			return;
		
		for (Integer i : carList.keySet()) {
			carList.get(i).myUpdate(tpf);
		}
		
		for (AI a: AIlist) {
			a.update(tpf);
		}
		
		if (r.dynamicWorld) {
			r.worldB.update(carList.get(0).getPhysicsLocation());
		}
			
		if (r.ifDebug) {
			H.p(carList.get(0).getPhysicsLocation() + "distance:"+carList.get(0).distance);
		}
	}
	
	public MyPhysicsVehicle get(int a) {
		return carList.get(a);
	}
}
