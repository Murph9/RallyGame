package drive;

import world.wp.DefaultBuilder;
import world.wp.WP.DynamicType;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

import car.ai.FollowWorldAI;
import car.data.Car;
import car.ray.RayCarControl;
import game.BasicCamera;

public class DriveDemo extends DriveBase {

	private BasicCamera basicCam;
	
	private float angle;
	private Vector3f offset;
	private Vector3f lookAtHeight;

	public DriveDemo (Car car) {
		super(car, DynamicType.Valley.getBuilder());
    }
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
    	super.initialize(stateManager, app);
    	
		RayCarControl car = this.cb.get(0);
		this.offset = new Vector3f(0, car.getCarData().cam_offsetHeight, car.getCarData().cam_offsetLength);
		this.lookAtHeight = new Vector3f(0, car.getCarData().cam_lookAtHeight, 0);

		FollowWorldAI ai = new FollowWorldAI(car, (DefaultBuilder)world);
		ai.setMaxSpeed(27.7778f); //27.7 = 100 km/h
    	car.attachAI(ai, true);
    	
    	stateManager.detach(uiNode);

		app.getInputManager().removeRawInputListener(camera);
		stateManager.detach(camera);

		this.basicCam = new BasicCamera("demo camera", app.getCamera(), 
				car.getPhysicsLocation().add(offset), car.getPhysicsLocation().add(lookAtHeight));
		stateManager.attach(this.basicCam);
	}
	
	public void update(float tpf) {
		super.update(tpf);

		angle += tpf;

		RayCarControl car = this.cb.get(0);

		Vector3f movedOffset = new Quaternion().fromAngleAxis(angle, Vector3f.UNIT_Y).mult(this.offset);
		Vector3f camPos = car.getPhysicsLocation().add(movedOffset);

		this.basicCam.updatePosition(camPos, car.getPhysicsLocation().add(lookAtHeight));
	}
}
