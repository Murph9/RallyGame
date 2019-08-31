package drive;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import car.ai.CarAI;
import car.ai.DriveAlongAI;
import car.data.Car;
import car.ray.RayCarControl;
import game.App;
import helper.H;
import world.wp.DefaultBuilder;
import world.wp.WP.DynamicType;

public class DriveMainRoadGetaway extends DriveBase {

	// goal:
	// single straight highway with traffic cars that just stay in their lane (going both ways)
	// one (heavier) car trying to slow you down
	// more traffic/higher speed as the game goes on

	//Other options for when to kill the player:
	// - touching the chase car
	// - stopping
	// - hitting traffic

	private static final int TRAFFIC_COUNT = 60;
	private static final float TRAFFIC_BUFFER = 100;
	private static final float HUNTER_BUFFER = 25;

	private final Car hunterType;
	private final Car[] trafficTypes;
	private final int maxCount;

	private RayCarControl hunter;

	private boolean readyToSpawnTraffic = false;
	private boolean readyForHunter = false;

	private float life;

	// GUI objects
	private Container display;
	private Label scoreLabel;
	private Label lifeLabel;
	
	public DriveMainRoadGetaway() {
		super(Car.Runner, DynamicType.MainRoad.getBuilder());
		
		maxCount = 2 + TRAFFIC_COUNT; // me chaser + x
		hunterType = Car.Rocket;
		trafficTypes = new Car[] {Car.Ricer, Car.WhiteSloth};

		life = 10; // seconds of 'slowness' is your life
	}
	
	@Override
	public void initialize(Application app) {
		super.initialize(app);
		
		//add the chase car
		hunter = this.cb.addCar(hunterType, new Vector3f(HUNTER_BUFFER - 10, 0.3f, 0), world.getStartRot(), false, null);

		display = new Container();
		display.addChild(new Label("Score: "));
		scoreLabel = display.addChild(new Label("0"), 1);
		display.addChild(new Label("Life: "));
		lifeLabel = display.addChild(new Label("0"), 1);
		
		AppSettings set = app.getContext().getSettings();
		display.setLocalTranslation(new Vector3f(set.getWidth() / 2, set.getHeight(), 0));
		((SimpleApplication)app).getGuiNode().attachChild(display);
	}
	
	@Override
	public void update(float tpf) {
		super.update(tpf);
		
		RayCarControl player = this.cb.get(0);
		RayCarControl chaser = this.cb.get(1);

		float distanceFromStart = getPlayerDistanceFromStart(player);
		float playerSpeed = player.getCurrentVehicleSpeedKmHour();

		this.scoreLabel.setText(((int) distanceFromStart)+"");
		this.lifeLabel.setText(((int) (life*100))+"");

		//environment:
		readyForHunter = distanceFromStart > HUNTER_BUFFER;
		if (!readyForHunter) {
			// the chase car must stay still, for now
			hunter.setPhysicsLocation(new Vector3f(HUNTER_BUFFER-10, 0.3f, 0));
			hunter.setPhysicsRotation(this.world.getStartRot());
			return;
		}

		readyToSpawnTraffic = distanceFromStart > TRAFFIC_BUFFER;
		if (!readyToSpawnTraffic)
			return;

		//spawn more if required
		if (this.cb.getCount() < maxCount && App.getFrameCount() % 30 == 0) {
			final float z = nextZOff();
			final boolean spawnPlayerDirection = spawnPlayerDirection();
			
			BiFunction<RayCarControl, RayCarControl, CarAI> ai = (me, target) -> { 
				DriveAlongAI daai = new DriveAlongAI(me, (vec) -> {
					return new Vector3f(vec.x+20*(spawnPlayerDirection ? 1: -1), 0, z); // next pos math
				});
				daai.setMaxSpeed(27.777f); //100km/h
				return daai;
			};
			
			//closest piece from the start of the end of the world (as its always increasing in a straight line down positive x)
			//and then subtract a bit so that it doesn't fall off
			Vector3f spawnPos = ((DefaultBuilder) this.world).getNextPieceClosestTo(new Vector3f(100000, 0, 0))
					.add(-20, 0, z); //offset
			Matrix3f spawnDir = spawnPlayerDirection
				? world.getStartRot()
				: new Quaternion().fromAngleAxis(FastMath.PI, Vector3f.UNIT_Y).toRotationMatrix().mult(world.getStartRot());

			RayCarControl c = this.cb.addCar(H.randFromArray(this.trafficTypes), spawnPos, spawnDir, false, ai);
			c.setLinearVelocity(new Vector3f(20*(spawnPlayerDirection ? 1: -1), 0, 0));
		}
		
		//check if any traffic is falling and remove them
		List<RayCarControl> toKill = new ArrayList<RayCarControl>();
		for (RayCarControl c: this.cb.getAll()) {
			if (c == player) continue; 
			
			if (c.getPhysicsLocation().y < -10) {
				if (c == chaser) {
					//we only reset their position behind the player at the same speed, please don't delete them
					c.setPhysicsLocation(player.getPhysicsLocation().add(-15, 0, 0));
					c.setPhysicsRotation(player.getPhysicsRotation());
					c.setLinearVelocity(player.getLinearVelocity());
					c.setAngularVelocity(player.getAngularVelocity());
					continue;
				}
			
				toKill.add(c);
			}
		}
		for (RayCarControl c: toKill) {
			//killed (out of loop)
			cb.removeCar(c);
		}


		// scoring:
		if (readyToSpawnTraffic && playerSpeed < 10) {
			// loaded and ready to lose score
			life -= tpf;
		}

		if (life < 0) {
			//TODO something..
		}
	}
	
	private boolean spawnPlayerDirection() {
		return FastMath.rand.nextBoolean();
	}
 
	private float nextZOff() {
		int i = FastMath.nextRandomInt(0, 5);
		if (i == 5)
			return -8f;
		else if (i == 4)
			return -4.75f;
		else if (i == 3)
			return -1.5f;
		else if (i == 2)
			return 1.5f;
		else if (i == 1)
			return 4.75f;
		else if (i == 0)
			return 8f;
		else 
			return 8f;
	}

	private float getPlayerDistanceFromStart(RayCarControl car) {
		return FastMath.abs(this.world.getStartPos().x - car.getPhysicsLocation().x);
	}
}
