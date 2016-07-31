package car;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

import game.App;
import game.H;
import world.WorldType;

public class AI {

	WorldType type;
	MyPhysicsVehicle car;

	AI (MyPhysicsVehicle car) {
		this.car = car;
		this.type = App.rally.drive.type;
	}

	public void update(float tpf) {
	

		Vector3f pos = car.getPhysicsLocation();
		MyPhysicsVehicle player1 = App.rally.drive.cb.get(0);
		Vector3f player1Pos = player1.getPhysicsLocation();
		
		//move towards player 1
		Vector3f target;
		if (type == WorldType.DYNAMIC) {
			target = App.rally.drive.worldB.getNextPieceClosestTo(pos);
			if (target == null) {
				target = player1Pos;
			}
			
		} else {
			target = player1Pos;//.add(player1.getLinearVelocity().normalize());
		}
		
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
		
		//TODO some kind of ray cast so they can drive properly
		//yeah..
		
		//hack so they don't lose too bad
		
		if (player1Pos.y - pos.y > 50 || player1Pos.subtract(pos).length() > 500) {
			car.setPhysicsLocation(player1Pos.add(4, 1, 0)); //spawn 3 up and left of me
			car.setLinearVelocity(player1.getLinearVelocity()); //and give them my speed
			car.setPhysicsRotation(player1.getPhysicsRotation()); //and rotation
			car.setAngularVelocity(player1.getAngularVelocity()); //and rot angle
			H.p("respawned at " + player1.getPhysicsLocation());
		}
	}

	public void onEvent(String act, boolean ifdown, float amnt) {
		car.onAction(act, ifdown, amnt);
	}
}
