package car;

import com.jme3.math.Vector3f;

//TODO use ridge racer cars

public enum Car {
	Normal(new NormalCar()),
	WhiteSloth(new WhiteSloth()),
	Rally(new RallyCar()),
	Rocket(new Rocket()),
	Runner(new Runner()),
	Hunter(new Hunter()),
	Ricer(new Ricer()),
	Muscle(new Muscle()),
	Wagon(new Wagon()),
	Miata(new Miata()),
	Gt(new Gt()),
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
		NormalCar() {
			rollFraction = 1.2f;
		}
	}
	
	private static class Miata extends CarData {
		Miata() {
			carModel = dir+"miata.blend";
			wheelModel = dir+"miata_wheel.blend";
			
			mass = 1000;
			
			e_torque = new float[] {0, 300, 450, 500, 530, 550, 500, 400}; //TODO its slower than this
			trans_finaldrive = 3.5f;
			trans_gearRatios = new float[]{-2.9f,3.6f,2.5f,1.8f,1.3f,1.0f,0.84f}; //reverse,gear1,gear2,g3,g4,g5,g6,...
			
			e_mass = 20; //TODO underground 2 seems to have very low values like these
			w_mass = 10;
			
			rollFraction = 1;
			
			auto_gearDown = 4000;
			auto_gearUp = 6500;
			e_redline = 7000;
			
			sus_maxForce = 40000;
		}
	}
	
	//TODO it rolls realy easy compared to the runner car
	//TODO tune in general
	private static class Ricer extends CarData {
		//for using the default settings.
		//probably shouldn't have a custom constructor
		Ricer() {
			wheelModel = dir+"wheel3.blend";
			carModel = dir+"ricer.blend";
			
			//trying my best from: http://www.cars-data.com/en/nissan-200-sx-turbo-specs/26930
			mass = 1240;
			width = 1.74f;
			height = 1.295f;
			length = 4.52f;
			
			rollFraction = 1f;
			
			w_zOff = 1.32f;
			w_yOff = -0.15f;
			setw_Pos();
			
			//https://en.wikipedia.org/wiki/Automobile_drag_coefficient
			areo_drag = 0.33f; //do not touch this, it actually balances with the torque curve now

			driveFront = false;
			driveRear = true;
			
			//http://www.automobile-catalog.com/curve/1991/2179700/nissan_200_sx_turbo_16v.html
			e_torque = new float[] {0, 50, 150, 210, 226, 223, 200, 140};

			auto_gearDown = 3500;
			auto_gearUp = 6500;
			e_redline = 6800;
		
			e_compression = 2;
			e_mass = 5; //fast reving
			
			trans_effic = 0.85f;
			trans_finaldrive = 3.69f;//final drive maybe?
			trans_gearRatios = new float[] { -3.38f, 3.32f, 1.9f, 1.31f, 1f, 0.84f};
			
			sus_stiffness = 40.0f; //20 is fairly stiff
			sus_compValue = 0.5f; //(should be lower than damp)
			sus_dampValue = 0.6f;
			sus_restLength = 0.2f;

			sus_maxForce = 8000;
		}
	}

	
	private static class WhiteSloth extends CarData {
		//http://www.automobile-catalog.com/auta_details1.php
		WhiteSloth() {
			carModel = dir+"Mazda_121_Metro_2.blend";
			wheelModel = dir+"Mazda_121_Metro_wheel.blend";
			
			driveFront = true;
			driveRear = false;
			
			mass = 960;
			areo_drag = 0.38f;
						
			width = 1.67f;
			height = 1.535f;
			length = 3.8f;
			
			//change wheel size
			w_radius = 0.575f/2f;
			sus_restLength = 0.2f;
			rollFraction = 0.2f;
			
			auto_gearDown = 2000;
			auto_gearUp = 5600; //guessed
			e_redline = 6200; //guessed
			
			//smaller engine
			e_compression *= 0.2f;
			e_mass = 2;
			
			e_torque = new float[] {0, 50, 85, 102, 110, 107, 95, 40};
			
			trans_effic = 0.85f;
			trans_finaldrive = 4.105f;
			trans_gearRatios = new float[] { -3.214f, 3.416f, 1.842f, 1.29f, 0.972f, 0.775f };
			
			//just for kaz:
			nitro_force *= 10;
		}
	}

	private static class RallyCar extends CarData {
		RallyCar() {
			carModel = dir+"car4raid_1.obj";
			wheelModel = dir+"wheelraid1.obj";

			mass = 1500;
			areo_drag = 0.7f;
			
			w_width = 0.25f;
			w_radius = 0.4f;
			w_mass = 10;
			
			driveFront = true;
			driveRear = true;

			w_xOff = 0.7f;
			w_yOff = 0.2f;
			w_zOff = 1.1f;
			setw_Pos();

			sus_stiffness  = 35.0f;
			sus_restLength = 0.15f;
			sus_compValue  = 0.4f;
			sus_dampValue  = 0.5f;
			rollFraction = 0.6f;
			sus_maxForce = 35000;

			e_torque = new float[]{0,520,580,620,680,720,870,820,0};
			e_mass = 40;
			auto_gearDown = 3500;
			auto_gearUp = 6800;
			e_redline = 7200;

			trans_effic = 0.85f;
			trans_finaldrive = 4f;
			trans_gearRatios = new float[]{-3.5f,3.0f,2.3f,1.6f,1.2f,0.87f,0.7f};
			
			w_flatdata = new RallyLatWheel();
			w_flongdata = new RallyLongWheel();
		}
	}

	private static class Rocket extends CarData {
		Rocket() {
			carModel = dir + "rocket1_1.blend";
			w_zOff = 1.2f;
			w_xOff = 0.71f;
			setw_Pos();
			
			driveFront = true;
			driveRear = true;
			
			mass = 1500;

			w_steerAngle = 0.6f;
			w_mass = 30;
			w_flatdata = new RocketLatWheel();
			w_flongdata = new RocketLongWheel();
			
			areo_drag = 0.1f;
			areo_downforce = 2;
			rollFraction = 0f;

			sus_maxForce = 100*mass;
			
			brakeMaxTorque = 50000;

			e_redline = 15000;
			e_torque = new float[]{0,210,310,390,460,520,565,600,625,640,650,645,625,580,460,200};
			for (int i = 0; i < e_torque.length; i++) {
				e_torque[i] *= 2;
			}
			auto_gearDown = 7000;
			auto_gearUp = 13500;
			
			trans_finaldrive = 3.0f;
			trans_gearRatios = new float[]{-5f,5,3.23f,2.19f,1.71f,1.39f,1.16f,0.93f};
			
			nitro_force *= 10;
		}
	}

	private static class Runner extends CarData {
		Runner() {
			carModel = dir+"track1_2.blend";

			//TODO you can fix the oversteer at high speeds with a proper diff and different downforce scaling at higher speeds  
			
			e_torque = new float[] {0, 300, 450, 500, 530, 550, 500, 400};
			trans_finaldrive = 3.5f;
			trans_gearRatios = new float[]{-2.9f,3.6f,2.5f,1.8f,1.3f,1.0f,0.84f}; //reverse,gear1,gear2,g3,g4,g5,g6,...
			
			e_mass = 20; //TODO underground 2 seems to have very low values like these
			w_mass = 10;
			
			auto_gearDown = 4000;
			auto_gearUp = 6500;
			e_redline = 7000;

			sus_stiffness = 40.0f; //40 is fairly stiff //18.0
			sus_compValue = 0.5f; //(should be lower than damp)
			sus_dampValue = 0.6f;
			sus_restLength = 0.1f;
			
			rollFraction = 0.4f; //matters a lot when it comes to holding grip in corners

			sus_maxForce = 40000;

			areo_drag = 0.33f;
			areo_crossSection = 0.59f;
			
			brakeMaxTorque = 5000;
		}
	}
	
	private static class Gt extends CarData {
		Gt() {
			//http://www.automobile-catalog.com/auta_details1.php
			//Ford Gt (2005)
			
			//TODO might highlight unusual physics?
			
			carModel = dir+"gt.blend";
			wheelModel = dir+"gt_wheel.blend";
			
			mass = 1520;
			width = 1.953f;
			height = 1.125f;
			length = 4.643f;
			
			w_radius = 0.735f/2f;
			w_mass = 10;
			
			sus_maxForce = mass*55;
			
			driveFront = false;
			driveRear = true;
			
			//http://www.automobile-catalog.com/curve/2005/894440/ford_gt.html
			e_torque = new float[]{0, 322.9f, 565f, 645.7f, 677.4f, 662.4f, 627.3f, 503.4f, 0};
			e_redline = 6800;
			e_mass = 20;
			
			auto_gearUp = 6700;
			
			rollFraction = -0.8f; //???
			brakeMaxTorque = 8000;
			
			trans_effic = 1f; //TODO
			trans_finaldrive = 3.36f;
			trans_gearRatios = new float[]{-3.135f, 2.611f, 1.708f, 1.233f, 0.943f, 0.767f, 0.625f};
			
			areo_drag = 0.31f;
			cam_offset = new Vector3f(0,3.1f,-9);
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
			sus_maxForce = 85000;

			driveFront = true;
			driveRear = true;

			e_torque = new float[]{0,520,680,720,760,773,520,110};

			auto_gearDown = 2900;
			auto_gearUp = 5700;
			e_redline = 6500;

			trans_effic = 0.75f;
			trans_finaldrive = 3f;
			trans_gearRatios = new float[]{-3.5f,3.66f,2.5f,1.9f,1.4f,1.02f,0.7f};
			
			cam_offset = new Vector3f(0,3.1f,-9);
		}

	}
	
	private static class Muscle extends CarData {
		Muscle() {
			carModel = dir+"muscle.blend";
			
			w_flatdata = new MuscleWheelLatData();
			w_flongdata = new MuscleWheelLongData();
			
			mass = 1520;
			width = 1.758f;
			height = 1.217f;
			length = 4.75f;
			//some of these numbers all around here are incorrect
			e_redline = 6000;
			e_torque = new float[] {0, 295, 510, 583, 624, 598, 520, 50};
			
			trans_effic = 0.8f;
			trans_gearRatios = new float[]{-2.27f, 2.2f, 1.64f, 1.28f, 1f};
			
			rollFraction = 1f;
			brakeMaxTorque = 3000;
		}
		
		class MuscleWheelLatData extends WheelData {
			MuscleWheelLatData() {
				B = 10f;
				C = 1.9f;
				D = 0.9f;
				E = 0.95f;
			}
		}
		class MuscleWheelLongData extends WheelData {
			MuscleWheelLongData() {
				B = 12f;
				C = 1.9f;
				D = 0.9f;
				E = 0.95f;
			}
		}
	}
	
	private static class Wagon extends CarData {
		Wagon() {
			carModel = dir+"Wagon.blend";
			wheelModel = dir+"Wagon_Wheel.blend";
			
			w_flatdata = new WagonWheelData();
			w_flongdata = new WagonWheelData();
			
			mass = 500;
			height = 1;
			
			driveFront = true;
			
			w_radius = 0.5f;
			w_width = 0.1f;
						
			sus_stiffness = 100;
			
			rollFraction = 1;
			
			e_mass = 40;
			e_torque = new float[] {200, 
					200, 200, 200, 200, 200, 
					200, 200, 200, 200, 200, 
					200, 200, 200, 200, 200}; //electric
			e_redline = 15000;
			
			trans_finaldrive = 1;
			trans_gearRatios = new float[]{-8.27f, 8.27f};
		}
		
		class WagonWheelData extends WheelData {
			WagonWheelData() {
				B = 10f;
				C = 1.9f;
				D = 0.62f;
				E = 0.95f;
			}
		}
	}
}