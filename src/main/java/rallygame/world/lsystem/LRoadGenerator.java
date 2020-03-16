package rallygame.world.lsystem;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

import rallygame.game.App;
import rallygame.helper.Log;
import rallygame.world.highway.RoadMesh;

public class LRoadGenerator {

	private static Quaternion ROT90 = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
	
	private SimpleApplication app;

	Node rootNode;
	private final boolean DEBUG = false;
	float height;
	
	private PriorityQueue<QueueObj> Q;
	private List<QueueObj> S;
	private List<Vector3f> buildings;
	
	int lineCount;
	private int count;
	private float total;
	
	private Geometry box1;
	private Geometry box2;
	
	LRoadGenerator(SimpleApplication app, Node rootNode, float height) {
		this.app = app;		
		this.rootNode = rootNode;
		this.height = height;
	}
	
	public void init() {
		Log.p("|Starting L-system, size: " + lineCount);
		
		// l-system like generation attempt
		Q = new PriorityQueue<QueueObj>(10);
		S = new LinkedList<QueueObj>();
		buildings = new LinkedList<Vector3f>();
		
		count = 0;
		total = 0;
		lineCount = 2000;
		
		Box b = new Box(0.7f, 1, 0.7f); //square buildings because logic
		box1 = new Geometry("building", b);
		Box b2 = new Box(1, 3, 1);
		box2 = new Geometry("building2", b2);
		Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/ShowNormals.j3md");
		box1.setMaterial(mat);
		box2.setMaterial(mat);
		
		//initial state of the system is 2 lines pointing in different directions from each other <--+-->
		QueueObj o = new QueueObj(0, LineType.BIG, new Vector3f(0,0,0), new Vector3f(0,0,40), "startA"); //TODO this length defines road length throughout
		QueueObj o2 = new QueueObj(0, LineType.BIG, new Vector3f(0,0,0), new Vector3f(0,0,-40), "startB");
		
		//draw the starting point..
		drawMeAVerySmallBox(new Vector3f(0,0.25f,0), ColorRGBA.White);
		
		Q.add(o);
		Q.add(o2);
	}
	
	public void update(float tpf) {
		total += tpf;
//		int prevCount = count;
		
		while (count < lineCount && !Q.isEmpty() && Q.peek().time < total) {
			QueueObj q = Q.poll();
			
			ConstraintReason reason = localConstraints(q);
			if (reason != ConstraintReason.Overlap) {
				S.add(q);
				place(q);
				
				if (DEBUG)
					Log.p("|Placing a line with rule: " + q.rule);
				
				count++;
				
				if (reason == ConstraintReason.NearPoint)
					continue; //a near point road gets no points
				
				for (QueueObj c: globalGoals(q))
					Q.add(c);
			}
		}
		
		if (lineCount == count) {
			count++; //hacky thing to make this occur once
			for (int i = 0; i < lineCount; i++) {
				
//		if (prevCount != count) { //a building a line
//			for (int i = prevCount; i < count; i++) {
				QueueObj o = S.get(i);
				Vector3f p = canPlaceBuilding(o.end.add(FastMath.nextRandomFloat()*5,0,FastMath.nextRandomFloat()*5), 2);
				if (p != null) {
					buildings.add(p);
					
					Geometry boxNow = box1.clone();
					boxNow.setLocalTranslation(p);
					rootNode.attachChild(boxNow);
					Log.p("|Placing building at: " + p);
				}
			}
			
//			GeometryBatchFactory.optimize(rootNode);
		}
	}
	
	enum ConstraintReason {
		Ok,
		Overlap,
		NearPoint
		;
	}
	private ConstraintReason localConstraints(QueueObj q) {
		float length = q.start.subtract(q.end).length();
		//yay O(2*n)
		for (QueueObj o: S) {
			//check if the point is close to another
			if (IsNear(q.end, o.end, length/2)) {
				q.end = o.end;
				return ConstraintReason.NearPoint;
			}
			
			//if it overlaps another line, don't bother
			//TODO join
			boolean a = isIntersecting(q.start, q.end, o.start, o.end);
			if (a) return ConstraintReason.Overlap;
		}
		
		return ConstraintReason.Ok;
	}

	private Vector3f canPlaceBuilding(Vector3f p, float size) {
		for (QueueObj o: S) {
			Vector3f diff = o.end.subtract(p);
			if (diff.lengthSquared() < size*size) { //'circle collision' mode
				diff.y = 0;
				return null;
			}
			
			float d1 = o.start.subtract(p).length();
			float d2 = o.end.subtract(p).length();
			float dis = d1 + (d2-d1)/2;
			if (dis < size) {
				return null;
			}
			
			//try again?
		}
		
		for (Vector3f b: buildings) {
			if (b.subtract(p).lengthSquared() < size*size)
				return null; //too close to another building
		}
		return p;
	}
	
	private boolean IsNear(Vector3f p, Vector3f p2, float distance) {
		Vector3f diff = p.subtract(p2);
		return diff.length() <= distance;
	}
	private boolean isIntersecting(Vector3f a, Vector3f b, Vector3f c, Vector3f d) {
		if (a.equals(b) || c.equals(d))
			return false; //no length lines cannot intersect
		if (a.equals(c) || a.equals(d) || b.equals(c) || b.equals(d))
			return false; //share ends
		
		//http://gamedev.stackexchange.com/a/26022
		float denominator = ((b.x - a.x) * (d.z - c.z)) - ((b.z - a.z) * (d.x - c.x));
	    float numerator1 = ((a.z - c.z) * (d.x - c.x)) - ((a.x - c.x) * (d.z - c.z));
	    float numerator2 = ((a.z - c.z) * (b.x - a.x)) - ((a.x - c.x) * (b.z - a.z));
	
	    // Detect coincident lines (has a problem)
	    if (denominator == 0) return numerator1 == 0 && numerator2 == 0;
	
	    float r = numerator1 / denominator;
	    float s = numerator2 / denominator;
	
	    return (r >= 0 && r <= 1) && (s >= 0 && s <= 1);
	}
	
	//http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
	//also another way for reference: http://stackoverflow.com/a/16260220
	@SuppressWarnings("unused")
	private boolean pointInQuad(Vector3f p, Vector3f[] quad) {
		int i, j;
		boolean result = false;
	    for (i = 0, j = quad.length - 1; i < quad.length; j = i++) {
	    	if ((quad[i].y > p.y) != (quad[j].y > p.y) && (p.x < (quad[j].x - quad[i].x) * (p.y - quad[i].y) / (quad[j].y-quad[i].y) + quad[i].x)) {
	    		result = !result;
	    	}
	    }
	    return result;
	}

	private QueueObj[] globalGoals(QueueObj q) {
		
		List<QueueObj> list = new LinkedList<QueueObj>();

		Vector3f dir = q.end.subtract(q.start);

		float angle = FastMath.rand.nextFloat()*4 - 2;
		if (q.type == LineType.SMALL)
			angle *= 2;
		
		Vector3f newEnd = new Quaternion().fromAngleAxis(FastMath.DEG_TO_RAD*angle, Vector3f.UNIT_Y).mult(dir);
		QueueObj q1 = new QueueObj(q.time+FastMath.nextRandomFloat(), q.type, q.end, q.end.add(newEnd), null);
		list.add(q1);
		
		int rand = FastMath.nextRandomInt();
		if (rand % 6 == 0) {
			QueueObj q2 = new QueueObj(q.time+FastMath.nextRandomFloat(), LineType.SMALL, q.end, q.end.add(ROT90.mult(dir)), null);
			list.add(q2);
		}
		if (rand % 6 == 1) {
			QueueObj q2 = new QueueObj(q.time+FastMath.nextRandomFloat(), LineType.SMALL, q.end, q.end.add(ROT90.mult(dir.negate())), null);
			list.add(q2);
		}
		
		if (q.type == LineType.BIG) {
			if (rand % 37 == 0) { //split the highway
				QueueObj q2 = new QueueObj(q.time+FastMath.nextRandomFloat(), LineType.BIG, q.end, q.end.add(ROT90.mult(dir)), null);
				list.add(q2);
			}
			if (rand % 39 == 0) { //do both ways
				QueueObj q3 = new QueueObj(q.time+FastMath.nextRandomFloat(), LineType.BIG, q.end, q.end.add(ROT90.mult(dir.negate())), null);
				list.add(q3);
			}
		}
		
		return list.toArray(new QueueObj[]{});
	}

	private void place(QueueObj obj) {
		Vector3f dir = obj.end.subtract(obj.start);
		List<Vector3f> list = Arrays.asList(new Vector3f[] { 
				obj.start, obj.start.add(dir.mult(0.4f)), 
				obj.start.add(dir.mult(0.6f)), obj.end });
		RoadMesh m = new RoadMesh(dir.length()/12, 2, list);
		
		Geometry geometry = new Geometry("line", m);
		Material mat = new Material(this.app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", obj.type.colour);
		geometry.setMaterial(mat);
		rootNode.attachChild(geometry);
		
		CollisionShape col = CollisionShapeFactory.createMeshShape(geometry);
		RigidBodyControl landscape = new RigidBodyControl(col, 0);
		((App)this.app).getPhysicsSpace().add(landscape);
		
		/*
        Line line = new Line(obj.start, obj.end);
        geometry = new Geometry("line", line);
        mat = new Material(App.rally.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Gray);
        mat.getAdditionalRenderState().setLineWidth(obj.type.lineWidth);
        geometry.setMaterial(mat);
        rootNode.attachChild(geometry);
		*/
		
		if (DEBUG) {
//			Log.p("|Drawing line, " + obj.start + " " + obj.end + ", colour:" + obj.type.colour);
			drawMeAVerySmallBox(obj.end, ColorRGBA.White);
			
			drawMeAVerySmallBox(list.get(0), ColorRGBA.Red);
			drawMeAVerySmallBox(list.get(1), ColorRGBA.Brown);
			drawMeAVerySmallBox(list.get(2), ColorRGBA.Gray);
			drawMeAVerySmallBox(list.get(3), ColorRGBA.Blue);
		}
	}
	
	private void drawMeAVerySmallBox(Vector3f pos, ColorRGBA colour) {
		Box b = new Box(0.1f, 0.1f, 0.1f);
		Geometry geometry = new Geometry("box", b);
		Material mat = new Material(this.app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", colour);
		geometry.setMaterial(mat);
		geometry.setLocalTranslation(pos);
		rootNode.attachChild(geometry);
	}
	
	class QueueObj implements Comparable<QueueObj> {
		float time; //when it should be added (randomish)
		LineType type;
		Vector3f start;
		Vector3f end;
		String rule;
		
		QueueObj(float time, LineType type, Vector3f start, Vector3f end, String rule) {
			this.time = time;
			this.type = type;
			
			this.start = start;
			this.start.y = height;
			this.end = end;
			this.end.y = height;
			
			this.rule = rule;
		}
		
		@Override
		public int compareTo(QueueObj arg0) {
			return (int) FastMath.sign(this.time - arg0.time);
		}
	}
	
	enum LineType {
		BIG (4, ColorRGBA.Black),
		SMALL (2, ColorRGBA.Red);
		
		int lineWidth;
		ColorRGBA colour;
		
		LineType(int width, ColorRGBA colour) {
			this.lineWidth = width;
			this.colour = colour;
		}
	}
}
