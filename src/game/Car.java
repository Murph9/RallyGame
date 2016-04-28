package game;

import com.jme3.math.Vector3f;

public enum Car {
	Normal(new NormalCar()),
	Normalf(new NormalFCar()),
	Rally(new RallyCar()),
	Track(new TrackCar()),
	Rocket(new Rocket()),
	Runner(new Runner()),
	Hunter(new Hunter()),
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
			DRAG = 0.7f;
			RESISTANCE = 10;
			
			wheelWidth = 0.25f;
			wheelRadius = 0.4f;

			driveFront = true;
			driveRear = true;

			wheel_xOff = 0.7f;
			wheel_yOff = 0.2f;
			wheel_zOff = 1.1f;

			stiffness  = 35.0f;
			restLength = 0.15f;
			compValue  = 0.2f;
			dampValue  = 0.2f;
			rollFraction = 0.6f;
			maxSusForce = 25000;

			torque = new float[]{0,520,580,620,680,720,870,820,0};

			gearDown = 3500;
			gearUp = 6800;
			redline = 7200;

			transEffic = 0.75f;
			diffRatio = 2.5f;
			gearRatios = new float[]{-3.5f,3.0f,2.3f,1.6f,1.2f,0.87f,0.7f};
			
			wheellatdata = new RallyLatWheel();
			wheellongdata = new RallyLongWheel();
		}
	}

	private static class TrackCar extends CarData {
		TrackCar() {
			carModel = dir+"f1.blend";
			wheelModel = dir+"f1_wheel.blend";
			CAM_OFFSET = new Vector3f(0,2.5f,-6);

			mass = 900;

			DRAG = 0.3f; //engine is stopping before these values...
			RESISTANCE = 5;

			steerAngle = 0.25f;

			stiffness  = 200.0f;
			restLength = 0.05f;
			compValue  = 0.8f;
			dampValue  = 0.9f;

			width = 1.5f;
			height = 0.7f;
			length = 5f;
			rollFraction = 0.2f;

			wheel_xOff = 0.62f;
			wheel_yOff = 0.12f;
			wheel_zOff = 1.63f;

			//TODO found via internet (f1 '09)
			torque = new float[]{0, 300,500,500,550,608, 595,580,560,540,525, 500,440,410,360,350};
			gearDown = 9000;
			gearUp = 13500;
			redline = 15000;

			diffRatio = 3.2f;
			gearRatios = new float[]{-5f,3.23f,2.19f,1.71f,1.39f,1.16f,0.93f};
		}
	}

	private static class Rocket extends CarData {
		Rocket() {
			carModel = dir + "rocket1.obj";
			wheel_zOff = 1.2f;
			wheel_xOff = 0.71f;
			
			mass = 1500;
			steerAngle = 0.5f;

			DRAG = 0.1f;
			RESISTANCE = 5;
			rollFraction = 0f;

			MAX_BRAKE = 50000;

			torque = new float[]{0, 300,500,500,550,608, 595,580,560,540,525, 500,440,410,360,250};
			for (int i = 0; i < torque.length; i++) {
				torque[i] *= 2;
			}
			gearDown = 9000;
			gearUp = 13500;
			redline = 15000;

			diffRatio = 2.5f;
			gearRatios = new float[]{-5f,3.23f,2.19f,1.71f,1.39f,1.16f,0.93f};

			wheellatdata = new RocketWheel();
			wheellongdata = new RocketWheel();
		}
	}

	///////////////////////////////////////
	//for the runing mode

	private static class Runner extends CarData {
		Runner() {
			carModel = dir+"car5.obj";

			wheel_zOff = 1.3f;
			wheel_yOff = 0.2f;

			torque = new float[] {0, 300, 450, 500, 530, 550, 500, 1};

			gearDown = 3000;
			gearUp = 6200;
			redline = 7000;

			stiffness = 12.0f; //20 is fairly stiff
			compValue = 0.5f; //(should be lower than damp)
			dampValue = 0.6f;
			restLength = 0.3f;

			maxSusForce = 40000;

		}
	}

	private static class Hunter extends CarData {
		Hunter() {
			carModel = dir+"sa_hummer.blend";
			wheelModel = dir+"sa_hummer_wheel.blend";

			mass = 2500;

			wheel_xOff = 1.0f;
			wheel_yOff = -0.45f;
			wheel_zOff = 1.85f;

			wheelRadius = 0.4f;

			rollFraction = 0.1f;
			maxSusForce = 55000;

			driveFront = true;
			driveRear = true;

			torque = new float[]{0,520,680,720,760,773,520,110};

			gearDown = 2900;
			gearUp = 5700;
			redline = 6500;

			transEffic = 0.75f;
			diffRatio = 3f;
			gearRatios = new float[]{-3.5f,3.66f,2.5f,1.9f,1.4f,1.02f,0.7f};
		}

	}
}