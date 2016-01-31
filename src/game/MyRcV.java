package game;

import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.vehicle.RaycastVehicle;
import com.bulletphysics.dynamics.vehicle.VehicleRaycaster;
import com.bulletphysics.dynamics.vehicle.VehicleTuning;

//copied from:
//https://github.com/bubblecloud/jbullet/blob/master/src/main/java/com/bulletphysics/dynamics/vehicle/RaycastVehicle.java
//so i can add my own physics

public class MyRcV extends RaycastVehicle {

	public MyRcV(VehicleTuning tuning, RigidBody chassis, VehicleRaycaster raycaster) {
		super(tuning, chassis, raycaster);
	}
	
	//TODO extend methods so that I can write 'better' ones
	
}
