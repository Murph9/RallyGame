package game;

public abstract class CarWheelData {

	//http://www.edy.es/dev/docs/pacejka-94-parameters-explained-a-comprehensive-guide/
	
	//longitudinal force constants
	double b0 = 1.5;
	double b1 = 0;
	double b2 = 1100;
	double b3 = 0;
	double b4 = 300;
	double b5 = 0;
	double b6 = 0;
	double b7 = 0;
	double b8 = -2;
	double b9 = 0;
	double b10 = 0;
	double b11 = 0;
	double b12 = 0;
	double b13 = 0;
	
	//lateral force constants
	double a0 = 1.4;
	double a1 = 0;
	double a2 = 1100;
	double a3 = 1100;
	double a4 = 10;
	double a5 = 0;
	double a6 = 0;
	double a7 = -2;
	double a8 = 0;
	double a9 = 0;
	double a10 = 0;
	double a11 = 0;
	double a12 = 0;
	double a13 = 0;
	double a14 = 0;
	double a15 = 0;
	double a16 = 0;
	double a17 = 0;
}

class NormalWheel extends CarWheelData {

	NormalWheel() {}
}
