package car;

public abstract class WheelData {

	//http://www.edy.es/dev/docs/pacejka-94-parameters-explained-a-comprehensive-guide/
	//if you ever need the values a# and b# in here again go there ^ for the proper values
	float B = 10, C = 2, D = 1, E = 1;
}

class WheelDataLat extends WheelData {
	WheelDataLat() {
		B = 12f; // i made these up
		C = 1.9f;
		D = 1.1f;
		E = 1;
	}
}
class WheelDataLong extends WheelData {
	//defaults from here
	//http://au.mathworks.com/help/physmod/sdl/ref/tireroadinteractionmagicformula.html
	WheelDataLong() {
		B = 10f;
		C = 1.9f;
		D = 1f;
		E = 0.97f;
	}
}

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
		E = 1f;
	}
}
class RocketLongWheel extends WheelDataLong {
	RocketLongWheel() {
		B = 10f;
		C = 2f;
		D = 1.3f;
		E = 1f;
	}
}
