package car.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

import helper.Duo;

public class CarDataConst implements Serializable {

	private static final long serialVersionUID = 2121279530710074151L;

	public String name;
	public ColorRGBA baseColor;

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

	////////
	//Drivetrain stuff
	public boolean driveFront, driveRear;
	
	public float[] e_torque; //starts at 0 rpm, steps at every 1000rpm
	public int e_redline;
	public int e_idle;
	public float e_compression; // is going to be multiplied by the RPM
	public float e_mass; // kg, this is the interia of the engine, 100 is high

    public float[] auto_gearUpSpeed; //m/s for triggering the next gear [calculated]
    public float[] auto_gearDownSpeed; // m/s for triggering the next gear [calculated]
	public float auto_changeTime;
	
	//NOTE: please check torque curves are at the crank before using this value as anything other than 1.0f
	public float trans_effic; //apparently 0.9 is common (power is lost to rotating the transmission gears)
	public float trans_finaldrive; //helps set the total drive ratio
	public float[] trans_gearRatios; //reverse,gear1,gear2,g3,g4,g5,g6,...

	public float trans_powerBalance; //Only used in all drive cars, 0 front <-> 1 rear

	public boolean nitro_on;
	public float nitro_force;
	public float nitro_rate;
	public float nitro_max;
	
	
	public float brakeMaxTorque;
	
	////////
	//Wheels
	public float w_steerAngle; //in radians
	
	//do not put wheel offset in the wheel obj, as they shouldn't know because the car determines their position
	public Vector3f[] wheelOffset;
	public WheelDataConst[] wheelData;
		
	//no idea category
	public float minDriftAngle;
	public Vector3f JUMP_FORCE;
	
	//empty constructor required for yaml loading
	public CarDataConst() {}

	////////////////////////////////////////////////////////
    //usefulMethods
    
    public CarSusDataConst susByWheelNum(int i) {
        return (i < 2 ? susF : susR);
    }
    
    public float getGearUpSpeed(int gear) {
        return auto_gearUpSpeed[gear];
    }
    public float getGearDownSpeed(int gear) {
        return auto_gearDownSpeed[gear];
    }
    public float speedAtRpm(int gear, int rpm) {
        return rpm / (trans_gearRatios[gear] * trans_finaldrive * (60 / FastMath.TWO_PI)) * driveWheelRadius();
    }
    public int rpmAtSpeed(int gear, float speed) {
        return (int)(speed * (trans_gearRatios[gear] * trans_finaldrive * (60 / FastMath.TWO_PI)) / driveWheelRadius());
    }

	// https://en.wikipedia.org/wiki/Automobile_drag_coefficient#Drag_area
	public Vector3f quadraticDrag(Vector3f velocity) {
		float dragx = -1.225f * areo_drag * areo_crossSection * velocity.x * FastMath.abs(velocity.x);
		float dragy = -1.225f * areo_drag * areo_crossSection * velocity.y * FastMath.abs(velocity.y);
		float dragz = -1.225f * areo_drag * areo_crossSection * velocity.z * FastMath.abs(velocity.z);
        // Optional: use a cross section for each xyz direction to make a realistic drag feeling
        // but what we would really need is angle of attack -> much harder
		return new Vector3f(dragx, dragy, dragz);
	}
	//linear drag component (https://en.wikipedia.org/wiki/Rolling_resistance)
	public float rollingResistance(int w_id, float susForce) {
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

		throw new IllegalStateException("No drive wheels set, no wheel radius found.");
    }
    
    @Override
    public String toString() {
        return this.name + " " + this.carModel;
    }
}
