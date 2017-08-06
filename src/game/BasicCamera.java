package game;

import com.jme3.app.state.AbstractAppState;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

public class BasicCamera extends AbstractAppState {

	private Camera c;
	
	public BasicCamera(String name, Camera c, Vector3f pos, Vector3f lookat) {
		super();
		
		this.c = c;
		this.c.setLocation(pos); //starting position of the camera
		this.c.lookAt(lookat, new Vector3f(0,1,0));
	}
}
