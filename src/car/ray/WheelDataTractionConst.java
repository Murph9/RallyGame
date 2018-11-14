package car.ray;

public class WheelDataTractionConst {
	//http://www.edy.es/dev/docs/pacejka-94-parameters-explained-a-comprehensive-guide/
	//if you ever need the values a# and b# in here again go there ^ for the proper values
	//defaults from here: http://au.mathworks.com/help/physmod/sdl/ref/tireroadinteractionmagicformula.html
	public float B;
	public float C;
	public float D;
	public float E;
	public WheelDataTractionConst() {}
	
	public WheelDataTractionConst(WheelDataTractionConst copy) {
		B = copy.B;
		C = copy.C;
		D = copy.D;
		E = copy.E;
	}
	
	@Override
	public String toString() {
		return "B:"+B+",C:"+C+",D:"+D+",E:"+E;
	}
}
