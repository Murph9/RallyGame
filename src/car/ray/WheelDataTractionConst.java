package car.ray;

public class WheelDataTractionConst {
	//http://www.edy.es/dev/docs/pacejka-94-parameters-explained-a-comprehensive-guide/
	//if you ever need the values a# and b# in here again go there ^ for the proper values
	//defaults from here: http://au.mathworks.com/help/physmod/sdl/ref/tireroadinteractionmagicformula.html
	public final float B;
	public final float C;
	public final float D;
	public final float E;
	
	public WheelDataTractionConst(float b, float c, float d, float e) {
		B = b; C = c; D = d; E = e;
	}
	
	@Override
	public String toString() {
		return "B:"+B+",C:"+C+",D:"+D+",E:"+E;
	}
}