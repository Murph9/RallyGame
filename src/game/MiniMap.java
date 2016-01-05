package game;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;

public class MiniMap {

	Rally r;
	Camera cam;
	
	final float height = 150;
	
	MiniMap(Rally r) {
		this.r = r;
		
//		AppSettings set = r.getSettings();
		
		cam = r.getCam().clone();
//		cam = new Camera(set.getWidth(),set.getHeight());
//		cam.setParallelProjection(true); //TODO
//		cam.setFrustum(0, 5, -20, 20, -20, 20);
		
		cam.setViewPort(0.05f, 0.15f, 0.05f, 0.2f);
		cam.setLocation(new Vector3f(0, height, 0));
		cam.lookAt(r.player.getPhysicsLocation(), new Vector3f(0,1,0)); //look at car
		
		ViewPort miniview = r.getRenderManager().createMainView("Mini Map", cam);
		miniview.setClearFlags(true, true, true);
		miniview.attachScene(r.getRootNode());
		miniview.clearProcessors(); //no need for shadows on map
	}
	
	public void update(float tpf) {
		Vector3f pos = r.player.getPhysicsLocation();
		cam.lookAt(pos, r.player.forward);
		
		pos.addLocal(0,height,0);
		cam.setLocation(pos);
		
	}
}
