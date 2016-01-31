package game;

import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
import com.jme3.scene.control.CameraControl.ControlDirection;

public class MyCamera extends CameraNode {
	
	private MyVC p;
	Rally r;
	
	private float damping;
	
	MyCamera(String name, Camera c, MyVC p, Rally r) {
		super(name, c);
		this.p = p;
		this.r = r;
		
		this.damping = 30;
		
		Vector3f pPos = p.getPhysicsLocation();
		setLocalTranslation(pPos.add(p.car.CAM_OFFSET)); //starting position of the camera
		lookAt(pPos.add(p.car.LOOK_AT), new Vector3f(0,1,0)); //look at car
		
		setControlDir(ControlDirection.SpatialToCamera);
	}
	
	
	//I may have just made the default camera again :(
	public void myUpdate(float tpf) {
		Vector3f wantPos = null;
		Vector3f lookPos = new Vector3f(p.car.CAM_OFFSET);
		if (p.ifLookBack) {
			lookPos.x *= -1;
			lookPos.z *= -1;
		} else if (p.ifLookSide) {
			Matrix3f p = new Matrix3f();
			p.fromAngleAxis(FastMath.DEG_TO_RAD*90, new Vector3f(0,1,0));
			lookPos = p.mult(lookPos);
		}
		wantPos = p.getPhysicsLocation().add(p.getPhysicsRotation().mult(lookPos));
		Vector3f curPos = getLocalTranslation();
		
		Vector3f pos = FastMath.interpolateLinear(tpf*damping, curPos, wantPos);
		
		setLocalTranslation(pos);
		lookAt(p.getPhysicsLocation().add(p.car.LOOK_AT), new Vector3f(0,1,0));
		
	}

}
