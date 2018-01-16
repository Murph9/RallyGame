package car.ai;

import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;

import car.MyPhysicsVehicle;
import game.DriveRace;
import helper.H;

public class RaceAI extends CarAI {

	private DriveRace race;
	
	public RaceAI(MyPhysicsVehicle car, MyPhysicsVehicle notused, DriveRace race) {
		super(car);
		this.race = race;
	}

	@Override
	public void update(float tpf) {
		Vector3f pos = car.getPhysicsLocation();
		Vector3f atPos = race.getNextCheckpoint(this, pos);
		if (atPos == null) {
			H.e("at pos was null though?");
			return; //don't know what to do
		}
		
		Matrix3f w_angle = car.getPhysicsRotationMatrix();
		Vector3f velocity = w_angle.invert().mult(car.vel);
		int reverse = (velocity.z < 0 ? -1 : 1);
		
		Vector3f myforward = new Vector3f();
		car.getForwardVector(myforward);
		
		float angF = myforward.normalize().angleBetween((atPos.subtract(pos)).normalize());
		float ang = car.left.normalize().angleBetween((atPos.subtract(pos)).normalize());
		
		//get attempted turn angle as pos or negative
		float nowTurn = angF*Math.signum(FastMath.HALF_PI-ang);
		
		//turn towards 
		if (nowTurn < 0) {
			onEvent("Left", false, 0);
			onEvent("Right", true, Math.abs(nowTurn)*reverse);
		} else {
			onEvent("Right", false, 0);
			onEvent("Left", true, Math.abs(nowTurn)*reverse);
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
		if (car.getLinearVelocity().length() > 20) {
			onEvent("Accel", false, 0); //don't go too fast
			onEvent("Brake", false, 0);
		}
		
		//TODO some kind of ray cast so they can drive around things at all
	}
}
