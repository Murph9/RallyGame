package game;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.VehicleControl;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class MyVehicleControl extends VehicleControl implements ActionListener {
	
	
	//contact points
	boolean contact0 = false;
	boolean contact1 = false;
	boolean contact2 = false;
	boolean contact3 = false;
	
	float w0_myskid;
	float w1_myskid;
	float w2_myskid;
	float w3_myskid;
	
	boolean ifSmoke = false;
	ParticleEmitter emit;
	
	AssetManager assetManager;
	Rally rally;
	
	Car car;
	
	MyVehicleControl(CollisionShape col, Car car, AssetManager assetManager, Node carNode, Rally rally) {
		super(col, car.mass);
		this.car = car;
		this.assetManager = assetManager;
		this.rally = rally;
		
		this.setSuspensionCompression(car.susCompression);
		this.setSuspensionDamping(car.susDamping);
		this.setSuspensionStiffness(car.stiffness);
		this.setMaxSuspensionForce(25*car.mass);
		
		Node node1 = new Node("wheel 1 node");
		Spatial wheels1 = assetManager.loadModel(car.wheelOBJModel);
		wheels1.center();
		node1.attachChild(wheels1);
		addWheel(node1, new Vector3f(-car.w_xOff, car.w_yOff, car.w_zOff),
				car.wheelDirection, car.wheelAxle, car.restLength, car.wheelRadius, true);

		Node node2 = new Node("wheel 2 node");
		Spatial wheels2 = assetManager.loadModel(car.wheelOBJModel);
		wheels2.rotate(0, FastMath.PI, 0);
		wheels2.center();
		node2.attachChild(wheels2);
		addWheel(node2, new Vector3f(car.w_xOff, car.w_yOff, car.w_zOff),
				car.wheelDirection, car.wheelAxle, car.restLength, car.wheelRadius, true);

		Node node3 = new Node("wheel 3 node");
		Spatial wheels3 = assetManager.loadModel(car.wheelOBJModel);
		wheels3.center();
		node3.attachChild(wheels3);
		addWheel(node3, new Vector3f(-car.w_xOff-0.05f, car.w_yOff, -car.w_zOff),
				car.wheelDirection, car.wheelAxle, car.restLength, car.wheelRadius, false);

		Node node4 = new Node("wheel 4 node");
		Spatial wheels4 = assetManager.loadModel(car.wheelOBJModel);
		wheels4.rotate(0, FastMath.PI, 0);
		wheels4.center();
		node4.attachChild(wheels4);
		addWheel(node4, new Vector3f(car.w_xOff+0.05f, car.w_yOff, -car.w_zOff),
				car.wheelDirection, car.wheelAxle, car.restLength, car.wheelRadius, false);

		//Friction
		setFrictionSlip(0, car.wheel1Slip);
		setFrictionSlip(1, car.wheel2Slip);
		setFrictionSlip(2, car.wheel3Slip);
		setFrictionSlip(3, car.wheel4Slip);
		
		//attaching all the things (wheels)
		carNode.attachChild(node1);
		carNode.attachChild(node2);
		carNode.attachChild(node3);
		carNode.attachChild(node4);
		
		makeSmoke(node1);
		makeSmoke(node2);
		makeSmoke(node3);
		makeSmoke(node4);
		
		////////////////////////
		setupKeys();
	}
	
	private void makeSmoke(Node wheelNode) {
		emit = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 30);
	    emit.setImagesX(15); //smoke is 15x1 (1 is default for y)
	    emit.setEndColor(new ColorRGBA(1f, 1f, 1f, 0f)); //transparent
	    emit.setStartColor(new ColorRGBA(0.4f, 0.4f, 0.4f, 0.3f)); //strong white

	    emit.setStartSize(0.5f);
	    emit.setGravity(0, -4, 0);
	    emit.setLowLife(1f);
	    emit.setHighLife(1f);
	    emit.getParticleInfluencer().setVelocityVariation(0.05f);
//	    emit.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 20, 0));
	    
	    Material mat_emit = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
	    mat_emit.setTexture("Texture", assetManager.loadTexture("Effects/Smoke/Smoke.png"));
	    emit.setMaterial(mat_emit);
	    if (ifSmoke) {
	    	wheelNode.attachChild(emit);
	    }
	}
	
	//controls
	private void setupKeys() {
		rally.getInputManager().addMapping("Lefts", new KeyTrigger(KeyInput.KEY_H));
		rally.getInputManager().addMapping("Rights", new KeyTrigger(KeyInput.KEY_K));
		rally.getInputManager().addMapping("Ups", new KeyTrigger(KeyInput.KEY_U));
		rally.getInputManager().addMapping("Downs", new KeyTrigger(KeyInput.KEY_J));
		rally.getInputManager().addMapping("Space", new KeyTrigger(KeyInput.KEY_SPACE));
		rally.getInputManager().addMapping("Reset", new KeyTrigger(KeyInput.KEY_RETURN));
		rally.getInputManager().addMapping("Impluse", new KeyTrigger(KeyInput.KEY_LCONTROL));
		rally.getInputManager().addMapping("Reverse", new KeyTrigger(KeyInput.KEY_LSHIFT));
		
		rally.getInputManager().addListener(this, "Lefts");
		rally.getInputManager().addListener(this, "Rights");
		rally.getInputManager().addListener(this, "Ups");
		rally.getInputManager().addListener(this, "Downs");
		rally.getInputManager().addListener(this, "Space");
		rally.getInputManager().addListener(this, "Reset");
		rally.getInputManager().addListener(this, "Impluse");
		rally.getInputManager().addListener(this, "Reverse");
		
		//TODO use the Controller class
	}
	
	public void onAction(String binding, boolean value, float tpf) {
		if (binding.equals("Lefts")) {
			if (value) {
				rally.steeringDirection = 1;
			} else {
				rally.steeringDirection = 0;
			}
			steer(rally.steeringCurrent);
		} else if (binding.equals("Rights")) {
			if (value) {
				rally.steeringDirection = -1;
			} else {
				rally.steeringDirection = 0;
			}
		}
		
		if (binding.equals("Ups")) {
			if (value) {
				rally.ifAccel = true;
			} else {
				rally.ifAccel = false;
			}

		} else if (binding.equals("Downs")) {
			if (value) {
				rally.brakeCurrent += car.MAX_BRAKE;
			} else {
				rally.brakeCurrent -= car.MAX_BRAKE;
			}
			
		} else if (binding.equals("Space")) {
			if (value) {
				applyImpulse(car.JUMP_FORCE, Vector3f.ZERO);
				Vector3f old = getPhysicsLocation();
				old.y += 2;
				setPhysicsLocation(old);
			}
			
		} else if (binding.equals("Impluse")) {
			if (value) {
				rally.toggle = !rally.toggle;
				System.out.println("impluse");
			}
			
		} else if (binding.equals("Reset")) {
			if (value) {
				setPhysicsLocation(rally.world.start);
				setPhysicsRotation(new Matrix3f());
				setLinearVelocity(Vector3f.ZERO);
				setAngularVelocity(Vector3f.ZERO);
				resetSuspension();
				
				rally.arrowNode.detachAllChildren();
				
//				skidList.clear();
//				wheel0Last = Vector3f.ZERO;
//				wheel1Last = Vector3f.ZERO;
//				wheel2Last = Vector3f.ZERO;
//				wheel3Last = Vector3f.ZERO;
			} else {
			}
		}
		
		if (binding.equals("Reverse")) {
			rally.ifReverse = !rally.ifReverse;
		}
	}

	//-end controls
	
	public String getWheelTractionInfo() {
		String out = "";
		out = "\n" + getWheel(0).getSkidInfo() + "\n" +
		getWheel(1).getSkidInfo() + "\n" + 
		getWheel(2).getSkidInfo() + "\n" +
		getWheel(3).getSkidInfo();
		return out;
	}
	
	public float getTotalGrip() {
		return w0_myskid+w1_myskid+w2_myskid+w3_myskid;
	}
}
