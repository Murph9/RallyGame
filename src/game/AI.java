package game;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

public class AI {

	MyPhysicsVehicle car;

	AI (MyPhysicsVehicle car) {
		this.car = car;
	}

	public void update(float tpf) {
		//i think aim at player wouldn't be a bad one
		
		//second move towards player 1
		MyPhysicsVehicle player1 = App.rally.drive.cb.get(0);
		Vector3f target = player1.getPhysicsLocation();//.add(player1.getLinearVelocity().normalize());
		
		Vector3f pos = car.getPhysicsLocation();
		
		Vector3f myforward = new Vector3f();
		car.getForwardVector(myforward);
		
		float angF = myforward.normalize().angleBetween((target.subtract(pos)).normalize());
		float ang = car.left.normalize().angleBetween((target.subtract(pos)).normalize());
		
		float turndeg = (angF > FastMath.QUARTER_PI) ? 1 : angF/FastMath.QUARTER_PI;
		
		//turn towards player
		if (ang > FastMath.HALF_PI) {
			onEvent("Left", false, 0);
			onEvent("Right", true, turndeg);
		} else {
			onEvent("Right", false, 0);
			onEvent("Left", true, turndeg);
		}
		//slow down to turn
		if (FastMath.abs(angF) < FastMath.QUARTER_PI) {
			onEvent("Brake", false, 0);
			onEvent("Accel", true, 1);
		} else {
			onEvent("Brake", true, 1);
			onEvent("Accel", false, 0);
		}
		
		//if going to slow speed up
		if (car.getLinearVelocity().length() < 10) {
			onEvent("Accel", true, 1);
			onEvent("Brake", false, 0);
			
			if (car.getLinearVelocity().length() < 1 && car.up.y < 0) { //very still
				onEvent("Flip", true, 1);
			}
		}
	}

	public void onEvent(String act, boolean ifdown, float amnt) {
		car.onAction(act, ifdown, amnt);
	}
}
