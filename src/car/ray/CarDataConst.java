package car.ray;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.jme3.math.Vector3f;

import car.CarModelData;
import car.CarModelData.CarPart;
import helper.Log;
import helper.H.Duo;

public class CarDataConst implements Serializable {

	//model strings (can be xx.obj or xx.blend)
	public String carModel;
	public String wheelModel;
	
	//camera options
	public Vector3f cam_lookAt; //top of car usually
	public Vector3f cam_offset; //where the camera is
	public float cam_shake;
	
	public float mass; //kg (total, do NOT add wheel or engine mass/inertia to this)
	public float width; //x size meter, door handle to door handle
	public float height; //y size meter, roof to ground
	public float length; //z size meter, from front to back

	public float rollFraction; //TODO use in new physics //fake value to allow high cars to not roll as much as they should
	
	public float areo_drag;
	public float areo_lineardrag; //TODO use
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
	
	//TODO check hoursepowercurves are on crank before using this as anything other than 1
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
	public boolean w_diff;
	public float w_difflock; //small=slip large=locked -> 0.0001f < x < 5 i think is a good range TODO pls verify
	
	//do not put wheel offset in the wheel obj, as they shouldn't know because the car determines their position
	public Vector3f[] wheelOffset;
	public WheelDataConst[] wheelData;
		
	//no idea category
	public float minDriftAngle;
	public Vector3f JUMP_FORCE;
	
	
	
	public CarDataConst() {} 

	
	public boolean loaded = false; //TODO use?
	public final void load() { //final = no override pls
		if (loaded) {
			try {
				throw new Exception("Car data loaded twice");
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}
		
		loaded = true;

		modelLoad();
		
		//TODO value validations because they are now read from file :(
	}
	
	private void modelLoad() {
		//init car pos things based on the physical model

		//TODO make model data mandatory
		
		CarModelData modelData = new CarModelData(this.carModel, this.wheelModel);
		if (modelData.foundSomething() && modelData.foundAllWheels()) {
			wheelOffset = new Vector3f[4];
			wheelOffset[0] = modelData.getPosOf(CarPart.Wheel_FL);
			wheelOffset[1] = modelData.getPosOf(CarPart.Wheel_FR);
			wheelOffset[2] = modelData.getPosOf(CarPart.Wheel_RL);
			wheelOffset[3] = modelData.getPosOf(CarPart.Wheel_RR);
		} else {
			Log.e("!!! Missing car model wheel position data for: " + this.carModel);
			System.exit(-50203);
		}
	}

	////////////////////////////////////////////////////////
	//usefulMethods
	
	//linear drag component (https://en.wikipedia.org/wiki/Rolling_resistance)
	protected float rollingResistance(float gravity, int w_id) {
		return gravity*mass*areo_lineardrag/wheelData[w_id].radius;
	}
	
	public float wheel_inertia(int w_id) {
		//TODO this is a disc, pls make a thicc pipe so its closer to real life
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
}


//TODO:
//Some helpful notes

//you can fix the oversteer at high speeds with a proper diff and different downforce scaling at higher speeds
//and some stiffer front suspension?