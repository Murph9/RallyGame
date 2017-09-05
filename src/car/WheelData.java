package car;

import java.io.Serializable;

public abstract class WheelData implements Serializable {
	//http://www.edy.es/dev/docs/pacejka-94-parameters-explained-a-comprehensive-guide/
	//if you ever need the values a# and b# in here again go there ^ for the proper values
	//defaults from here: http://au.mathworks.com/help/physmod/sdl/ref/tireroadinteractionmagicformula.html
	public float B = 10, C = 1.9f, D = 2, E = 0.97f; //2 because its an arcade game
	
	@Override
	public String toString() {
		return "B:"+B+",C:"+C+",D:"+D+",E:"+E;
	}
}

class WheelDataLat extends WheelData {
	WheelDataLat() {
		B = 12f; // i made this all up
		D = 2.1f;
		E = 1;
	}
}
class WheelDataLong extends WheelData { /*nothing special*/ }

//rally
class RallyLatWheel extends WheelDataLat {
	RallyLatWheel() {
		B = 6f;
		C = 2f;
		E = 1f;
	}
}
class RallyLongWheel extends WheelDataLong {
	RallyLongWheel() {
		B = 8f;
		C = 2f;
		E = 1f;
	}
}

//rocket
class RocketLatWheel extends WheelDataLat {
	RocketLatWheel() {
		B = 10f;
		C = 2f;
		D = 1.3f;
		E = 0.99f;
	}
}
class RocketLongWheel extends WheelDataLong {
	RocketLongWheel() {
		B = 10f;
		C = 2f;
		D = 1.3f;
		E = 0.99f;
	}
}
