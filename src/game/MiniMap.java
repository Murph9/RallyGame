package game;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;

public class MiniMap {

	Rally r;
	Camera cam;
	ViewPort view;
	
	final float height = 100;
	
	MiniMap(Rally r) {
		this.r = r;
		
		cam = r.getCam().clone(); //TODO this just copies the current camera, causes weird water things
		
		cam.setViewPort(0f, 0.2f, 0f, 0.2f);
		cam.setLocation(new Vector3f(0, height, 0));
		
		
		view = r.getRenderManager().createMainView("MiniMap", cam);
		view.setClearFlags(true, true, true);
		view.clearProcessors();

		view.attachScene(r.getRootNode());
	}
	
	//TODO also fix the scene here so the water isn't so distracting
	public void update(float tpf) {
		Vector3f pos = r.cb.get(0).getPhysicsLocation();
		Vector3f f = new Vector3f();
		cam.lookAt(pos, r.cb.get(0).getForwardVector(f));
		
		pos.addLocal(0,height,0);
		cam.setLocation(pos);
	}
}
