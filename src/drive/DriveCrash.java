package drive;

import world.World;

import java.util.ArrayList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Vector3f;

import car.*;
import game.App;
import helper.H;

public class DriveCrash extends DriveBase {

	private static Vector3f[] spawns = new Vector3f[] {
			new Vector3f(0,0,0)
	};

	private CarData them;
	private int maxCount;
	private int nextId;
	
	private int totalKilled;

	public DriveCrash (World world) {
    	super(Car.Runner.get(), world);
    	this.them = Car.Rally.get();
    	this.maxCount = 30;
    	this.nextId = 1;
    	this.totalKilled = 0;
    }
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
    	super.initialize(stateManager, app);
	}
	
	public void update(float tpf) {
		super.update(tpf);
		
		if (this.cb.getCount() < maxCount && App.rally.frameCount % 60 == 0) {
			Vector3f spawn = H.randFromArray(spawns);
			this.cb.addCar(nextId, them, spawn, world.getStartRot(), false, null);
			nextId++;
		}
		
		//check if any are upside down, if so kill them
		//TODO check if collisions are from the player
		List<MyPhysicsVehicle> toKill = new ArrayList<MyPhysicsVehicle>();
		for (MyPhysicsVehicle c: this.cb.getAll()) {
			if (c.up.y < 0 && c != this.cb.get(0)) {
				toKill.add(c); //TODO
			}
		}
		for (MyPhysicsVehicle c: toKill) {
			//killed
			totalKilled++;
			cb.removePlayer(c);
		}
			
		
		if (this.menu.randomthing != null)
			this.menu.randomthing.setText("Total Killed: " + totalKilled);
	}
}
