package game;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Quad;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.util.SkyFactory;

public class SkyState extends AbstractAppState {

	private Geometry sun;
	private Node sunNode;
	public boolean isDay = false;
	private float rotateSpeed = 0.05f;
	
	private DirectionalLight sunL;
	private AmbientLight ambL;

	//shadow stuff
	public final boolean ifShadow = true;
	public final boolean ifFancyShadow = false;
	private DirectionalLightShadowRenderer dlsr;
	private DirectionalLightShadowFilter dlsf;

	//glowing stuff
	private final boolean ifGlow = false; //TODO does this even work?
//	FilterPostProcessor fpp;
//	BloomFilter bloom;

	public SkyState() { }

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		Rally r = App.rally;
		AssetManager am = r.getAssetManager();
		Node root = r.getRootNode();
		
		//lights
		ambL = new AmbientLight(); //TODO isn't applied to world objects
		ambL.setColor(ColorRGBA.White.mult(0.1f).add(ColorRGBA.Blue.mult(0.1f)));
		root.addLight(ambL);

		sunL = new DirectionalLight();
		sunL.setColor(new ColorRGBA(0.9f, 0.9f, 1f, 1f));
		sunL.setDirection(new Vector3f(-0.3f, -0.6f, -0.5f).normalizeLocal());
		root.addLight(sunL);

		
		//TODO moon object and light
		//sun 'object'
		sunNode = new Node("Sun rotate node");
		
		BillboardControl sunBC = new BillboardControl();
		sun = new Geometry("sun", new Quad(10, 10)); 
		Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", ColorRGBA.Yellow);
		sun.setMaterial(mat);
		sun.center();
		sun.move(0, 0, 200);
		sun.addControl(sunBC);

		sunNode.attachChild(sun);
		root.attachChild(sunNode);
		//end sun
		
		
		r.getViewPort().setBackgroundColor(ColorRGBA.Blue); //incase the skymap screws up

		if (ifShadow) {
			//Shadows and lights
			dlsr = new DirectionalLightShadowRenderer(am, 2048, 3);
			dlsr.setLight(sunL);
			dlsr.setLambda(0.55f);
			dlsr.setShadowIntensity(0.6f);
			dlsr.setEdgeFilteringMode(EdgeFilteringMode.Nearest);
			App.rally.getViewPort().addProcessor(dlsr);

			dlsf = new DirectionalLightShadowFilter(am, 2048, 3);
			dlsf.setLight(sunL);
			dlsf.setLambda(0.55f);
			dlsf.setShadowIntensity(0.6f);
			dlsf.setEdgeFilteringMode(EdgeFilteringMode.Nearest);
			dlsf.setEnabled(false);

			FilterPostProcessor fpp = new FilterPostProcessor(am);
			fpp.addFilter(dlsf);

			if (ifFancyShadow) {
				fpp = new FilterPostProcessor(am);
				SSAOFilter ssaoFilter = new SSAOFilter(12.94f, 43.92f, 0.33f, 0.61f);
				fpp.addFilter(ssaoFilter);
			}

			if (ifGlow) {
				BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
				fpp.addFilter(bloom);
			}

			App.rally.getViewPort().addProcessor(fpp);
		}
		
		//sky map
		boolean theirs = false; //TODO my try (note, the images need to rotate around one, so that the sky and floor match
		if (theirs) {
			root.attachChild(SkyFactory.createSky(am, "Textures/Sky/Bright/BrightSky.dds", false));	
		} else {
			TextureKey keye = new TextureKey("assets/east.png", true);
			TextureKey keyw = new TextureKey("assets/west.png", true);
			TextureKey keyn = new TextureKey("assets/north.png", true);
			TextureKey keys = new TextureKey("assets/south.png", true);
			TextureKey keyt = new TextureKey("assets/top.png", true);
			TextureKey keyb = new TextureKey("assets/bottom.png", true);
			root.attachChild(SkyFactory.createSky(am, am.loadTexture(keyw), 
					am.loadTexture(keye), am.loadTexture(keyn), 
					am.loadTexture(keys), am.loadTexture(keyt), 
					am.loadTexture(keyb)));
		}

	}
	
	public void update(float tpf) {
		super.update(tpf);
		Vector3f camPos = App.rally.getCamera().getLocation();
		sunNode.setLocalTranslation(camPos);
		sunNode.rotate(-rotateSpeed*tpf, 0, 0);

		Vector3f sunPos = sun.getWorldTranslation();
		sunL.setDirection(camPos.subtract(sunPos).normalizeLocal());
		
		if ((sunPos.y-camPos.y) > 0) //the sun is generally high in the sky 
			isDay = true;
		else 
			isDay = false;
	}
}