package drive;

import world.World;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;

import car.ray.CarDataConst;

public class DriveAI extends DriveBase {
	
	private CarDataConst them;
	private int themCount;

	public DriveAI (CarDataConst car, CarDataConst them, World world) {
    	super(car, world);
    	this.them = them;
    	this.themCount = 3;
    }
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
    	super.initialize(stateManager, app);
    	
    	for (int i = 0; i < this.themCount; i++)
    		this.cb.addCar(i + 1, them, world.getStartPos(), world.getStartRot(), false, null);
	}
	
	public void update(float tpf) {
		super.update(tpf);
	}
}
