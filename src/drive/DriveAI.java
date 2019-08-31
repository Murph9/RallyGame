package drive;

import world.StaticWorld;
import world.StaticWorldBuilder;
import world.World;

import com.jme3.app.Application;

import car.data.Car;
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
    	
    	for (int i = 0; i < this.themCount; i++)
    		this.cb.addCar(them, world.getStartPos(), world.getStartRot(), false, null);
	}
	
	public void update(float tpf) {
		super.update(tpf);
	}
}
