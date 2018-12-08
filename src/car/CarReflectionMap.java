package car;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureCubeMap;

import car.ray.RayCarControl;
import game.App;

public class CarReflectionMap extends AbstractAppState {

	private static String VIEW_PORT_NAME = "car_reflection_map";
	private static int CAMERA_COUNT = 6;
	
	private Vector3f[] dir = new Vector3f[6];
	private Vector3f[] up = new Vector3f[6];
	private ViewPort[] offViews = new ViewPort[6];
	private Camera[] cams = new Camera[6];
	protected FrameBuffer[] framebuffers = new FrameBuffer[6];
	private TextureCubeMap offTex;

	private RayCarControl car;
	
	public CarReflectionMap(RayCarControl car, TextureCubeMap textureMap) {
		this.car = car;

		//setup framebuffer's texture
		this.offTex = textureMap;
        offTex.setMagFilter(Texture.MagFilter.Bilinear);
        offTex.setMinFilter(Texture.MinFilter.BilinearNoMipMaps);
        offTex.setAnisotropicFilter(0);
        offTex.setWrap(Texture.WrapMode.EdgeClamp);
 
		//TODO
		/*
		https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-examples/src/main/java/jme3test/light/pbr/TestPBRLighting.java
		https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-core/src/main/java/com/jme3/environment/EnvironmentCamera.java
		https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-core/src/main/java/com/jme3/environment/LightProbeFactory.java
		https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-testdata/src/main/resources/Models/Tank/tank.j3m
		https://hub.jmonkeyengine.org/t/how-to-get-blender-s-material-name/19939
	 	*/
	}
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		up[0] = Vector3f.UNIT_Y;
		up[1] = Vector3f.UNIT_Y;
		up[2] = Vector3f.UNIT_Z;
		up[3] = Vector3f.UNIT_Z.mult(-1);
		up[4] = Vector3f.UNIT_Y;
		up[5] = Vector3f.UNIT_Y;
		
		//TODO
		dir[0] = Vector3f.UNIT_Z.mult(-1);
		dir[1] = Vector3f.UNIT_Z;
		
		dir[2] = Vector3f.UNIT_Y;
		dir[3] = Vector3f.UNIT_Y.mult(-1);
		
		dir[4] = Vector3f.UNIT_X.mult(-1);
		dir[5] = Vector3f.UNIT_X;
		
		for (int i = 0; i < CAMERA_COUNT; i++) {
			cams[i] = new Camera(512, 512);	
			
			offViews[i] = app.getRenderManager().createPreView(VIEW_PORT_NAME+i, cams[i]);
	        offViews[i].setClearFlags(true, true, true);
	        offViews[i].setBackgroundColor(ColorRGBA.DarkGray);
	        offViews[i].attachScene(App.rally.getRootNode());
	        
	        //setup framebuffer's cam
	        cams[i].setFrustumPerspective(90f, 1f, 2f, 1000f); //does this always avoid car visual collision? [no pls use car size]
	 
	        // create offscreen framebuffer
	        framebuffers[i] = new FrameBuffer(512, 512, 1);

	        // setup framebuffer to use texture
	        framebuffers[i].setDepthBuffer(Format.Depth);
	        framebuffers[i].setMultiTarget(false);
	        switch(i) {
	        	case 0:
	        		framebuffers[i].addColorTexture(offTex, TextureCubeMap.Face.NegativeX);
	        		break;
	        	case 1:
	        		framebuffers[i].addColorTexture(offTex, TextureCubeMap.Face.PositiveX);
	            	break;
	        	case 2:
	        		framebuffers[i].addColorTexture(offTex, TextureCubeMap.Face.NegativeY);
	            	break;
	        	case 3:
	        		framebuffers[i].addColorTexture(offTex, TextureCubeMap.Face.PositiveY);
	        		break;
	        	case 4:
	        		framebuffers[i].addColorTexture(offTex, TextureCubeMap.Face.NegativeZ);
	        		break;
	        	case 5:
	        		framebuffers[i].addColorTexture(offTex, TextureCubeMap.Face.PositiveZ);
	        		break;
	        	default:
					try {
						throw new Exception("Car reflection map doesn't go past 6 cameras");
					} catch (Exception e) {
						e.printStackTrace();
					}
	        }
	        
	        framebuffers[i].addColorTexture(offTex, TextureCubeMap.Face.NegativeY);
	        
	        //set viewport to render to offscreen framebuffer
	        offViews[i].setOutputFrameBuffer(framebuffers[i]);
		}
	}
	
	@Override
	public void update(float tpf) {
		super.update(tpf);
		
		if (!isEnabled())
			return;
		
		for (int i = 0; i < CAMERA_COUNT; i++) {
			cams[i].setLocation(car.getPhysicsLocation());
			cams[i].lookAtDirection(car.getPhysicsRotation().mult(this.dir[i]), up[i]);
		}
	}
	
	@Override
	public void cleanup() {
		super.cleanup();
		
		for (int i = 0; i < CAMERA_COUNT; i++)
			App.rally.getRenderManager().removePreView(VIEW_PORT_NAME+i);
	}
}
