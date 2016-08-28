package car;

import com.jme3.math.Vector3f;

public enum Car {
	Normal(new NormalCar()),
	Normalf(new NormalFCar()),
	Rally(new RallyCar()),
	Track(new TrackCar()),
	Rocket(new Rocket()),
	Runner(new Runner()),
	Hunter(new Hunter()),
	Ricer(new Ricer()),
	
//	Test(new Test()),
	;
	
	private CarData car;
	Car(CarData car) {
		this.car = car;
	}
	
	public CarData get() {
		return car;
	}
	
	private static class NormalCar extends CarData {
		//for using the default settings.
		//probably shouldn't have a custom constructor
		NormalCar() {}
	}

	/*
	private static class Test extends CarData {
		Test() {
			carModel = dir+"base.blend";
		}
	}
	*/
	
	private static class Ricer extends CarData {
		//for using the default settings.
		//probably shouldn't have a custom constructor
		Ricer() {
			wheelModel = dir+"wheel3.blend";
			carModel = dir+"ricer.blend";
			
			//trying my best: http://www.cars-data.com/en/nissan-200-sx-turbo-specs/26930
			mass = 1240;
			width = 1.74f;
			height = 1.295f;
			length = 4.52f;
			
			rollFraction = 0.1f;
			w_steerAngle += 0.25f;
			
			w_zOff = 1.32f;
			w_yOff = -0.15f;
			setw_Pos();
			
			//https://en.wikipedia.org/wiki/Automobile_drag_coefficient
			areo_drag = 0.33f;

			driveFront = false;
			driveRear = true;
			
			//http://www.automobile-catalog.com/curve/1991/2179700/nissan_200_sx_turbo_16v.html
			e_torque = new float[] {0, 50, 150, 210, 226, 223, 200, 140};

			auto_gearDown = 3500;
			auto_gearUp = 6500;
			e_redline = 6800;
			
			//TODO testing
			e_compression = 0;
			e_mass = 20;
			
			trans_effic = 0.85f;
			trans_finaldrive = 3.69f;//final drive maybe?
			trans_gearRatios = new float[] { -3.38f, 3.32f, 1.9f, 1.31f, 1f, 0.84f};
			
			sus_stiffness = 40.0f; //20 is fairly stiff
			sus_compValue = 0.5f; //(should be lower than damp)
			sus_dampValue = 0.6f;
			sus_restLength = 0.2f;

			sus_maxForce = mass*4*9.81f;

			
		}
	}

	
	private static class NormalFCar extends CarData {
		//Front wheel drive car
		NormalFCar() {
			driveFront = true;
			driveRear = false;
		}
	}

	private static class RallyCar extends CarData {
		RallyCar() {
			carModel = dir+"car4raid_1.obj";
			wheelModel = dir+"wheelraid1.obj";

			mass = 1400;
			areo_drag = 0.7f;
			
			w_width = 0.25f;
			w_radius = 0.4f;

			driveFront = true;
			driveRear = true;

			w_xOff = 0.7f;
			w_yOff = 0.2f;
			w_zOff = 1.1f;
			setw_Pos();

			sus_stiffness  = 35.0f;
			sus_restLength = 0.15f;
			sus_compValue  = 0.2f;
			sus_dampValue  = 0.2f;
			rollFraction = 0.6f;
			sus_maxForce = 25000;

			e_torque = new float[]{0,520,580,620,680,720,870,820,0};

			auto_gearDown = 3500;
			auto_gearUp = 6800;
			e_redline = 7200;

			trans_effic = 0.75f;
			trans_finaldrive = 2.5f;
			trans_gearRatios = new float[]{-3.5f,3.0f,2.3f,1.6f,1.2f,0.87f,0.7f};
			
			w_flatdata = new RallyLatWheel();
			w_flongdata = new RallyLongWheel();
		}
	}

	private static class TrackCar extends CarData {
		TrackCar() {
			carModel = dir+"f1.blend";
			wheelModel = dir+"f1_wheel.blend";
			cam_offset = new Vector3f(0,2.5f,-6);

			mass = 900;

			areo_drag = 0.3f;

			w_steerAngle = 0.25f;

			sus_stiffness  = 200.0f;
			sus_restLength = 0.05f;
			sus_compValue  = 0.8f;
			sus_dampValue  = 0.9f;

			width = 1.5f;
			height = 0.7f;
			length = 5f;
			rollFraction = 0.2f;

			w_xOff = 0.62f;
			w_yOff = 0.12f;
			w_zOff = 1.63f;
			setw_Pos();

			//found via internet (f1 '09)
			e_torque = new float[]{0, 300,500,500,550,608, 595,580,560,540,525, 500,440,410,360,350};
			auto_gearDown = 9000;
			auto_gearUp = 13500;
			e_redline = 15000;

			trans_finaldrive = 3.2f;
			trans_gearRatios = new float[]{-5f,3.23f,2.19f,1.71f,1.39f,1.16f,0.93f};
		}
	}

	private static class Rocket extends CarData {
		Rocket() {
			carModel = dir + "rocket1.obj";
			w_zOff = 1.2f;
			w_xOff = 0.71f;
			setw_Pos();
			
			driveFront = true;
			driveRear = true;
			
			mass = 3000;
			w_steerAngle = 0.5f;

			areo_drag = 0.1f;
			areo_downforce = 200;
			rollFraction = 0f;

			sus_maxForce = 100*mass;
			
			brakeMaxTorque = 50000;

			e_torque = new float[]{0,210,310,390,460,520,565,600,625,640,650,645,625,580,460,200};
			for (int i = 0; i < e_torque.length; i++) {
				e_torque[i] *= 2;
			}
			auto_gearDown = 9000;
			auto_gearUp = 13500;
			e_redline = 15000;
			
			e_mass = 30;

			trans_finaldrive = 3.0f;
			trans_gearRatios = new float[]{-5f,3.23f,2.19f,1.71f,1.39f,1.16f,0.93f};

			w_flatdata = new RocketLatWheel();
			w_flongdata = new RocketLongWheel();
			
			nitro_force *= 10;
		}
	}

	///////////////////////////////////////
	//for the runing mode

	private static class Runner extends CarData {
		Runner() {
			carModel = dir+"track1_2.blend";
//
//			w_zOff = 1.3f;
//			w_yOff = 0.2f;
//			setw_Pos();
			
			e_torque = new float[] {0, 300, 450, 500, 530, 550, 500, 400};

			e_mass = 10; //TODO underground 2 seems to have very low values like these
			w_mass = 20;
			
			auto_gearDown = 4000;
			auto_gearUp = 6500;
			e_redline = 7000;

			sus_stiffness = 40.0f; //40 is fairly stiff //18.0
			sus_compValue = 0.5f; //(should be lower than damp)
			sus_dampValue = 0.6f;
			sus_restLength = 0.1f;

			sus_maxForce = 40000;

			areo_drag = 0.6f;
		}
	}

	private static class Hunter extends CarData {
		Hunter() {
			carModel = dir+"sa_hummer.blend";
			wheelModel = dir+"sa_hummer_wheel.blend";

			mass = 2500;
			width = 1.8f;
			height = 1.5f;
			length = 5f;
			
			w_xOff = 1.0f;
			w_yOff = -0.45f;
			w_zOff = 1.85f;
			setw_Pos();

			w_radius = 0.4f;

			rollFraction = 0.1f;
			sus_maxForce = 55000;

			driveFront = true;
			driveRear = true;

			e_torque = new float[]{0,520,680,720,760,773,520,110};

			auto_gearDown = 2900;
			auto_gearUp = 5700;
			e_redline = 6500;

			trans_effic = 0.75f;
			trans_finaldrive = 3f;
			trans_gearRatios = new float[]{-3.5f,3.66f,2.5f,1.9f,1.4f,1.02f,0.7f};
		}

	}
}