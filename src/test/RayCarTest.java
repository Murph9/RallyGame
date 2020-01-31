package test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import com.jme3.math.FastMath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import car.data.WheelDataTractionConst;
import car.ray.GripHelper;
import helper.Log;

public class RayCarTest {

    private WheelDataTractionConst curveConstLong;
    private WheelDataTractionConst curveConstLat;
    private float maxLong;
    private float maxLat;

    @BeforeEach
    public void initConsts() {
        curveConstLong = new WheelDataTractionConst();
        curveConstLong.B = 17;
        curveConstLong.C = 1.9f;
        curveConstLong.D1 = 2;
        curveConstLong.D2 = 0.000055f;
        curveConstLong.E = 0.1f;

        curveConstLat = new WheelDataTractionConst();
        curveConstLat.B = 40;
        curveConstLat.C = 1.9f;
        curveConstLat.D1 = 2;
        curveConstLong.D2 = 0.000055f;
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
        writeToFile(sb.toString(), "S:\\murph\\Desktop\\tractionCurveResults.txt");
    }
    private void writeToFile(String data, String filePath) {
        File file = new File(filePath);
        if (!file.exists())
            try {
                file.createNewFile();
            } catch (IOException e1) {
                e1.printStackTrace();
                return;
            }

        try (PrintWriter out = new PrintWriter(file)) {
            out.println(data);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        }

        Log.p("Done, see file in: " + file.getAbsolutePath());
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
