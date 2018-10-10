package car;

import com.jme3.math.Vector3f;

import car.ray.CarDataConst;
import car.ray.WheelDataConst;
import car.ray.WheelDataTractionConst;

//TODO use ridge racer cars

//TODO rewrite this whole file to make a little bit more sense
//like using the enum to actually generate the data, instead of using extended classes

public enum Car {
	Normal(new NormalCar()),
	Runner(new Runner()),
	Rally(new RallyCar()),
	Miata(new Miata()),
	Gt(new Gt()),
	
	Hunter(new Hunter()),
	Ricer(new Ricer()),
	Muscle(new Muscle()),
	Wagon(new Wagon()),
	
	WhiteSloth(new WhiteSloth()),
	Rocket(new Rocket())
	;
	private CarDataConst car;
	Car(CarDataConst car) {
		this.car = car;
	}
	
	public CarDataConst get() {
		return car;
	}
	
	private static class NormalCar extends CarDataConst {
		//for using the default settings.
		NormalCar() {
			rollFraction = 1.2f;
		}
		
		@Override
		public void postLoad() {
			//nothing
		}
	}
	
	private static class Runner extends CarDataConst {
		Runner() {
			carModel = dir+"track1_2.blend";

			e_torque = new float[] {0, 300, 450, 500, 530, 550, 500, 400};
			trans_finaldrive = 3.5f;
			trans_gearRatios = new float[]{-2.9f,3.6f,2.5f,1.8f,1.3f,1.0f,0.84f}; //reverse,gear1,gear2,g3,g4,g5,g6,...
			
			e_mass = 20;
			
			auto_gearDown = 4000;
			auto_gearUp = 6500;
			e_redline = 7000;

			sus_stiffness = 40.0f; //40 is fairly stiff //18.0
			sus_comp = 0.5f; //(should be lower than damp)
			sus_relax = 0.6f;
			
			rollFraction = 0.4f; //matters a lot when it comes to holding grip in corners

			sus_max_force = 40000;

			areo_drag = 0.33f;
			areo_crossSection = 0.59f;
			
			brakeMaxTorque = 5000;
		}
		
		@Override
		public void postLoad() {
			float x_off = 0.68f;
			float y_off = 0;
			float z_off = 1.1f;
			
			for (int i = 0; i < wheelData.length; i++) {
				WheelDataTractionConst wLat = new WheelDataTractionConst(10f, 1.9f, 1f, 0.97f);
				WheelDataTractionConst wLong = new WheelDataTractionConst(10f, 1.9f, 1f, 0.97f);
				wheelData[i] = new WheelDataConst(wheelModel, 0.3f, 10, 0.15f, wLat, wLong);
				
				setWheelOffset(i, x_off, y_off, z_off);
			}
		}
	}
	
	
	private static class Miata extends CarDataConst {
		Miata() {
			carModel = dir+"miata.blend";
			wheelModel = dir+"miata_wheel.blend";
			
			mass = 1000;
			
			e_torque = new float[] {0, 300, 450, 500, 530, 550, 500, 400}; //TODO its slower than this
			trans_finaldrive = 3.5f;
			trans_gearRatios = new float[]{-2.9f,3.6f,2.5f,1.8f,1.3f,1.0f,0.84f}; //reverse,gear1,gear2,g3,g4,g5,g6,...
			
			e_mass = 20; //TODO underground 2 seems to have very low values like these
			
			rollFraction = 1;
			
			auto_gearDown = 4000;
			auto_gearUp = 6500;
			e_redline = 7000;
			
			sus_max_force = 40000;
		}
		
		@Override 
		public void postLoad() {
			float x_off = 0.68f;
			float y_off = 0;
			float z_off = 1.1f;
			
			for (int i = 0; i < wheelData.length; i++) {
				WheelDataTractionConst wLat = new WheelDataTractionConst(10f, 1.9f, 1f, 0.97f);
				WheelDataTractionConst wLong = new WheelDataTractionConst(10f, 1.9f, 1f, 0.97f);
				wheelData[i] = new WheelDataConst(wheelModel, 0.3f, 10, 0.15f, wLat, wLong);
				
				setWheelOffset(i, x_off, y_off, z_off);
			}
		}
	}
	
	//TODO it rolls realy easy compared to the runner car
	//TODO tune in general
	private static class Ricer extends CarDataConst {
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
						
			//https://en.wikipedia.org/wiki/Automobile_drag_coefficient
			areo_drag = 0.33f; //do not touch this, it actually balances with the torque curve now

			driveFront = false;
			driveRear = true;
			
			//http://www.automobile-catalog.com/curve/1991/2179700/nissan_200_sx_turbo_16v.html
			e_torque = new float[] {0, 50, 150, 210, 226, 223, 200, 140};

			auto_gearDown = 3500;
			auto_gearUp = 6500;
			e_redline = 6700;
			e_mass = 5; //fast reving
			
			trans_effic = 0.85f;
			trans_finaldrive = 3.69f;//final drive maybe?
			trans_gearRatios = new float[] { -3.38f, 3.32f, 1.9f, 1.31f, 1f, 0.84f};
			
			sus_stiffness = 20.0f; //20 is fairly stiff
			sus_comp = 0.5f; //(should be lower than damp)
			sus_relax = 0.6f;

			sus_max_force = mass*9.81f*4f;
		}
		
		@Override 
		public void postLoad() {
			float x_off = 0.68f;
			float y_off = -0.15f;
			float z_off = 1.32f;
			
			for (int i = 0; i < wheelData.length; i++) {
				WheelDataTractionConst wLat = new WheelDataTractionConst(10f, 1.9f, 1f, 0.97f);
				WheelDataTractionConst wLong = new WheelDataTractionConst(10f, 1.9f, 1f, 0.97f);
				wheelData[i] = new WheelDataConst(wheelModel, 0.3f, 10, 0.15f, wLat, wLong);
				
				setWheelOffset(i, x_off, y_off, z_off);
			}
		}
	}

	
	private static class WhiteSloth extends CarDataConst {
		//http://www.automobile-catalog.com/auta_details1.php
		WhiteSloth() {
			carModel = dir+"Mazda_121_Metro.blend";
			wheelModel = dir+"Mazda_121_Metro_wheel.blend";
			
			driveFront = true;
			driveRear = false;
			
			mass = 960;
			areo_drag = 0.38f;
						
			width = 1.67f;
			height = 1.535f;
			length = 3.8f;
			
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
			
			//just for kaz..
			nitro_force *= 10;
		}
		
		@Override 
		public void postLoad() {
			float x_off = 0.68f;
			float y_off = -0.15f;
			float z_off = 1.32f;
			
			for (int i = 0; i < wheelData.length; i++) {
				WheelDataTractionConst wLat = new WheelDataTractionConst(10f, 1.9f, 1f, 0.97f);
				WheelDataTractionConst wLong = new WheelDataTractionConst(10f, 1.9f, 1f, 0.97f);
				wheelData[i] = new WheelDataConst(wheelModel, 0.575f/2f, 10, 0.15f, wLat, wLong);
				
				setWheelOffset(i, x_off, y_off, z_off);
			}
		}
	}

	private static class RallyCar extends CarDataConst {
		RallyCar() {
			carModel = dir+"car4raid_1.obj";
			wheelModel = dir+"wheelraid1.obj";

			mass = 1500;
			areo_drag = 0.7f;
			
			driveFront = true;
			driveRear = true;

			sus_stiffness  = 35.0f;
			sus_comp = 0.4f;
			sus_relax = 0.5f;
			sus_max_force = 35000;

			e_torque = new float[]{0,520,580,620,680,720,870,820,0};
			e_mass = 40;
			auto_gearDown = 3500;
			auto_gearUp = 6800;
			e_redline = 7200;

			trans_effic = 0.85f;
			trans_finaldrive = 4f;
			trans_gearRatios = new float[]{-3.5f,3.0f,2.3f,1.6f,1.2f,0.87f,0.7f};
		}
		
		@Override
		public void postLoad() {
			float x_off = 0.7f;
			float y_off = 0.2f;
			float z_off = 1.1f;
			
			for (int i = 0; i < wheelData.length; i++) {
				WheelDataTractionConst wLat = new WheelDataTractionConst(6f, 2f, 1f, 1f);
				WheelDataTractionConst wLong = new WheelDataTractionConst(6f, 2f, 1f, 1f);
				wheelData[i] = new WheelDataConst(wheelModel, 0.4f, 15, 0.25f, wLat, wLong);

				setWheelOffset(i, x_off, y_off, z_off);
			}
		}
	}

	private static class Rocket extends CarDataConst {
		Rocket() {
			carModel = dir + "rocket1_1.blend";
			
			driveFront = true;
			driveRear = true;
			
			mass = 1500;

			w_steerAngle = 0.6f;
			
			areo_drag = 0.15f; //0.3f is smallest possible real min value
			areo_downforce = 2;
			rollFraction = 0.1f;
			cam_shake *= 0.1f;

			sus_stiffness = 100f;
			
			brakeMaxTorque = 50000;

			e_redline = 15000;
			e_torque = new float[]{0,210,310,390,460,520,565,600,625,640,650,645,625,580,460,200};
			for (int i = 0; i < e_torque.length; i++) {
				e_torque[i] *= 2;
			}
			auto_gearDown = 7000;
			auto_gearUp = 13500;
			
			trans_finaldrive = 3.0f;
			trans_gearRatios = new float[]{-5f,5,3.23f,2.19f,1.71f,1.39f,1.16f,0.93f,0.81f,0.74f}; //please no more than 9 gears
			
			nitro_force *= 10;
		}
		
		@Override
		public void postLoad() {
			float x_off = 0.71f;
			float y_off = 0f;
			float z_off = 1.2f;
			
			for (int i = 0; i < wheelData.length; i++) {
				WheelDataTractionConst wLat = new WheelDataTractionConst(10f, 2f, 1.3f, 0.985f);
				WheelDataTractionConst wLong = new WheelDataTractionConst(10f, 2f, 1.3f, 0.985f);
				wheelData[i] = new WheelDataConst(wheelModel, 0.4f, 10, 0.15f, wLat, wLong);

				setWheelOffset(i, x_off, y_off, z_off);
			}
		}
	}

	private static class Gt extends CarDataConst {
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
						
			sus_max_force = mass*55;
			
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
		
		@Override
		public void postLoad() {
			float x_off = 0.71f;
			float y_off = 0.2f;
			float z_off = 1.2f;
			
			for (int i = 0; i < wheelData.length; i++) {
				WheelDataTractionConst wLat = new WheelDataTractionConst(6f, 2f, 1f, 1f);
				WheelDataTractionConst wLong = new WheelDataTractionConst(6f, 2f, 1f, 1f);
				wheelData[i] = new WheelDataConst(wheelModel, 0.735f/2f, 10, 0.15f, wLat, wLong);

				setWheelOffset(i, x_off, y_off, z_off);
			}
		}
	}

	private static class Hunter extends CarDataConst {
		Hunter() {
			carModel = dir+"sa_hummer.blend";
			wheelModel = dir+"sa_hummer_wheel.blend";

			mass = 2500;
			width = 1.8f;
			height = 1.5f;
			length = 5f;

			rollFraction = 0.1f;
			sus_max_force = 85000;

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

		@Override
		public void postLoad() {
			float x_off = 1.0f;
			float y_off = -0.45f;
			float z_off = 1.85f;
			
			for (int i = 0; i < wheelData.length; i++) {
				WheelDataTractionConst wLat = new WheelDataTractionConst(6f, 2f, 1f, 1f);
				WheelDataTractionConst wLong = new WheelDataTractionConst(6f, 2f, 1f, 1f);
				wheelData[i] = new WheelDataConst(wheelModel, 0.4f, 10, 0.15f, wLat, wLong);

				setWheelOffset(i, x_off, y_off, z_off);
			}
		}
	}
	
	private static class Muscle extends CarDataConst {
		Muscle() {
			carModel = dir+"muscle.blend";
			
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
		
		@Override
		public void postLoad() {
			float x_off = 0.68f;
			float y_off = 0;
			float z_off = 1.1f;
			
			for (int i = 0; i < wheelData.length; i++) {
				WheelDataTractionConst wLat = new WheelDataTractionConst(10f, 1.9f, 0.9f, 0.95f);
				WheelDataTractionConst wLong = new WheelDataTractionConst(12f, 1.9f, 0.9f, 0.95f);
				wheelData[i] = new WheelDataConst(wheelModel, 0.4f, 10, 0.15f, wLat, wLong);

				setWheelOffset(i, x_off, y_off, z_off);
			}
		}
	}
	
	private static class Wagon extends CarDataConst {
		Wagon() {
			carModel = dir+"Wagon.blend";
			wheelModel = dir+"Wagon_Wheel.blend";
			
			mass = 500;
			height = 1;
			
			driveFront = true; //AWD
						
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
		
		@Override
		public void postLoad() {
			float x_off = 0.68f;
			float y_off = 0;
			float z_off = 1.1f;
			
			for (int i = 0; i < wheelData.length; i++) {
				WheelDataTractionConst wLat = new WheelDataTractionConst(10f, 1.9f, 0.62f, 0.95f);
				WheelDataTractionConst wLong = new WheelDataTractionConst(10f, 1.9f, 0.62f, 0.95f);
				wheelData[i] = new WheelDataConst(wheelModel, 0.5f, 10, 0.1f, wLat, wLong);

				setWheelOffset(i, x_off, y_off, z_off);
			}
		}
	}
}