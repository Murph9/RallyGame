package car;

public abstract class CarWheelData {

	//http://www.edy.es/dev/docs/pacejka-94-parameters-explained-a-comprehensive-guide/
	//if you ever need the values a<N> and b<N> in here again go there ^ for the proper values
	
	//im currently using values that make sense for my game, its a simplified pacejka 
	
	//these are the recommened defaults.
	float B = 10f;
	float C = 1.8f;
	float D = 1f;
	float E = 0.97f;
    
}

//normal
class NormalLatData extends CarWheelData {
	NormalLatData() {
		B = 14f;
		C = 1.8f;
		E = 0.95f;
	}
}

class NormalLongData extends CarWheelData {
	NormalLongData() {
		B = 12f;
		C = 1.85f;
		E = 0.94f;
	}
}

//rally
class RallyLatWheel extends CarWheelData {
	RallyLatWheel() {
		B = 6f;
		C = 1.65f;
		E = 0.95f;
	}
}
class RallyLongWheel extends CarWheelData {
	RallyLongWheel() {
		B = 4f;
		C = 1.65f;
		E = 0.95f;
	}
}

//rocket
class RocketLatWheel extends CarWheelData {
	RocketLatWheel() {
		B = 10f;
		C = 1.8f;
		D = 1.3f;
		E = 0.95f;
	}
}
class RocketLongWheel extends CarWheelData {
	RocketLongWheel() {
		B = 10f;
		C = 1.85f;
		D = 1.3f;
		E = 0.92f;
	}
}


//ricer
class RicerLatWheel extends CarWheelData {
	RicerLatWheel() {
		//default
	}
}
class RicerLongWheel extends CarWheelData {
	RicerLongWheel() {
		D = 0.5f; //slippery
	}
}