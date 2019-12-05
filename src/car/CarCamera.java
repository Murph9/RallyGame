package car;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Ray;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.RenderManager;
import com.jme3.scene.Geometry;

import car.data.CarDataConst;
import car.ray.RayCarControl;
import helper.H;

public class CarCamera extends BaseAppState implements RawInputListener {

	private static final float ROT_SPEED = 0.008f;
	private static final float CAM_TIMEOUT = 3;

	private Camera c;
	private RayCarControl p;
	private Vector3f lastPos;
	
	private float tpf;
	private float lastTimeout;
	private float rotRad;
	private float lastRad;
	
	private Vector3f lastShake = new Vector3f();
	
	public CarCamera(String name, Camera c, RayCarControl p) {
		super();
		
		this.c = c;
		
		if (p != null) {
			this.p = p;
			Vector3f pPos = p.getPhysicsLocation();
			Vector3f cam_offset = new Vector3f(0, p.getCarData().cam_offsetHeight, p.getCarData().cam_offsetLength);
			c.setLocation(cam_offset); //starting position of the camera
			Vector3f cam_lookAt = new Vector3f(0, p.getCarData().cam_lookAtHeight, 0);
			c.lookAt(pPos.add(cam_lookAt), new Vector3f(0,1,0)); //look at car
		}
	}

	@Override
	public void initialize(Application app) {
	}

	@Override
	public void update(float tpf) {
		if (p == null) {
			return;
		}
		this.tpf = tpf;

		super.update(tpf);
	}
	
	
	public void setCar(RayCarControl p) {
		this.p = p;
	}
	
	@Override
	public void render(RenderManager rm) {
		if (p == null)
			return;

		//TODO: react to g forces
		
		Vector3f carPos = p.getRootNode().getLocalTranslation();
		Quaternion pRot = p.getRootNode().getLocalRotation();
		
		CarDataConst data = p.getCarData();

	
		if (!FastMath.approximateEquals(rotRad, 0)) {
			lastTimeout += tpf;
			if (lastTimeout > CAM_TIMEOUT) {
				rotRad *= tpf; //reset to back of car slowly
			}
			
			Quaternion q = new Quaternion();
			q.fromAngleAxis((rotRad-lastRad)*ROT_SPEED, p.up);

			if (lastPos == null)
				lastPos = Vector3f.UNIT_Z;
			lastPos = q.mult(lastPos);
			lastRad = rotRad;

		} else {
			//calculate world pos of a camera
			Vector3f vec = new Vector3f();
			float smoothing = tpf*10;
			if (p.vel.length() > 4f)
				vec.interpolateLocal(pRot.mult(Vector3f.UNIT_Z).normalize(), p.vel.normalize(), 0.5f);
			else {
				//at slow speeds use just the rotation
				vec = pRot.mult(Vector3f.UNIT_Z).normalize();
				//reduce interpolation to much slower
				smoothing /= 3;
			}

			//make it smooth
			if (lastPos == null)
				lastPos = vec;
			lastPos.interpolateLocal(vec, smoothing);
		}

		//force it to be the same distance away at all times
		Vector3f next = new Vector3f();
		Vector2f vec_2 = H.v3tov2fXZ(lastPos).normalize();
		
		next.x = vec_2.x * data.cam_offsetLength;
		next.y = data.cam_offsetHeight; //ignore y last
		next.z = vec_2.y * data.cam_offsetLength;
		
		next = carPos.add(next);
		
		c.setLocation(next);

		//do a ray cast to make sure that you can still see the car
		CollisionResults results = new CollisionResults();
		Vector3f cam_lookAt = new Vector3f(0, data.cam_lookAtHeight, 0);
		Vector3f dir = c.getLocation().subtract(carPos.add(cam_lookAt));
		Ray ray = new Ray(carPos.add(cam_lookAt), dir);
		((SimpleApplication)getApplication()).getRootNode().collideWith(ray, results);
		CollisionResult cr = results.getClosestCollision();
		if (cr != null && cr.getDistance() < dir.length()) {
			Geometry g = cr.getGeometry();
			if (!H.hasParentNode(g, p.getRootNode())) { //don't collide with the car TODO doesn't work
				c.setLocation(cr.getContactPoint());
			}
		}
		
		//at high speeds shake the camera a little TODO not the motion sickness type
		float shakeFactor = p.vel.length() * p.vel.length() * p.getCarData().cam_shake;
		Vector3f lookAt = carPos.add(cam_lookAt);
		lastShake.addLocal(new Vector3f(FastMath.nextRandomFloat(), FastMath.nextRandomFloat(), FastMath.nextRandomFloat()).normalize().mult(shakeFactor*FastMath.nextRandomInt(-1, 1)));
		if (lastShake.length() > 0.01f)
			lastShake.interpolateLocal(Vector3f.ZERO, 0.3f);
		else
			lastShake = new Vector3f();
		
		lookAt.addLocal(lastShake);
		
		c.lookAt(lookAt, new Vector3f(0,data.cam_lookAtHeight,0));
	}
	
	
	public void beginInput() {}
	public void endInput() {}
	
	public void onMouseButtonEvent(MouseButtonEvent arg0) {}
	public void onMouseMotionEvent(MouseMotionEvent arg0) {
		if (isEnabled()) {
			lastTimeout = 0;
			rotRad += arg0.getDX();
		}
	}
	public void onKeyEvent(KeyInputEvent arg0) {}
	public void onTouchEvent(TouchEvent arg0) {}
	public void onJoyAxisEvent(JoyAxisEvent arg0) {}
	public void onJoyButtonEvent(JoyButtonEvent arg0) {}


	@Override
	protected void onEnable() {
	}
	@Override
	protected void onDisable() {
	}
	@Override
	protected void cleanup(Application app) {
	}
}
