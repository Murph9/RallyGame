package world.track;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;

import com.jme3.math.Vector3f;

import helper.Log;

public class BezierPolygonInterpolation {

	public static void BezierCurvePolygonInterpolation()
    {
		BezierPolygonInterpolation.GetBezierCurvesN(
		Arrays.asList(        
            new Vector3f(-1,0,1),
            new Vector3f(-1,0,-1),
            new Vector3f(1,0,-1),
            new Vector3f(1,0,1)
        ), 0.5f);
    }
	
	public static List<TrackSegment> GetBezierCurvesN(List<Vector3f> points, float k)
    {
        //See: http://www.antigrain.com/research/bezier_interpolation/

		Log.p(points);

		int count = points.size();
		
        //calculate mid points [step 1]
		List<Vector3f> midPoints = new LinkedList<Vector3f>();
        for (int i = 0; i < count; i++)
        {
            midPoints.add(lerp(points.get(i), points.get((i + 1) % points.size()), 0.5f));
        }
        Log.p(midPoints);

        //calculate proportional points of the mid points [step2]
        List<Vector3f> midmidPoints = new LinkedList<Vector3f>();
        for (int i = 0; i < count; i++)
        {
            float _diff = points.get(i).distance(points.get(Math.floorMod(i - 1, count)))/
                       (points.get(i).distance(points.get(Math.floorMod(i - 1, count))) + points.get(i).distance(points.get((i + 1) % count)));
            midmidPoints.add(lerp(midPoints.get(i), midPoints.get(Math.floorMod(i - 1, count)), _diff));
        }

        Log.p(midmidPoints);

        //calculate the pos of the points moved from the control points [step 3]
        List<TrackSegment> curves = new LinkedList<TrackSegment>();

        for (int i = 0; i < count; i++)
        {
            Vector3f dirab = midmidPoints.get(i).subtract(midPoints.get(i)).mult(k);
            Vector3f dirba = midmidPoints.get((i + 1) % count).subtract(midPoints.get(i)).mult(k);
            curves.add(new TrackSegmentStraight(new Vector3f[]
            {
                points.get(i), points.get(i).add(dirab), points.get((i + 1) % count).add(dirba), points.get((i + 1) % count)
            }, (BiFunction<Vector3f, Vector3f, TrackSlice>)null));
        }

        for (int i = 0; i < points.size(); i++)
        {
            Log.p(curves.get(i).getControlPoints(), "curves[" + i + "]");
        }

        return curves;
    }

    private static Vector3f lerp(Vector3f a, Vector3f b, float t)
    {
    	try {
	        if (a == null || b == null)
	            throw new Exception("aaaa pls stop");
	        if (t < 0 || t > 1)
	            throw new Exception("aaaa pls stop 2");
    	} catch (Exception ex) {
    		ex.printStackTrace();
    	}

        return new Vector3f(a.x * t + b.x * (1 - t), a.y * t + b.y * (1 - t), a.z * t + b.z * (1 - t));
    }
}
