package game;

import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.vehicle.RaycastVehicle;
import com.bulletphysics.dynamics.vehicle.VehicleRaycaster;

import javax.vecmath.Vector3f;

//copied from:
//https://github.com/bubblecloud/jbullet/blob/master/src/main/java/com/bulletphysics/dynamics/vehicle/RaycastVehicle.java

//For writing the physics engine.

public class ExtendedRcV extends RaycastVehicle {

	Vector3f fwd = new Vector3f();
	
	public ExtendedRcV(ExtendedVT tuning, RigidBody chassis, VehicleRaycaster raycaster) {
		super(tuning.vt, chassis, raycaster);
	}
	
	//TODO extend methods so that I can write 'extended' ones
	
	
	public void updateFriction(float timeStep) {
		//don't need it
	}
	
	//TODO there are problems with overwriting methods because they are private (not protected)
}


/*

private float getEngineWheelForce(float wheelrot, float tpf) {
float curGearRatio = car.gearRatios[curGear];//0 = reverse, >= 1 normal make sense
float diffRatio = car.diffRatio;
curRPM = (int)(wheelrot*curGearRatio*diffRatio*60); //rad/sec to rad/min and the drive ratios to engine
	//wheel rad/s, gearratio, diffratio, conversion from rad/sec to rad/min

redlineKillFor -= tpf;

if (redlineKillFor > 0) {
	return 0;
}

if (Math.abs(curRPM) > car.redline) {
	redlineKillFor = car.redlineCutTime;
	return 0; //kill engine if greater than redline
}

autoTransmission(curRPM);

float engineTorque = lerpTorque(curRPM);
float driveTorque = engineTorque*curGearRatio*diffRatio*car.transEffic;

float totalTorque = driveTorque/car.wheelRadius;
return totalTorque;
}
/////////////////////////////

private void autoTransmission(int rpm) {
	if (curGear == 0) return; //no changing out of reverse on me please..
	
	if (rpm > car.gearUp && curGear < car.gearRatios.length-1) {
		curGear++;
	} else if (rpm < car.gearDown && curGear > 1) {
		curGear--;
	}
}

private float lerpTorque(int rpm) {
	if (rpm < 1000) rpm = 1000; //prevent stall values
	float RPM = (float)rpm / 1000;
	return H.lerpArray(RPM, car.torque);
}


*/