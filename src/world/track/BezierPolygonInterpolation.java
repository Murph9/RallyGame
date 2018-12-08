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
		BezierPolygonInterpolation.GetBezierCurves(
		Arrays.asList(        
            new Vector3f(-2,0,0),
            new Vector3f(5,0,0),
            new Vector3f(2.5f,0,2)
        ), 0.5f);
    }
	
	//TODO this is not complete, missing:
	//- polygon not triangle
	//- the final step where it generates the tracksegment from all the sides
	
    public static List<TrackSegment> GetBezierCurves(List<Vector3f> points, float k)
    {
        if (points.size() != 3)
            return null; //TODO hack for starting the logic working

        //See: http://www.antigrain.com/research/bezier_interpolation/

        Log.p(points);

        //calculate mid points [step 1]
        List<Vector3f> midPoints = new LinkedList<Vector3f>();
        midPoints.add(lerp(points.get(0), points.get(1), 0.5f)); //A
        midPoints.add(lerp(points.get(1), points.get(2), 0.5f)); //B
        midPoints.add(lerp(points.get(0), points.get(2), 0.5f)); //C
        //[0-1,1-2,0-2]

        Log.p(midPoints);

        //calculate proportional points of the mid points [step2]
        List<Vector3f> midmidPoints = new LinkedList<Vector3f>();
        float diff02 = points.get(2).distance(points.get(0));
        float diff12 = points.get(2).distance(points.get(1));
        float diff01 = points.get(1).distance(points.get(0));

        float diff = diff02 / (diff02 + diff01);
        midmidPoints.add(lerp(midPoints.get(0), midPoints.get(2), diff)); //D

        diff = diff01 / (diff12 + diff01);
        midmidPoints.add(lerp(midPoints.get(1), midPoints.get(0), diff)); //E

        diff = diff12 / (diff02 + diff12);
        midmidPoints.add(lerp(midPoints.get(2), midPoints.get(1), diff)); //F
        //[0,1,2]

        Log.p(midmidPoints);

        //calculate the pos of the points moved from the control points [step 3]
        List<TrackSegment> curves = new LinkedList<TrackSegment>();

        Vector3f dir01 = midmidPoints.get(0).subtract(midPoints.get(0));
        Vector3f dir10 = midmidPoints.get(1).subtract(midPoints.get(0));
        curves.add(new TrackSegmentStraight(new Vector3f[] {
            points.get(0), points.get(0).add(dir01), points.get(1).add(dir10), points.get(1)
        }, (BiFunction<Vector3f, Vector3f, TrackSlice>)null));

        Log.p(curves.get(0).getControlPoints());

        return curves;
    }

    //TODO the real Vector3f class handles all this..
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
