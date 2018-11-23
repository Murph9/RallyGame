package test;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.texture.FrameBuffer;
import com.jme3.texture.Image.Format;
import com.jme3.texture.Texture;
import com.jme3.texture.TextureCubeMap;
import com.jme3.util.SkyFactory;
import com.jme3.util.SkyFactory.EnvMapType;

/**
 * Renders a rotating box to a cubemap texture, then applies the cubemap
 * texture as a sky.
 */
public class TestRenderToCubemap  extends SimpleApplication {
 
    private Geometry offBox;
    private float angle = 0;
    private ViewPort offView;
 
    public static void main(String[] args){
        TestRenderToCubemap app = new TestRenderToCubemap();
        AppSettings settings = new AppSettings(true);
        settings.setVSync(true);
        app.setSettings(settings);
        app.start();
    }
 
    public Texture setupOffscreenView(){
        Camera offCamera = new Camera(512, 512);
 
        offView = renderManager.createPreView("Offscreen View", offCamera);
        offView.setClearFlags(true, true, true);
        offView.setBackgroundColor(ColorRGBA.DarkGray);
 
        // create offscreen framebuffer
        FrameBuffer offBuffer = new FrameBuffer(512, 512, 1);
 
        //setup framebuffer's cam
        offCamera.setFrustumPerspective(45f, 1f, 0.1f, 1000f);
        offCamera.setLocation(new Vector3f(0f, 0f, 0f));
        offCamera.lookAt(new Vector3f(-1f, 0f, 0f), Vector3f.UNIT_Y);
 
        //setup framebuffer's texture
        TextureCubeMap offTex = new TextureCubeMap(512, 512, Format.RGBA8);
        offTex.setMinFilter(Texture.MinFilter.Trilinear);
        offTex.setMagFilter(Texture.MagFilter.Bilinear);
 
        //setup framebuffer to use texture
        offBuffer.setDepthBuffer(Format.Depth);
        offBuffer.setMultiTarget(true);
        offBuffer.addColorTexture(offTex, TextureCubeMap.Face.NegativeX);
        offBuffer.addColorTexture(offTex, TextureCubeMap.Face.PositiveX);
        offBuffer.addColorTexture(offTex, TextureCubeMap.Face.NegativeY);
        offBuffer.addColorTexture(offTex, TextureCubeMap.Face.PositiveY);
        offBuffer.addColorTexture(offTex, TextureCubeMap.Face.NegativeZ);
        offBuffer.addColorTexture(offTex, TextureCubeMap.Face.PositiveZ);
        
        //set viewport to render to offscreen framebuffer
        offView.setOutputFrameBuffer(offBuffer);
 
        // setup framebuffer's scene
        Box boxMesh = new Box(0.1f,0.1f,0.1f);
        Material material = new Material(getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        offBox = new Geometry("box", boxMesh);
        offBox.setMaterial(material);
 
        // attach the scene to the viewport to be rendered
        offView.attachScene(offBox);
 
        return offTex;
    }
 
    @Override
    public void simpleInitApp() {
    	cam.setLocation(new Vector3f());
        cam.lookAt(Vector3f.UNIT_X, Vector3f.UNIT_Y);
 
        Texture offTex = setupOffscreenView();
        Spatial sky = SkyFactory.createSky(assetManager, offTex, EnvMapType.CubeMap);
        rootNode.attachChild(sky);
    }
 
    
    private int frameCount = 0;
    @Override
    public void simpleUpdate(float tpf){
    	frameCount++;
    	
        Quaternion q = new Quaternion();
 
        angle += tpf;
        angle %= FastMath.TWO_PI;
        q.fromAngles(angle, 0, angle);

        
        offBox.setLocalTranslation(new Quaternion().fromAngleAxis(FastMath.PI*(((float)frameCount)/120), Vector3f.UNIT_Y).mult(new Vector3f(1,0,0)));
        
        offBox.setLocalRotation(q);
        offBox.updateLogicalState(tpf);
        offBox.updateGeometricState();
        
    }
}