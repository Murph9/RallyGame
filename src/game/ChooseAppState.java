package game;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

import world.StaticWorld;
import world.StaticWorldBuilder;

public class ChooseAppState extends AbstractAppState {

	private BulletAppState bulletAppState;
	
	private DirectionalLight sun;
	
	private StaticWorld world;
	private StaticWorldBuilder sWorldB;
	private final boolean ifShadow = true;
	
	private CarBuilder cb;
	private FancyVT car = new Runner();

	MyCamera camNode;
	
	ChooseAppState() {
		world = StaticWorld.choose;
	}
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		bulletAppState = new BulletAppState();
		app.getStateManager().attach(bulletAppState);

		createWorld();
		buildPlayers();
		initCamera();
	}

	private void createWorld() {
		sWorldB = new StaticWorldBuilder(getPhysicsSpace(), world, ifShadow);

		//lights
		AmbientLight al = new AmbientLight();
		al.setColor(ColorRGBA.Blue.mult(0.3f));
		App.rally.getRootNode().addLight(al);

		sun = new DirectionalLight();
		sun.setColor(new ColorRGBA(0.9f, 0.9f, 1f, 1f));
		sun.setDirection(new Vector3f(-0.3f, -0.6f, -0.5f).normalizeLocal());
		App.rally.getRootNode().addLight(sun);

		App.rally.getViewPort().setBackgroundColor(ColorRGBA.Blue);
	}
	
	private void buildPlayers() {
		Vector3f start = world.start;
		Matrix3f dir = new Matrix3f();
		
		cb = new CarBuilder();
		cb.addPlayer(getPhysicsSpace(), 0, car, start, dir, false);
	}
	
	private void initCamera() {
		camNode = new MyCamera("Cam Node 2", App.rally.getCamera(), null);
		App.rally.getRootNode().attachChild(camNode);
	}
	
	
	public void update(float tpf) {
		if (!isEnabled()) return; //appstate stuff
		super.update(tpf);
		
		camNode.myUpdate(tpf);
		
		MyPhysicsVehicle car = cb.get(0);
		Vector3f pos = car.getPhysicsLocation();
		car.setPhysicsLocation(new Vector3f(0, pos.y, 0));
	}
	
	public PhysicsSpace getPhysicsSpace() {
		return bulletAppState.getPhysicsSpace();
	}
	

	public void cleanup() {
		//TODO a lot:
		//lights, car, plane..
		Node root = App.rally.getRootNode();
		root.detachAllChildren();
		root.removeLight(sun);
		
		cb.cleanup();
	}
	
}
