package test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import com.jme3.math.FastMath;

import car.ray.RayCar;
import car.ray.WheelDataTractionConst;
import helper.Log;

public class RayCarTest {

	private static final boolean COMPLEX_FORMULA = true;
	
	public static void main(String[] args) {
		TestCombinedSlip();
	}
	
	private static void TestCombinedSlip()
    {
		WheelDataTractionConst curveConstLong = new WheelDataTractionConst();
		curveConstLong.B = 17;
		curveConstLong.C = 1.9f;
		curveConstLong.D = 1;
		curveConstLong.E = 0.1f; //aggressive so you can see the color gradient 
		float maxLong = CalcSlipMax(curveConstLong, 0.1f, 0.005f);
		
		WheelDataTractionConst curveConstLat = new WheelDataTractionConst();
        curveConstLat.B = 40;
		curveConstLat.C = 1.9f;
		curveConstLat.D = 1;
		curveConstLat.E = 0.1f;
        float maxLat = CalcSlipMax(curveConstLat, 0.05f, 0.005f);
        
        if (Float.isNaN(maxLong)) {
        	Log.e("Maxlong is NaN");
        	System.exit(-87000);
        }
        if (Float.isNaN(maxLat)) {
        	Log.e("Maxlat is NaN");
        	System.exit(-87001);
        }

        StringBuilder sb = new StringBuilder();
        int scale = 100;
        for (int i = -scale; i <= scale; i++)
        {
            for (int j = -scale; j <= scale; j++)
            {
            	float slipRatio = i / ((float) scale * 2);
            	float slipAngle = j / ((float) scale * 2);
            	float result = GetFromSlips(curveConstLong, curveConstLat, maxLong, maxLat, slipRatio, slipAngle);
                sb.append(slipRatio + " " + slipAngle +  " " + result + "\n");
            }
        }

        File file = new File("S:\\murph\\Desktop\\tractionCurveResults.txt");
        if (!file.exists())
			try {
				file.createNewFile();
			} catch (IOException e1) {
				e1.printStackTrace();
				return;
			}
        
        try (PrintWriter out = new PrintWriter(file)) {
            out.println(sb.toString());
        } catch (FileNotFoundException e) {
			e.printStackTrace();
			return;
		}
        
        Log.p("Done, see file in: " + file.getAbsolutePath());
    }

    private static float GetFromSlips(WheelDataTractionConst curveConstLong, WheelDataTractionConst curveConstLat, float maxLong, float maxLat, float slipRatio, float slipAngle)
    {
        //original
    	float ratiofract = slipRatio / maxLong;
    	float anglefract = slipAngle / maxLat;
    	float p = (float)Math.sqrt(ratiofract * ratiofract + anglefract * anglefract);
    	if (p == 0)
    		return 0;
    	float fZ = (ratiofract / p) * TractionFormula(curveConstLong, p*maxLong);
    	float fX = (anglefract / p) * TractionFormula(curveConstLat, p*maxLat);
    	return FastMath.sqrt(fX * fX + fZ * fZ);
        
        //v2 will make a circle
    	/*
        float p2 = FastMath.sqrt(slipRatio * slipRatio + slipAngle * slipAngle);
        float fX = slipRatio*TractionFormula(curveConst, p2)/p2;
        
        float fZ = slipAngle*TractionFormula(curveConst2, p2)/p2;
        
        return FastMath.sqrt(fX * fX + fZ * fZ);
        */
    }

    private static float TractionFormula(WheelDataTractionConst w, float slip)
    {
        if (COMPLEX_FORMULA)
            return RayCar.GripHelper.tractionFormula(w, slip);
        else
        {   //hack simple forumla
            if (Math.abs(slip) <= 0.1f)
                return slip * 10;
            if (slip > 0)
            	return -(slip - 0.1f) + 1;
            else
            	return (slip + 0.1f) + 1;
        }
    }
    
    private static float CalcSlipMax(WheelDataTractionConst w, double guess, double error) {
    	if (COMPLEX_FORMULA)
    		return RayCar.GripHelper.calcSlipMax(w, guess, error);
        else
            return 0.1f;
    }
}
