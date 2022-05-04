package rallygame.car.data;

import java.io.Serializable;

public class WheelDataTractionConst implements Serializable {
    // http://www.edy.es/dev/docs/pacejka-94-parameters-explained-a-comprehensive-guide/
    // if you ever need the values a# and b# in here again go there ^ for the proper values
    // defaults from here:
    // http://au.mathworks.com/help/physmod/sdl/ref/tireroadinteractionmagicformula.html
    public float B;
    public float C;
    public float D1;
    public float D2;
    public float E;

    public WheelDataTractionConst() {
    }

    public WheelDataTractionConst(WheelDataTractionConst copy) {
        B = copy.B;
        C = copy.C;
        D1 = copy.D1;
        D2 = copy.D2;
        E = copy.E;
    }

    @Override
    public String toString() {
        return "B:" + B + ",C:" + C + ",D1:" + D1 + ",D2:" + D2 + ",E:" + E;
    }
}
