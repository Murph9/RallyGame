package car.ray;

public class WheelDataConst {

	public String modelName;
	public float radius;
	public float mass;
	public float width;
	
	public WheelDataTractionConst pjk_lat = new WheelDataTractionConst(10f, 1.9f, 1f, 0.97f);
	public WheelDataTractionConst pjk_long = new WheelDataTractionConst(10f, 1.9f, 1f, 0.97f);


	public class WheelDataTractionConst {
		public WheelDataTractionConst(float b, float c, float d, float e) {
			B = b; C = c; D = d; E = e;
		}
		
		//http://www.edy.es/dev/docs/pacejka-94-parameters-explained-a-comprehensive-guide/
		//if you ever need the values a# and b# in here again go there ^ for the proper values
		//defaults from here: http://au.mathworks.com/help/physmod/sdl/ref/tireroadinteractionmagicformula.html
		public float B;
		public float C;
		public float D;
		public float E;
	}
}