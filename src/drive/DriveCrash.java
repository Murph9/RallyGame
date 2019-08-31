package drive;

import world.World;

import java.util.ArrayList;
import java.util.List;

import com.jme3.math.Vector3f;

import car.data.Car;
import car.ray.RayCarControl;
import helper.H;

public class DriveCrash extends DriveBase {

	private static Vector3f[] spawns = new Vector3f[] {
			new Vector3f(0,0,0),
			new Vector3f(0,10,0),
			new Vector3f(5,5,0)
	};

	private Car them;
	private int maxCount;
	
	private int totalKilled;
	
	private int frameCount = 0; //global frame timer

	public DriveCrash (World world) {
    	super(Car.Runner, world);
    	this.them = Car.Rally;
    	this.maxCount = 10;
    	this.totalKilled = 0;
    }
	
	public void update(float tpf) {
		super.update(tpf);
		frameCount++;
		
		if (this.cb.getCount() < maxCount && frameCount % 60 == 0) {
			Vector3f spawn = H.randFromArray(spawns);
			this.cb.addCar(them, spawn, world.getStartRot(), false, null);
		}
		
		//check if any are upside down, if so kill them
		//TODO check if collisions are from the player
		List<RayCarControl> toKill = new ArrayList<RayCarControl>();
		for (RayCarControl c: this.cb.getAll()) {
			if (c.up != null && c.up.y < 0 && c != this.cb.get(0)) {
				toKill.add(c);
			}
		}
		for (RayCarControl c: toKill) {
			//killed
			totalKilled++;
			cb.removeCar(c);
		}

		
		if (this.menu.randomthing != null)
			this.menu.randomthing.setText("Total Killed: " + totalKilled);
	}
}
