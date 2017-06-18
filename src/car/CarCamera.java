package car;

import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
import com.jme3.scene.control.CameraControl.ControlDirection;

public class CarCamera extends CameraNode implements RawInputListener {

	//TODO: seems like nfs most wanted is (velocity + some gforce value + fancy AAA stuff) 
	
	private MyPhysicsVehicle p;
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
		//TODO smooth the mouse stuff
		
		if (prevPos == null) //its needed but not on the first loop apparently
			prevPos = getLocalTranslation();
		
		Vector3f nextPos = p.getPhysicsRotation().mult(p.car.cam_offset);
		Vector3f carPos = new Vector3f();
		p.getInterpolatedPhysicsLocation(carPos);
		
        //combine prev and vector 
		prevPos = carPos.add(nextPos);
		setLocalTranslation(prevPos);
		
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
