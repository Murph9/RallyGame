package car.ray;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.jme3.math.Vector3f;

import helper.H.Duo;

public class CarDataConst implements Serializable {

	private static final long serialVersionUID = 2121279530710074151L;

	// model strings (can be xx.obj or xx.blend)
	public String carModel;
	public String wheelModel;
	
	//camera options
	public float cam_lookAtHeight; //from the middle of the model up
	public float cam_offsetLength; //from the middle of the model back
	public float cam_offsetHeight; //from the middle of the model up
	public float cam_shake;
	
	public float mass; //kg (total, do NOT add wheel or engine mass/inertia to this)

	public float areo_drag;
	public float areo_lineardrag; //0.003 to 0.02 (dimensionless number)
	public float areo_crossSection; //m^2 front area
	public float areo_downforce; //not a default yet
	
	//travel values are relative to wheel offset pos
	public CarSusDataConst susF;
	public CarSusDataConst susR;
	public CarSusDataConst susByWheelNum(int i) {
		return (i < 2 ? susF : susR);
	}

	////////
	//Drivetrain stuff
	public boolean driveFront, driveRear;
	
	//this one is from the notes, is a ~1999 corvette c6 
	public float[] e_torque; //starts at 0 rpm, steps every 1000rpm (until done)
	//found a porsche boxter engine curve:
//	public float[] e_torque = new float[]{0,223,250,280,300,310,280,245,10};
	
	public int auto_gearDown; //rpm triggering a gear down
	public int auto_gearUp; //rpm triggering a gear up
	public float auto_changeTime;
	public int e_redline;
	public int e_idle; 
	
	public float e_compression; //is going to be multiplied by the RPM
	public float e_mass; //kg, this is the interia of the engine, 100 is high	
	
	//NOTE: please check hoursepowercurves are crank before using this value as anything other than 1.0f
	public float trans_effic; //apparently 0.9 is common (power is lost to rotating the transmission gears)
	public float trans_finaldrive; //helps set the total drive ratio
	public float[] trans_gearRatios; //reverse,gear1,gear2,g3,g4,g5,g6,...
	
	public boolean nitro_on;
	public float nitro_force;
	public float nitro_rate;
	public float nitro_max;
	
	
	public float brakeMaxTorque;
	
	////////
	//Wheels
	public float w_steerAngle; //in radians
	
	public float w_cdifflock;
	public boolean w_fdiff;
	public float w_fdifflock; //small=slip large=locked -> 0.0001f < x < 5 i think is a good range
	public boolean w_rdiff;
	public float w_rdifflock;
	
	//do not put wheel offset in the wheel obj, as they shouldn't know because the car determines their position
	public Vector3f[] wheelOffset;
	public WheelDataConst[] wheelData;
		
	//no idea category
	public float minDriftAngle;
	public Vector3f JUMP_FORCE;
	
	
	
	public CarDataConst() {} 


	////////////////////////////////////////////////////////
	//usefulMethods
	
	//linear drag component (https://en.wikipedia.org/wiki/Rolling_resistance)
	protected float rollingResistance(int w_id, float susForce) {
		return susForce*areo_lineardrag/wheelData[w_id].radius;
	}
	
	public float wheel_inertia(int w_id) {
		//NOTE: PERF: this is a disc, pls make a thicc pipe so its closer to real life
		return wheelData[w_id].mass*wheelData[w_id].radius*wheelData[w_id].radius/2;
	}
	public float e_inertia() { //car internal engine + wheel inertia
		
		float wheels = 0;
		for (int i = 0; i < wheelData.length; i++) 
			wheels += wheel_inertia(i);
		
		if (driveFront && driveRear) {
			return e_mass + wheels*4;
		}
		return e_mass + wheels*2;
	}
	
	//compute the torque at rpm
	//assumed to be a valid and useable rpm value
	public float lerpTorque(int rpm) {
		float RPM = (float)rpm / 1000;
		return helper.H.lerpArray(RPM, this.e_torque);
	}
	
	//get the max power and rpm
	public Duo<Float, Float> getMaxPower() {
		float max = 0;
		float maxrpm = 0;
		for (int i = 0; i < e_torque.length; i++) {
			float prevmax = max;
			max = Math.max(max, e_torque[i]*(1000*i)/9549);
			if (prevmax != max) maxrpm = i;
		} //http://www.autospeed.com/cms/article.html?&title=Power-versus-Torque-Part-1&A=108647
		return new Duo<Float, Float>(max, maxrpm*1000);
	}
	
	public CarDataConst cloneWithSerialization() {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(this);
	
			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
			ObjectInputStream ois = new ObjectInputStream(bais);
			return (CarDataConst) ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	public float driveWheelRadius() {
		if (driveFront && driveRear)
			return (wheelData[0].radius + wheelData[1].radius + wheelData[2].radius + wheelData[3].radius) / 4f;
		if (driveFront)
			return (wheelData[0].radius + wheelData[1].radius) / 2f;
		if (driveRear)
			return (wheelData[2].radius + wheelData[3].radius) / 2f;

		return 1;
	}
}


//Some helpful notes
//you can fix the oversteer at high speeds with a proper diff and different downforce scaling at higher speeds
//and some stiffer front suspension?