package game;

public abstract class CarWheelData {

	//http://www.edy.es/dev/docs/pacejka-94-parameters-explained-a-comprehensive-guide/
	//if you ever need the values a<N> and b<N> in here again go there ^
	
	//these are the 'nice' defaults.
	float B = 10f;
	float C = 1.9f;
	float D = 1f;
	float E = 0.97f;
    
}

class NormalLatData extends CarWheelData {

	NormalLatData() {
		B = 10f;
		C = 1.9f;
		D = 1f;
		E = 0.97f;
	}
}

class NormalLongData extends CarWheelData {

	NormalLongData() {
		B = 10f;
		C = 1.9f;
		D = 1f;
		E = 0.97f;
	}
}


class RocketWheel extends CarWheelData {
	
	RocketWheel() {
		B = 10f;
		C = 1.9f;
		D = 2f;
		E = 0.97f;
	}
}
