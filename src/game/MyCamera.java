package game;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
import com.jme3.scene.control.CameraControl.ControlDirection;

import car.MyPhysicsVehicle;

public class MyCamera extends CameraNode {

	private MyPhysicsVehicle p;
	private Vector3f prevPos;
	
	MyCamera(String name, Camera c, MyPhysicsVehicle p) {
		super(name, c);
		
		if (p != null) {
			this.p = p;

			Vector3f pPos = p.getPhysicsLocation();
			setLocalTranslation(pPos.add(p.car.cam_offset)); //starting position of the camera
			lookAt(pPos.add(p.car.cam_lookAt), new Vector3f(0,1,0)); //look at car
		}
		setControlDir(ControlDirection.SpatialToCamera);
	}

	public void myUpdate(float tpf) {
		if (p == null) {
			return;
		}
		if (prevPos == null) //its needed but not on the first loop apparently
			prevPos = getLocalTranslation();
		
		float distance = p.car.cam_offset.length();
		Vector3f carForward = new Vector3f();
		p.getForwardVector(carForward);
		carForward = carForward.mult(distance).add(0, p.car.cam_offset.y, 0);
		
		Vector3f camPos = prevPos;
		Vector3f carPos = p.getPhysicsLocation();
		
		Vector3f diff = carPos.subtract(camPos);
		if (diff.length() > distance) {
			diff = diff.normalize().mult(distance);
		}

		prevPos = carPos.add(diff.add(0, p.car.cam_offset.y*2, 0));
		setLocalTranslation(prevPos);
		
		if (p.ifLookBack)
			setLocalTranslation(carPos.add(carForward));
		
        if (p.ifLookSide) {
			Quaternion q = new Quaternion();
			q.fromAngleAxis(90*FastMath.DEG_TO_RAD, Vector3f.UNIT_Y); 
			setLocalTranslation(carPos.add(q.mult(carForward)));
        }

		lookAt(p.getPhysicsLocation().add(p.car.cam_lookAt), new Vector3f(0,1,0));
	}
}
