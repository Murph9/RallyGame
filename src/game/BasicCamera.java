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
		this.c.lookAt(lookat, Vector3f.UNIT_Y);
	}

	public void updatePosition(Vector3f pos, Vector3f lookAt) {
		this.c.setLocation(pos);
		this.c.lookAt(lookAt, Vector3f.UNIT_Y);

		//Please avoid changing the camera too often 
		//change to use the same logic as CarCamera.render
		//otherwise the camera kind of jumps around on unstable framerates
	}
}
