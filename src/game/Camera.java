package game;

import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.CameraNode;
import com.jme3.scene.control.CameraControl.ControlDirection;

public class Camera extends CameraNode {
	
	private MyVehicleControl p;
	
	private float damping;
	
	Camera(String name, com.jme3.renderer.Camera c, MyVehicleControl p) {
		super(name, c);
		this.p = p;
		
		this.damping = 30;
		
		Vector3f pPos = p.getPhysicsLocation();
		setLocalTranslation(pPos.add(p.car.CAM_OFFSET)); //starting position of the camera
		lookAt(pPos.add(p.car.LOOK_AT), Vector3f.UNIT_Y); //look at car
		
		setControlDir(ControlDirection.SpatialToCamera);
	}
	
	
	//I may have just made the default camera again :(
	public void myUpdate(float tpf) {
		Vector3f wantPos = p.getPhysicsLocation().add(p.getPhysicsRotation().mult(p.car.CAM_OFFSET));
		Vector3f curPos = getLocalTranslation();
		
		setLocalTranslation(FastMath.interpolateLinear(tpf*damping, curPos, wantPos));
		
		lookAt(p.getPhysicsLocation().add(p.car.LOOK_AT), Vector3f.UNIT_Y);
	}

}
