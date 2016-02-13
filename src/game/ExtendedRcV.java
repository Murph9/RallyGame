package game;

import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.vehicle.RaycastVehicle;
import com.bulletphysics.dynamics.vehicle.VehicleRaycaster;

//copied from:
//https://github.com/bubblecloud/jbullet/blob/master/src/main/java/com/bulletphysics/dynamics/vehicle/RaycastVehicle.java
//so i can add my own physics

public class ExtendedRcV extends RaycastVehicle {

	public ExtendedRcV(ExtendedVT tuning, RigidBody chassis, VehicleRaycaster raycaster) {
		super(tuning.vt, chassis, raycaster);
	}
	
	//TODO extend methods so that I can write 'extended' ones
	
}
