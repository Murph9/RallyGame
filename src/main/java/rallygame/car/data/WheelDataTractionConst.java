package rallygame.car.data;

import java.io.Serializable;

public class WheelDataTractionConst implements Serializable {
    // http://www.edy.es/dev/docs/pacejka-94-parameters-explained-a-comprehensive-guide/
    // if you ever need the values a# and b# in here again go there ^ for the proper values
    // another source: http://au.mathworks.com/help/physmod/sdl/ref/tireroadinteractionmagicformula.html
    public float B;
    public float C;
    public float D;
    public float E;

    public float loadQuadratic;

    public float _max;

    public WheelDataTractionConst() {
    }

    public WheelDataTractionConst(WheelDataTractionConst copy) {
        B = copy.B;
        C = copy.C;
        D = copy.D;
        loadQuadratic = copy.loadQuadratic;
        E = copy.E;
    }

    @Override
    public String toString() {
        return "B:" + B + ",C:" + C + ",D1:" + D + ",quad:" + loadQuadratic + ",E:" + E;
    }
}
