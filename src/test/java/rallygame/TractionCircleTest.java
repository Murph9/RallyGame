package rallygame;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import com.jme3.math.FastMath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import rallygame.car.data.WheelDataTractionConst;
import rallygame.car.ray.GripHelper;
import rallygame.helper.H;

public class TractionCircleTest {

    private WheelDataTractionConst curveConstLong;
    private WheelDataTractionConst curveConstLat;
    private float maxLong;
    private float maxLat;

    @BeforeEach
    public void initConsts() {
        curveConstLong = new WheelDataTractionConst();
        curveConstLong.B = 17;
        curveConstLong.C = 1.9f;
        curveConstLong.D = 2;
        curveConstLong.E = 0.1f;

        curveConstLat = new WheelDataTractionConst();
        curveConstLat.B = 40;
        curveConstLat.C = 1.9f;
        curveConstLat.D = 2;
        curveConstLat.E = 0.1f;

        maxLong = CalcSlipMax(curveConstLong);
        maxLat = CalcSlipMax(curveConstLat);

        assumeFalse(Float.isNaN(maxLat));
        assumeFalse(Float.isNaN(maxLong));
    }

    @Test
	public void TestSlipFormula()
    {
        assertEquals(1, TractionFormula(curveConstLong, maxLong), 0.001f);
        assertEquals(1, TractionFormula(curveConstLat, maxLat), 0.01f);
    }

    @Test
    @Disabled
    public void generateCurveImage() {
        StringBuilder sb = new StringBuilder();
        int scale = 100;
        for (int i = -scale; i <= scale; i++) {
            for (int j = -scale; j <= scale; j++) {
            	float slipRatio = i / ((float) scale * 2);
            	float slipAngle = j / ((float) scale * 2);
            	float result = GetFromSlips(curveConstLong, curveConstLat, maxLong, maxLat, slipRatio, slipAngle);
                sb.append(slipRatio + " " + slipAngle +  " " + result + "\n");
            }
        }

        H.writeToFile(sb.toString(), "S:\\murph\\Desktop\\tractionCurveResults.txt");
    }
    

    private static float GetFromSlips(WheelDataTractionConst curveConstLong, WheelDataTractionConst curveConstLat, float maxLong, float maxLat, float slipRatio, float slipAngle)
    {
    	float ratiofract = slipRatio / maxLong;
    	float anglefract = slipAngle / maxLat;
    	float p = (float)Math.sqrt(ratiofract * ratiofract + anglefract * anglefract);
    	if (p == 0)
    		return 0;
    	float fZ = (ratiofract / p) * TractionFormula(curveConstLong, p*maxLong);
    	float fX = (anglefract / p) * TractionFormula(curveConstLat, p*maxLat);
    	return FastMath.sqrt(fX * fX + fZ * fZ);
    }

    private static float TractionFormula(WheelDataTractionConst w, float slip)
    {
        return GripHelper.tractionFormula(w, slip);
    }
    
    private static float CalcSlipMax(WheelDataTractionConst w) {
        return GripHelper.calcSlipMax(w);
    }
}
