package game;

import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;

import car.ray.RayCarControl;

public class MiniMap { //TODO appstate

	//TODO (how do we keep this fixed?)
	//	might have to map it onto a an actual object as a texture
	
	public Node rootNode;
	
	RayCarControl target;
	ViewPort viewport;
	Camera cam;
	
	final float height = 100;
	
	MiniMap(RayCarControl target) {
		this.target = target;
		Main r = App.rally;
		
		Camera c = r.getCamera();
		cam = new Camera((int)(c.getWidth()*0.2),(int)(c.getHeight()*0.2f)); 
		cam.setViewPort(0f, 1f, 0f, 1f);
		cam.setParallelProjection(true);
		
		float asp = c.getWidth()/c.getHeight();
		cam.setFrustumPerspective(75, asp, 10, 1000);
		
		viewport = r.getRenderManager().createMainView("MiniMap", cam);
		
		viewport.setClearFlags(true, true, true);
		
		rootNode = new Node();
		r.getRootNode().attachChild(rootNode);
		
		viewport.attachScene(rootNode);//set what it can see here
		viewport.clearProcessors();

		viewport.attachScene(r.getRootNode());
		cam.update();
	}
	
	public void update(float tpf) {
		Vector3f pos = target.getPhysicsLocation();
		cam.lookAt(pos, target.forward);
		
		pos.addLocal(0,height,0);
		cam.setLocation(pos);
	}
}
