package game;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.CameraNode;
import com.jme3.scene.control.CameraControl.ControlDirection;

public class BasicCamera extends CameraNode {

	BasicCamera(String name, Camera c, Vector3f pos, Vector3f lookat) {
		super(name, c);
		
		setLocalTranslation(pos); //starting position of the camera
		lookAt(lookat, new Vector3f(0,1,0));
		setControlDir(ControlDirection.SpatialToCamera);
	}
}
