package world.wp;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.collision.shapes.PlaneCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.*;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Quad;
import com.jme3.water.SimpleWaterProcessor;

import game.App;

public enum Floating implements WP {
	STRAIGHT("straight.blend", new Vector3f(20,0,0), Quaternion.IDENTITY),
	
	//these weird angles are just to show i can do it...
//	STRAIGHT_UP("straight.blend", new Vector3f(20,0,0), new Quaternion(0, 0, 0.098f, 0.9952f)),
//	STRAIGHT_DOWN("straight.blend", new Vector3f(20,0,0), new Quaternion(0, 0, -0.098f, 0.9952f)),
	
	LEFT_CURVE("left.blend", new Vector3f(21.21f,0,-8.79f), new Quaternion(0, 0.38268346f, 0, 0.9238795f)),
	RIGHT_CURVE("right.blend", new Vector3f(21.21f,0,8.79f), new Quaternion(0, -0.38268346f, 0, 0.9238795f)),
	;
	
	private static String dir = "assets/wb/floating/";
	
	String name;
	Vector3f newPos;
	Quaternion newRot;
	NodeType startNode;
	NodeType endNode;

	Floating(String s, Vector3f a, Quaternion g) {
		this.name = s;
		this.newPos = a;
		this.newRot = g;
		this.startNode = NodeType.A;
		this.endNode = NodeType.A;
	}
	public float getScale() { return 1; }
	public boolean needsMaterial() { return false; }
	
	public String getName() { return dir+name; }
	public Vector3f getNewPos() { return new Vector3f(newPos); }
	public Quaternion getNewAngle() { return new Quaternion(newRot); }
	
	public NodeType startNode() {
		return startNode;
	}
	public NodeType endNode() {
		return endNode;
	}
	
	static class Builder extends DefaultBuilder {
		private SimpleWaterProcessor waterProcessor;
		private Geometry water;
		
		Builder() {
			super(Floating.values());
		}
		
		@Override
		public void initialize(AppStateManager stateManager, Application app) {
			super.initialize(stateManager, app);
			
			// we create a water processor
        	waterProcessor = new SimpleWaterProcessor(App.rally.getAssetManager());
        	// we set wave properties
        	waterProcessor.setRenderSize(256,256);    // performance boost because small (don't know the defaults)
        	waterProcessor.setWaterDepth(40);         // transparency of water
        	waterProcessor.setDistortionScale(0.5f);  // strength of waves (default = 0.2)
        	waterProcessor.setWaveSpeed(0.05f);       // speed of waves
        	waterProcessor.setReflectionScene(App.rally.getRootNode());
        	
        	// we set the water plane
        	Vector3f waterLocation = new Vector3f(0,-6,0);
        	waterProcessor.setPlane(new Plane(Vector3f.UNIT_Y, waterLocation.dot(Vector3f.UNIT_Y)));
        	App.rally.getViewPort().addProcessor(waterProcessor);
        	
        	// we define the wave size by setting the size of the texture coordinates
        	Quad quad = new Quad(4000,4000);
        	quad.scaleTextureCoordinates(new Vector2f(50f,50f));
        	 
        	// we create the water geometry from the quad
        	water = new Geometry("water", quad);
        	water.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
        	water.setLocalTranslation(-2000, -4, 2000);
        	water.setShadowMode(ShadowMode.Receive);
        	water.setMaterial(waterProcessor.getMaterial());
        	rootNode.attachChild(water);
		}
		
		public void cleanup() {
			rootNode.detachChild(water);
			App.rally.getViewPort().removeProcessor(waterProcessor);
			
			super.cleanup();
		}
		

		public DefaultBuilder copy() {
			return new Builder();
		}
	}
}
