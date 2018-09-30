package car.ray;

import com.jme3.scene.Node;

public class RayWheelControl {

	private final RayWheel wheel;
	
	public RayWheelControl(RayWheel wheel, Node carRootNode) {
		this.wheel = wheel;
		//TODO use carRootNode
	}

	//hopefully called by the FakeRayCarControl
	public void update(float tpf) {
		//TODO
//		H.e("update wheel: "+ wheel.num);
	}
	
	public RayWheel getRayWheel() {
		return wheel;
	}
}
