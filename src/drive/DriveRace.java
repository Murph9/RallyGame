package drive;

import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.BulletAppState;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import car.CarBuilder;
import car.CarCamera;
import car.CarUI;
import car.ai.RaceAI;
import car.data.Car;
import car.ray.RayCarControl;
import helper.Log;
import world.StaticWorldBuilder;
import world.World;

//TODO DriveRace can't be converted to DriveBase as the world must be initialised before this
public class DriveRace extends BaseAppState {

	//TODO better AI, otherwise this actually sucks
	
	public RaceMenu menu;
	
	//things that should be in a world class
	private Node rootNode = new Node("root");
	
    private final Car car;
    private final World world;
    
    // ai things
    private final int themCount = 4;
    private final Car themType = Car.Runner;

    public CarBuilder cb;
    public DriveRaceProgress progress;

	//gui and camera stuff
	private CarCamera camera;
	private CarUI uiNode;
	
	//racing things
    private Vector3f[] worldStarts;
	
	public DriveRace(World world) {
		super();
        this.car = Car.Runner;
        this.world = world;
	}
	
	@Override
    public void initialize(Application app) {
        ((SimpleApplication) app).getRootNode().attachChild(rootNode);
        
        nextState();
        
        //get the list of checkpoints
        //TODO use whatever duel does, when its finished
		List<Vector3f> _checkpoints = new LinkedList<Vector3f>();
		Spatial model = ((StaticWorldBuilder)world).getModelForDriveRace();
		Spatial s = ((Node) model).getChild(0);
		for (Spatial points : ((Node) s).getChildren()) {
			if (points.getName().equals("Points")) {
				for (Spatial checkpoint : ((Node) points).getChildren()) {
					_checkpoints.add(checkpoint.getLocalTranslation());
				}
			}
        }
        Vector3f[] checkpoints = new Vector3f[_checkpoints.size()];
		if (!_checkpoints.isEmpty()) {
			_checkpoints.toArray(checkpoints);
		}
        //end get checkpoints
		
        
        Vector3f worldStart = checkpoints[checkpoints.length - 1];
        Quaternion q = new Quaternion();
        q.lookAt(checkpoints[0].subtract(checkpoints[checkpoints.length - 1]), Vector3f.UNIT_Y);
        Matrix3f worldRot = q.toRotationMatrix();

    	//TODO put this in the world class
		this.worldStarts = new Vector3f[themCount+1];
		for (int i = 0; i < worldStarts.length; i++) {
			this.worldStarts[i] = worldStart.add(worldRot.mult(new Vector3f(3,0,0).mult(i % 2 == 0 ? (i+1)/2 : -((i+1)/2))));
		}
    	
		//buildCars
		this.cb = getState(CarBuilder.class);
		RayCarControl rayCar = cb.addCar(car, worldStarts[0], worldRot, true);
		
		menu = new RaceMenu(null);
		getStateManager().attach(menu);
		
		uiNode = new CarUI(rayCar);
		getStateManager().attach(uiNode);
		
    	for (int i = 0; i < this.themCount; i++) {
			RayCarControl c = this.cb.addCar(themType, worldStarts[i+1], worldRot, false);
			RaceAI rAi = new RaceAI(c, this);
			c.attachAI(rAi, true);
		}
		
		//initCameras
		camera = new CarCamera(app.getCamera(), rayCar);
		getStateManager().attach(camera);
		app.getInputManager().addRawInputListener(camera);
		
		getState(BulletAppState.class).setEnabled(true);
        

        progress = new DriveRaceProgress(getApplication(), checkpoints, cb.getAll());
        
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
	

	private float stateTimeout = 0;
	@Override
	public void update(float tpf) {
		if (!isEnabled()) return;
		super.update(tpf);

		menu.setText("State:"+state.name()
		+"\nStateTimeout:" + this.stateTimeout
		+"\nCheckpoints:" + progress.getCheckpointAsStr());
		
		if (stateChanged) {
			stateChanged = false;
			
			if (state == RaceState.Init) {
				setAllCarsToStart();
			} else if (state == RaceState.Ready)
				this.stateTimeout = 2;
		}
		
		switch(state) {
		case NA:
			return;
		case Init:
			setAllCarsToStart();
			break;
		case Ready:
			setAllCarsToStart();
			break;
		case Racing:
            progress.update(tpf);
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
		
		
		if (this.stateTimeout != -1.0f) {
			this.stateTimeout -= tpf; //only update when not -1 as it will reset it every frame
			if (this.stateTimeout < 0) {
				this.stateTimeout = -1; //stop any timeout stuff unless the state says so
				nextState();
			}
		}		
	}
	
	private void setAllCarsToStart() {
        int count = 0;
        for (RayCarControl car: cb.getAll()) {
            car.setPhysicsLocation(worldStarts[count]);
            car.setPhysicsRotation(world.getStartRot());
            car.setAngularVelocity(new Vector3f());
            car.setLinearVelocity(new Vector3f());
            count++;
        }
	}
    
    @Override
	public void cleanup(Application app) {
        Log.p("cleaning driverace class");
        progress.cleanup();
		
		getStateManager().detach(menu);
		menu = null;
		
		getStateManager().detach(uiNode);
		uiNode = null;
				
		getStateManager().detach(camera);
		app.getInputManager().removeRawInputListener(camera);
		camera = null;
		
		((SimpleApplication)app).getRootNode().detachChild(rootNode);

		cb.removeAll();
		cb = null;
	}
	
	@Override
	protected void onEnable() {
		getState(BulletAppState.class).setEnabled(true);
		this.camera.setEnabled(true);
		this.cb.setEnabled(true);
	}

	@Override
	protected void onDisable() {
		getState(BulletAppState.class).setEnabled(false);
		this.camera.setEnabled(false);
		this.cb.setEnabled(false);
	}
	
	public Vector3f getNextCheckpoint(RayCarControl car, Vector3f pos) {
        return progress.getNextCheckpoint(car, pos);
	}

}
