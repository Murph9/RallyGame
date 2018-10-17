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
	
	//some pre made ones:
	public static final WheelDataTractionConst BASE = new WheelDataTractionConst(10f, 0.72f, 1.4f, -0.2f);
	public static final WheelDataTractionConst MY_BASE = new WheelDataTractionConst(10f, 1.9f, 1f, 0.97f);
	public static final WheelDataTractionConst DEBUG = new WheelDataTractionConst(6, 2, 1, 1);
	
	public static final WheelDataTractionConst BASE_SAT = new WheelDataTractionConst(0.85f, 2.3f, 0.51f, -2.75f);
}
