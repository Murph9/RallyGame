package game;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.post.filters.BloomFilter;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;

import jme3utilities.Misc;
import jme3utilities.sky.SkyControl;

public class SkyState extends AbstractAppState {

	//Uses SkyControl from https://github.com/stephengold/jme3-utilities

	public static final boolean IF_SHADOW = true;
	public static final boolean IF_BLOOM = true;
	
	private DirectionalLight mainLight;
	private AmbientLight ambLight;
	private DirectionalLightShadowRenderer dlsr;
	
	public SkyState() { }

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		Main r = App.rally;
		AssetManager am = r.getAssetManager();
		ViewPort vp = r.getViewPort();
		Node skyRoot = r.getRootNode(); //can't have a sub root because the lights only effect things in their sub-tree

		mainLight = new DirectionalLight();
        mainLight.setName("main"); //color, direction not set because skyfactory does itself
        skyRoot.addLight(mainLight);

        ambLight = new AmbientLight();
        ambLight.setName("ambient");
        skyRoot.addLight(ambLight);
		
		SkyControl sky = new SkyControl(am, r.getCamera(), 0.7f, false, true);
		skyRoot.addControl(sky);
		sky.setCloudiness(0.4f);
		sky.getSunAndStars().setHour(12f);
		sky.getSunAndStars().setObserverLatitude(0f); //equator
        sky.getUpdater().setAmbientLight(ambLight);
        sky.getUpdater().setMainLight(mainLight);
		
        if (IF_SHADOW) {
			//Shadows and lights
			dlsr = new DirectionalLightShadowRenderer(am, 2048, 4);
			dlsr.setLight(mainLight);
			dlsr.setLambda(0.55f);
			dlsr.setShadowIntensity(0.6f);
			dlsr.setEdgeFilteringMode(EdgeFilteringMode.Bilinear);
			vp.addProcessor(dlsr);
		}
        
        if (IF_BLOOM) {
	        BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
	        bloom.setBlurScale(2.5f);
	        bloom.setExposurePower(1f);
	        Misc.getFpp(vp, am).addFilter(bloom);
	        sky.getUpdater().addBloomFilter(bloom);
        }
        
		sky.setEnabled(true);
	}
	
	public void update(float tpf) {
		super.update(tpf);
		
	}
}