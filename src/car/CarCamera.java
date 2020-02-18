package car;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.bullet.control.GhostControl;
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
import com.jme3.scene.Spatial;

import car.data.CarDataConst;
import car.ray.RayCarControl;
import helper.Geo;
import helper.H;
import service.averager.AverageV3f;
import service.averager.IAverager;
import service.averager.IAverager.Type;

public class CarCamera extends BaseAppState implements RawInputListener {

	private static final float ROT_SPEED = 0.008f;
	private static final float CAM_TIMEOUT = 3;

	private final Camera c;
    private final RayCarControl p;
    private final IAverager<Vector3f> gAverage;
	private Vector3f lastPos;
	
	private float tpf;
	private float lastTimeout;
	private float rotRad;
	private float lastRad;
	
	private Vector3f lastShake = new Vector3f();
	
	public CarCamera(Camera c, RayCarControl p) {
		super();
        this.c = c;
        this.p = p;
        this.gAverage = new AverageV3f(25, Type.Simple);
	}

	@Override
	public void initialize(Application app) {
        Vector3f pPos = p.location;
        Vector3f cam_offset = new Vector3f(0, p.getCarData().cam_offsetHeight, p.getCarData().cam_offsetLength);
        c.setLocation(cam_offset); // starting position of the camera
        Vector3f cam_lookAt = new Vector3f(0, p.getCarData().cam_lookAtHeight, 0);
        c.lookAt(pPos.add(cam_lookAt), new Vector3f(0, 1, 0)); // look at car
	}

    public void resetMouseView() {
        rotRad = 0;
    }

	@Override
	public void update(float tpf) {
		this.tpf = tpf;

		super.update(tpf);
	}
		
	@Override
	public void render(RenderManager rm) {
		Vector3f carPos = p.getRootNode().getLocalTranslation();
		Quaternion carRot = p.getRootNode().getLocalRotation();
			
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
				vec.interpolateLocal(carRot.mult(Vector3f.UNIT_Z).normalize(), p.vel.normalize(), 0.5f);
			else {
				//at slow speeds use just the rotation
				vec = carRot.mult(Vector3f.UNIT_Z).normalize();
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
        
        CarDataConst data = p.getCarData();
		next.x = vec_2.x * data.cam_offsetLength;
		next.y = data.cam_offsetHeight; //ignore y last
		next.z = vec_2.y * data.cam_offsetLength;
		
		next = carPos.add(next);
		
		c.setLocation(next);

        Vector3f cam_lookAt = new Vector3f(0, data.cam_lookAtHeight, 0);

        // move camera closer if there is an object in the way
        Vector3f newPos = posOfThingInWay(cam_lookAt, carPos);
        if (newPos != null) {
            c.setLocation(newPos);
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
        
        //and g force reactions by moving it up/down, left or right
        Vector3f gs = p.planarGForce.mult(1 / p.getPhysicsObject().getGravity().length());
        gs.y = gs.z; // z is front back, convert to up/down
        gs.z = 0;
        gs.multLocal(tpf);
        gs = gAverage.get(gs);
        // lookAt.addLocal(gs); //TODO: react to g forces better (its really jumpy)
		
		c.lookAt(lookAt, new Vector3f(0, 1, 0));
	}
    
    public void onMouseMotionEvent(MouseMotionEvent arg0) {
        if (isEnabled()) {
            lastTimeout = 0;
            rotRad += arg0.getDX();
        }
    }

    private Vector3f posOfThingInWay(Vector3f cam_lookAt, Vector3f carPos) {
        // do a ray cast to make sure that you can still see the car
        Vector3f dir = c.getLocation().subtract(carPos.add(cam_lookAt));
        Ray ray = new Ray(carPos.add(cam_lookAt), dir);
        CollisionResults results = new CollisionResults();
        ((SimpleApplication) getApplication()).getRootNode().collideWith(ray, results);
        
        for (CollisionResult result: results) {
            if (result != null && result.getDistance() < dir.length()) {
                Geometry g = result.getGeometry();
                // ignore GhostObjects like checkpoints
                if (!hasGhostControl(g)) {
                    // attempt to not collide with the car TODO doesn't work
                    if (!Geo.hasParentNode(g, p.getRootNode())) {
                        return result.getContactPoint();
                    }
                }
            }
        }

        return null;
    }

    private boolean hasGhostControl(Spatial s) {
        while (s != null) {
            if (s.getControl(GhostControl.class) != null)
                return true;
            s = s.getParent();
        }
        return false;
    }

	//#region unused methods
	public void beginInput() {}
	public void endInput() {}
	
	public void onMouseButtonEvent(MouseButtonEvent arg0) {}
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
    //#endregion
}
