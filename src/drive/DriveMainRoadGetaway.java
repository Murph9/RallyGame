package drive;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

import car.ai.CarAI;
import car.ai.DriveAlongAI;
import car.data.Car;
import car.ray.RayCarControl;
import game.App;
import world.wp.WP.DynamicType;

public class DriveMainRoadGetaway extends DriveBase {

	//single straight highway with traffic cars that just stay in their lane
	//one car trying to slow you down
	
	private final Car chaser;
	private final int maxCount;
	private int nextId;
	
	
	public DriveMainRoadGetaway(Car car) {
		super(car, DynamicType.MainRoad.getBuilder());
		
		maxCount = 7; //6 + me + chaser
		chaser = Car.Rally;
	}
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		
		//add the chase car
		this.cb.addCar(1, chaser, world.getStartPos().add(0, 0, -10), world.getStartRot(), false, null);
		nextId = 2;
	}
	
	@Override
	public void update(float tpf) {
		super.update(tpf);
		
		if (this.cb.getCount() < maxCount && App.rally.frameCount % 60 == 0) {
			final float z = xOffGivenCarNum(this.cb.getCount());
			final boolean posDir = dirGivenCarNum(this.cb.getCount()); //TODO use when they go in reverse
			BiFunction<RayCarControl, RayCarControl, CarAI> ai = (me, target) -> new DriveAlongAI(me, (vec) -> {
				return new Vector3f(vec.x+20, 0, z);
			}); //next pos math
			
			//calculate spawn pos
			RayCarControl c = this.cb.addCar(nextId, Car.Normal, new Vector3f(this.cb.get(0).getPhysicsLocation().x - 5, 0, z), world.getStartRot(), false, ai);
			c.setLinearVelocity(new Vector3f(100, 0, 0));
			nextId++;
		}

		
		//check if any traffic is falling and remove them
		List<RayCarControl> toKill = new ArrayList<RayCarControl>();
		for (RayCarControl c: this.cb.getAll()) {
			if (c.up != null && (c.getPhysicsLocation().y < -10 || c.up.y < 0) && c != this.cb.get(0)) {
				toKill.add(c);
			}
		}
		for (RayCarControl c: toKill) {
			//killed (out of loop)
			cb.removeCar(c);
		}
	}
	
	private float xOffGivenCarNum(int i) {
		if (i == 7)
			return -8f;
		if (i == 6)
			return -4.75f;
		if (i == 5)
			return -1.5f;
		if (i == 4)
			return 1.5f;
		if (i == 2)
			return 4.75f;
		if (i == 3)
			return 8f;
		return 0;
	}
	private boolean dirGivenCarNum(int i) {
		return i > 0;
	}
}
