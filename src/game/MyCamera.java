package game;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
import com.jme3.scene.control.CameraControl.ControlDirection;

public class MyCamera extends CameraNode {
	
	private MyPhysicsVehicle p;
	private float damping;
	
	MyCamera(String name, Camera c, MyPhysicsVehicle p) {
		super(name, c);
		this.p = p;
		
		this.damping = 50; //just high so it has no delay
		
		Vector3f pPos = p.getPhysicsLocation();
		setLocalTranslation(pPos.add(p.car.CAM_OFFSET)); //starting position of the camera
		lookAt(pPos.add(p.car.LOOK_AT), new Vector3f(0,1,0)); //look at car
		
		setControlDir(ControlDirection.SpatialToCamera);
		
		
	}
	
	public void myUpdate(float tpf) {
//		Vector3f wantPos = null;
//		Vector3f lookPos = new Vector3f(p.car.CAM_OFFSET);
//		if (p.ifLookBack) {
//			lookPos.x *= -1;
//			lookPos.z *= -1;
//		} else if (p.ifLookSide) {
//			Matrix3f p = new Matrix3f();
//			p.fromAngleAxis(FastMath.DEG_TO_RAD*90, new Vector3f(0,1,0));
//			lookPos = p.mult(lookPos);
//		}
//		wantPos = p.getPhysicsLocation().add(p.getPhysicsRotation().mult(lookPos));
//		wantPos.y = p.getPhysicsLocation().y+p.car.CAM_OFFSET.y;//always want to be above the player.
//		Vector3f curPos = getLocalTranslation();
//		
//		Vector3f pos = FastMath.interpolateLinear(tpf*damping, curPos, wantPos);
//		
//		setLocalTranslation(pos);
//		lookAt(p.getPhysicsLocation().add(p.car.LOOK_AT), new Vector3f(0,1,0));
//		
		
		// try 2
		Vector3f curPos = getLocalTranslation();
		Vector3f back = new Vector3f();
		p.getForwardVector(back);
		
		if (!p.ifLookBack)
			back.negateLocal();
		
		back.normalizeLocal();
		back.y = 0.4f;
		back.normalizeLocal();
		back.multLocal(8);
		
		Vector3f wantPos = p.getPhysicsLocation().add(back);
		Vector3f pos = FastMath.interpolateLinear(tpf*damping, curPos, wantPos);
		setLocalTranslation(pos);
		
		lookAt(p.getPhysicsLocation().add(p.car.LOOK_AT), new Vector3f(0,1,0));
	}

}
