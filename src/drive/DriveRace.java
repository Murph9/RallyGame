package drive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import car.Car;
import car.CarBuilder;
import car.CarCamera;
import car.CarData;
import car.CarUI;
import car.MyPhysicsVehicle;
import car.ai.CarAI;
import car.ai.RaceAI;
import game.App;
import game.RaceMenu;
import helper.H;

public class DriveRace extends AbstractAppState {

	//TODO:
	//- better AI, otherwise this actually sucks
	
	public RaceMenu menu;
	
	//things that should be in a world class
	private static List<RigidBodyControl> landscapes = new ArrayList<RigidBodyControl>(5);
    private static List<Spatial> models = new ArrayList<Spatial>(5); //at least 5
	private Vector3f worldStart;
	private Matrix3f worldRot = new Matrix3f();
	private Node rootNode = new Node("root");
	
	//car stuff
	public CarBuilder cb;
	protected CarData car;

	//gui and camera stuff
	private CarCamera camera;
	private CarUI uiNode;
	
	//ai things
	private int themCount = 4;
	private CarData themType = Car.Runner.get();
	
	//race start things
	private Vector3f[] worldStarts;
	private MyPhysicsVehicle[] cars; 
	private Integer[] carCheckpointNext;
	
	private Vector3f[] checkpoints;
	
	//hacky things
	float globalScaleX = 1;
	float globalScaleY = 0.25f;
	float globalScaleZ = 1;
	
	//debug things
	Node debugNode;
	Geometry[] carArrows;
	
	public DriveRace() {
		super();
		this.car = Car.Runner.get();
		this.menu = new RaceMenu(null);
	}
	
	@Override
    public void initialize(AppStateManager stateManager, Application app) {
    	super.initialize(stateManager, app);
    	nextState();
    	
    	Collection<PhysicsRigidBody> list = App.rally.getPhysicsSpace().getRigidBodyList();
    	if (list.size() > 0) {
    		H.p("some one didn't clean up after themselves..." + list.size());
    		for (PhysicsRigidBody r: list)
    			App.rally.getPhysicsSpace().remove(r);
    	}
    	
		App.rally.getRootNode().attachChild(rootNode);
		addTrack(true);
    	
		worldStart = checkpoints[checkpoints.length - 1];
		Quaternion q = new Quaternion();
		q.lookAt(checkpoints[0].subtract(checkpoints[checkpoints.length - 1]), Vector3f.UNIT_Y); 
		worldRot = q.toRotationMatrix();
		
    	//TODO put this in the 3d world model
		this.worldStarts = new Vector3f[themCount+1];
		for (int i = 0; i < worldStarts.length; i++) {
			this.worldStarts[i] = worldStart.add(worldRot.mult(new Vector3f(3,0,0).mult(i % 2 == 0 ? (i+1)/2 : -((i+1)/2))));
		}
    	
		//buildCars
		this.cb = new CarBuilder();
		cb.addCar(0, car, worldStarts[0], worldRot, true, null);
		app.getStateManager().attach(cb);
		app.getStateManager().attach(menu);
		
		uiNode = new CarUI(cb.get(0));
		app.getStateManager().attach(uiNode);
		
    	for (int i = 0; i < this.themCount; i++)
    		this.cb.addCar(i + 1, themType, worldStarts[i+1], worldRot, false, (c,s) -> new RaceAI(c, s, this));
		
		//initCameras
		camera = new CarCamera("Camera", App.rally.getCamera(), cb.get(0));
		App.rally.getStateManager().attach(camera);
		app.getInputManager().addRawInputListener(camera);
		
		App.rally.bullet.setEnabled(true);
		
		Object[] a = cb.getAll().toArray();
		cars = Arrays.copyOf(a, a.length, MyPhysicsVehicle[].class);
		carCheckpointNext = new Integer[cars.length];
		Arrays.fill(carCheckpointNext, 0);
		
		carArrows = new Geometry[cars.length];
		
		nextState();
	}
	
	private boolean stateChanged = false;
	private RaceState state = RaceState.NA;
	enum RaceState {
		NA,
		Init,
		Ready,
		Racing,
		Win,
	}
	private void nextState() {
		stateChanged = true;
		switch(state) {
			case NA:
				state = RaceState.Init;
				break;
			case Init:
				state = RaceState.Ready;
				break;
			case Ready:
				state = RaceState.Racing;
				break;
			case Racing:
				state = RaceState.Win;
				break;
			case Win:
				state = RaceState.Init;
				break;
			default:
				try {
					throw new Exception("Huh?" + state);
				} catch (Exception e) {
					e.printStackTrace();
				}
				return;
		}
	}
	

	private float StateTimeout = 0;
	@Override
	public void update(float tpf) {
		if (!isEnabled()) return;
		super.update(tpf);

		menu.setText("State:"+state.name()
		+"\nStateTimeout:" + StateTimeout
		+"\nCheckpoints:" + H.str(carCheckpointNext, ","));
		
		if (stateChanged) {
			stateChanged = false;
			
			if (state == RaceState.Init) {
				resetAllCars();
			} else if (state == RaceState.Ready)
				StateTimeout = 2;
		}
		
		switch(state) {
		case NA:
			return;
		case Init:
			resetAllCars();
			break;
		case Ready:
			resetAllCars();
			break;
		case Racing:
			normalUpdate(tpf);
			break;
		case Win:
			//delay and stuff maybe
			break;
		default:
			try {
				throw new Exception("Huh?" + state);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		
		if (StateTimeout != -1.0f) {
			StateTimeout -= tpf; //only update when not -1 as it will reset it every frame
			if (StateTimeout < 0) {
				StateTimeout = -1; //stop any timeout stuff unless the state says so
				nextState();
			}
		}
		
		
		//update the checkpoint arrows
		if (debugNode != null)
			rootNode.detachChild(debugNode);
		debugNode = new Node("debugnode");
		for (int i = 0; i < cars.length; i++) {
			Vector3f pos = cars[i].getPhysicsLocation().add(0,3,0);
			Vector3f dir = checkpoints[carCheckpointNext[i]].subtract(pos);
			carArrows[i] = H.makeShapeArrow(App.rally.getAssetManager(), ColorRGBA.Cyan, dir, pos);
			debugNode.attachChild(carArrows[i]);
		}
		rootNode.attachChild(debugNode);
	}
	
	private void resetAllCars() {
		for (int i = 0; i < cars.length; i++) {
			resetSingleCar(i);
		}
	}
	private void resetSingleCar(int i) {
		int checkpointIndex = carCheckpointNext[i] - 1;
		if (checkpointIndex < 0)
			checkpointIndex = checkpoints.length - 1;
		Vector3f newPos = checkpoints[checkpointIndex];
		
		cars[i].setPhysicsLocation(newPos.add(0,2,0));
		cars[i].setPhysicsRotation(worldRot); //TODO
		cars[i].setAngularVelocity(new Vector3f());
		cars[i].setLinearVelocity(new Vector3f());
	}
	
	private void normalUpdate(float tpf) {
		//get every car's pos, and check if its close to its current checkpoint
		for (int i = 0; i < cars.length; i++) {
			Vector3f nextCheckPoint = checkpoints[carCheckpointNext[i]];
			if (nextCheckPoint.distance(cars[i].getPhysicsLocation()) < 3) {
				carCheckpointNext[i]++;
				carCheckpointNext[i] = carCheckpointNext[i] % checkpoints.length;
			}
		}
	}
	
	public void cleanup() {
		super.cleanup();
		H.p("cleaning driverace class");
		
		for (RigidBodyControl r: landscapes) {
			App.rally.getPhysicsSpace().remove(r);
		}
		landscapes.clear();
		for (Spatial s: models) {
			rootNode.detachChild(s);
		}
		models.clear();
		
		App.rally.getStateManager().detach(cb);
		cb = null;
		
		App.rally.getStateManager().detach(menu);
		menu = null;
		
		App.rally.getStateManager().detach(uiNode);
		uiNode = null;
				
		App.rally.getStateManager().detach(camera);
		App.rally.getInputManager().removeRawInputListener(camera);
		camera = null;
		
		App.rally.getRootNode().detachChild(rootNode);
	}
	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		App.rally.bullet.setEnabled(enabled);
		this.camera.setEnabled(enabled);
		this.cb.setEnabled(enabled);
	}
	
	//making the world exist
	public void addTrack(boolean ifShadow) {
		AssetManager as = App.rally.getAssetManager();
		List<Vector3f> _checkpoints = new LinkedList<Vector3f>();
		
		Material mat = new Material(as, "Common/MatDefs/Misc/ShowNormals.j3md");
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		
	    //imported model
		Spatial worldNode = as.loadModel("assets/staticworld/lakelooproad.blend");
		if (worldNode instanceof Node) {
			Spatial s = ((Node) worldNode).getChild(0);
			addWorldModel(rootNode, App.rally.getPhysicsSpace(), s, ifShadow);
			for (Spatial points: ((Node)s).getChildren()) {
				if (points.getName().equals("Points")) {
					for (Spatial checkpoint: ((Node)points).getChildren()) {
						_checkpoints.add(checkpoint.getLocalTranslation());
					}
				}
			}
		} else {
			Geometry worldModel = (Geometry) as.loadModel("assets/staticworld/lakelooproad.blend");
			addWorldModel(rootNode, App.rally.getPhysicsSpace(), worldModel, ifShadow);
		}
		
		checkpoints = _checkpoints.toArray(new Vector3f[_checkpoints.size()-1]);
		for (Vector3f checkpoint: checkpoints) {
			checkpoint.multLocal(globalScaleX, globalScaleY, globalScaleZ);
			rootNode.attachChild(H.makeShapeBox(as, ColorRGBA.Brown, checkpoint, 1));
		}
	}

	private void addWorldModel(Node node, PhysicsSpace phys, Spatial s, boolean ifShadow) {
		s.scale(globalScaleX, globalScaleY, globalScaleZ); //world.scale
		
		CollisionShape col = CollisionShapeFactory.createMeshShape(s);
		RigidBodyControl landscape = new RigidBodyControl(col, 0);
		s.addControl(landscape);
		if (ifShadow) {
			s.setShadowMode(ShadowMode.Receive);
		}

		landscapes.add(landscape);
		models.add(s);
		
		phys.add(landscape);
		node.attachChild(s);
	}
	
	
	public void ResetMe(CarAI ai) {
		resetSingleCar(getCarIndexFromAI(ai));
	}
	public Vector3f getNextCheckpoint(CarAI ai, Vector3f pos) {
		return checkpoints[carCheckpointNext[getCarIndexFromAI(ai)]];
	} 
	private int getCarIndexFromAI(CarAI ai) {
		for (int i = 0; i < cars.length; i++)
			if (cars[i].getAI() == ai)
				return i;
		return -1;
	}
}
