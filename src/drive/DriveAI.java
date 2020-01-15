package drive;

import world.StaticWorld;
import world.StaticWorldBuilder;
import world.World;

import com.jme3.app.Application;

import car.ai.DriveAtAI;
import car.data.Car;
import car.ray.RayCarControl;
import game.IDriveDone;

public class DriveAI extends DriveBase {
	
	private Car them;
	private int themCount;

	public DriveAI(IDriveDone done) {
    	super(done, Car.Runner, (World)new StaticWorldBuilder(StaticWorld.track2));
    	this.them = Car.Runner;
    	this.themCount = 3;
    }
	
	@Override
	public void initialize(Application app) {
        super.initialize(app);
        
    	for (int i = 0; i < this.themCount; i++) {
			RayCarControl c = this.cb.addCar(them, world.getStartPos(), world.getStartRot(), false);
			c.attachAI(new DriveAtAI(c, this.cb.get(0).getPhysicsObject()), true);
		}
	}
	
	public void update(float tpf) {
		super.update(tpf);
	}
}
