package car.ray;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

import car.CarModelData;
import car.CarModelData.CarPart;
import helper.H.Duo;

public abstract class CarDataConst implements Serializable {
	protected static final String dir = "assets/models/";
	
	//model strings (can be xx.obj or xx.blend)
	public String carModel = dir+"car4.obj";
	public String wheelModel = dir+"wheel.obj";
	
	//camera options
	public Vector3f cam_lookAt = new Vector3f(0,1.3f,0); //top of car usually
	public Vector3f cam_offset = new Vector3f(0,2.1f,-6); //where the camera is
	public float cam_shake = 0.000002f;
	
	public float mass = 1200; //kg (total, do NOT add wheel or engine mass/inertia to this)
	public float width = 1.4f; //x size meter, door handle to door handle
	public float height = 1f; //y size meter, roof to ground
	public float length = 4f; //z size meter, from front to back

	public float rollFraction = 1; //TODO use in new physics //fake to allow high cars to not roll as much
	
	public float areo_drag = 1;
	public float areo_lineardrag = 0; //TODO use
	public float areo_crossSection = 0.47f; //m^2 front area
	public float areo_downforce = 0.0f; //not a default yet
	
	//all relative to 'wheel' offset pos
	public float sus_min_travel = -0.25f; //upper travel length - closer to car
	public float sus_max_travel = 0f; //lower travel length - closer to ground
	public float susTravel() { return sus_max_travel - sus_min_travel; }
	public float sus_preload_force = 3f/4; //spring pre-load Kg (Nm from gravity) //TODO tune (balanced against gravity /4)
	public float sus_stiffness = 20;//50f; //20-200
	public float sus_max_force = 50*mass;
	
	protected float sus_comp = 0.2f;
	protected float sus_relax = 0.3f;
	public float susCompression() { return sus_comp * 2 * FastMath.sqrt(sus_stiffness); }
	public float susRelax() { return sus_relax * 2 * FastMath.sqrt(sus_stiffness); }

	////////
	//Drivetrain stuff
	public boolean driveFront = false, driveRear = true; //this would be rear wheel drive
	
	//this one is from the notes, is a ~1999 corvette c6 
	public float[] e_torque = new float[]{0,390,445,460,480,475,360,10}; //starts at 0 rpm, steps every 1000rpm (until done)
	//found a porsche boxter engine curve:
//	public float[] e_torque = new float[]{0,223,250,280,300,310,280,245,10};
	
	public int auto_gearDown = 2400; //rpm triggering a gear down
	public int auto_gearUp = 5500; //rpm triggering a gear up
	public int e_redline = 6500;
	public int e_idle = 1000; 
	
	public float e_compression = 0.1f; //is going to be multiplied by the RPM
	public float e_mass = 30; //kg, this is the interia of the engine, 100 is high	
	
	//TODO check hoursepowercurves are on crank before using this as anything other than 1
	public float trans_effic = 0.85f; //apparently 0.9 is common (power is lost to rotating the transmission gears)
	public float trans_finaldrive = 3.0f; //helps set the total drive ratio
	public float[] trans_gearRatios = new float[]{-2.9f,3.6f,2.5f,1.8f,1.3f,1.0f,0.74f}; //reverse,gear1,gear2,g3,g4,g5,g6,...
	
	public boolean nitro_on = true;
	public float nitro_force = 300;
	public float nitro_rate = 1;
	public float nitro_max = 15;
	
	
	public float brakeMaxTorque = 4000;
	
	////////
	//Wheels
	public float w_steerAngle = 0.5f; //in radians
	public float w_difflock = 0.1f;
	//small=slip large=locked
	//0.0001f < x < 5 i think is a good range
	
	//do not put wheel offset in the wheel obj, as they shouldn't know because the car determines their position
	public Vector3f[] wheelOffset = new Vector3f[4];
	public WheelDataConst[] wheelData = new WheelDataConst[4];
		
	//no idea category
	public float minDriftAngle = 7;
	public Vector3f JUMP_FORCE = new Vector3f(0, mass, 0);
	
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

		postLoad();
		
		modelLoad();
		
		//load defaults when data not set by modelLoad or postLoad 
		if (wheelOffset[0] == null) { 
			float x_off = 0.68f;
			float y_off = 0;//TODO try using large wheel offsets (any axis) and the car feels very wrong
			float z_off = 1.1f;
			for (int i = 0; i < wheelOffset.length; i++) {
				setWheelOffset(i, x_off, y_off, z_off);
			}
		}
		if (wheelData[0] == null) {
			for (int i = 0; i < wheelData.length; i++) {
				WheelDataTractionConst wLat = new WheelDataTractionConst(10f, 1.9f, 1f, 0.97f);
				WheelDataTractionConst wLong = new WheelDataTractionConst(10f, 1.9f, 1f, 0.97f);
				wheelData[i] = new WheelDataConst(wheelModel, 0.3f, 25, 0.15f, wLat, wLong);
			}
		}
	}
	protected final void setWheelOffset(int i, float x_off, float y_off, float z_off) {
		wheelOffset[i] = new Vector3f(x_off, y_off, z_off);
		wheelOffset[i].x *= i % 2 == 0 ? -1 : 1;
		wheelOffset[i].z *= i < 2 ? 1 : -1;
	}
	
	private void modelLoad() {
		//init car pos things based on the physical model

		CarModelData modelData = new CarModelData(this.carModel, this.wheelModel);
		if (modelData.foundSomething() && modelData.foundAllWheels()) {
			wheelOffset[0] = modelData.getPosOf(CarPart.Wheel_FL);
			wheelOffset[1] = modelData.getPosOf(CarPart.Wheel_FR);
			wheelOffset[2] = modelData.getPosOf(CarPart.Wheel_RL);
			wheelOffset[3] = modelData.getPosOf(CarPart.Wheel_RR);
		}
	}
	
	protected abstract void postLoad();

	////////////////////////////////////////////////////////
	//usefulMethods
	
	//linear drag component (https://en.wikipedia.org/wiki/Rolling_resistance)
	protected float rollingResistance(float gravity, int w_id) {
		return gravity*mass*areo_lineardrag/wheelData[w_id].radius;
	}
	
	public float e_inertia(int w_id) { //car internal engine + wheel inertia
		float wheels = (wheelData[w_id].mass*wheelData[w_id].radius*wheelData[w_id].radius/2);
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
//and some proper stiffer suspension