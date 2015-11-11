package game;

import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.VehicleControl;

public class MyVehicleControl extends VehicleControl {
	
	float w0_myskid;
	float w1_myskid;
	float w2_myskid;
	float w3_myskid;
	
	MyVehicleControl(CollisionShape col, float mass) {
		super(col, mass);
	}
}
