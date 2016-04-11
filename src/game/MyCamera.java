package game;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
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
		
		this.damping = 20; //just high so it has no delay
		
		Vector3f pPos = p.getPhysicsLocation();
		setLocalTranslation(pPos.add(p.car.CAM_OFFSET)); //starting position of the camera
		lookAt(pPos.add(p.car.LOOK_AT), new Vector3f(0,1,0)); //look at car
		
		setControlDir(ControlDirection.SpatialToCamera);
		
		
	}
	
	//TODO some kind of actual stabilisation
	public void myUpdate(float tpf) {
		Vector3f curPos = getLocalTranslation();
		Vector3f forw = new Vector3f();
		Vector3f vel = new Vector3f();
		p.getForwardVector(forw); //look in the direction you are facing
		p.getLinearVelocity(vel); //look in the direction you are moving
		
		Vector3f back = forw.normalize().interpolate(vel.normalize(), 0.5f); //average of them both
		
		if (!p.ifLookBack)
			back.negateLocal();
		
		if (p.ifLookSide) {
			Quaternion q = new Quaternion();
			q.fromAngleAxis(90*FastMath.DEG_TO_RAD, Vector3f.UNIT_Y); 
			back = q.mult(back);
		}
		
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
