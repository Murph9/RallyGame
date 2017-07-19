package car;

import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
import com.jme3.scene.control.CameraControl.ControlDirection;

import helper.H;

public class CarCamera extends CameraNode implements RawInputListener {

	//TODO: seems like nfs most wanted is (velocity + some gforce value + fancy AAA stuff) 
	
	private MyPhysicsVehicle p;
	private Vector3f prevVel; //for smoothing the velocity vector
	private Vector3f prevPos;
	
	private float lastTimeout;
	private float rotRad;
	private static final float ROT_SPEED = 0.01f;
	
	public CarCamera(String name, Camera c, MyPhysicsVehicle p) {
		super(name, c);
		
		if (p != null) {
			this.p = p;
			Vector3f pPos = new Vector3f();
			p.getInterpolatedPhysicsLocation(pPos);
			setLocalTranslation(pPos.add(p.car.cam_offset)); //starting position of the camera
			lookAt(pPos.add(p.car.cam_lookAt), new Vector3f(0,1,0)); //look at car
		}
		
		setControlDir(ControlDirection.SpatialToCamera);
	}

	public void myUpdate(float tpf) {
		if (p == null) {
			return;
		}
		//TODO use the direction of the wheels
		//TODO also react to g forces
		//TODO smooth the mouse stuff
		
		if (prevPos == null) //doesn't exist on the first loop
			prevPos = getLocalTranslation();
		if (prevVel == null) prevVel = p.vel.normalize();
		
		Vector3f carPos = new Vector3f();
		p.getInterpolatedPhysicsLocation(carPos);
		
		Vector3f vec1 = p.getPhysicsRotation().mult(new Vector3f(0, 0, 1)).normalize();
		prevVel.interpolateLocal(p.vel.normalize(), 1f*tpf);
		
		Vector3f vec = new Vector3f();
		vec.interpolateLocal(vec1, prevVel, 0.4f).normalize();
		
		vec.y = 1;
		
		Vector3f next = new Vector3f();
		Vector2f vec_2 = H.v3tov2fXZ(vec).normalize();
		
		next.x = vec_2.x*p.car.cam_offset.z;
		next.y = vec.y*p.car.cam_offset.y;
		next.z = vec_2.y*p.car.cam_offset.z;
		
		next = carPos.add(next);
		next.interpolateLocal(next, prevPos, 0.1f*tpf);//maybe
		prevPos = next; 
		
		setLocalTranslation(prevPos); //already set for next frame
		
		if (rotRad != 0) {
			lastTimeout += tpf;
			if (lastTimeout > 2) { //TODO static number
				rotRad = 0; //reset to back of car
			}
			
			Quaternion q = new Quaternion();
			q.fromAngleAxis(rotRad*ROT_SPEED, p.up);
			
			Vector3f carForward = new Vector3f();
			p.getForwardVector(carForward);
			carForward.normalizeLocal();
			setLocalTranslation(carPos.add(q.mult(carForward)));
		}

		lookAt(carPos.add(p.car.cam_lookAt), new Vector3f(0,1,0));
	}
	
	
	public void beginInput() {}
	public void endInput() {}
	
	public void onMouseButtonEvent(MouseButtonEvent arg0) {}
	public void onMouseMotionEvent(MouseMotionEvent arg0) {
		lastTimeout = 0;
		rotRad += arg0.getDX();
	}
	public void onKeyEvent(KeyInputEvent arg0) {}
	public void onTouchEvent(TouchEvent arg0) {}
	public void onJoyAxisEvent(JoyAxisEvent arg0) {}
	public void onJoyButtonEvent(JoyButtonEvent arg0) {}

}
