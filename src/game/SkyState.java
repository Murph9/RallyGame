package game;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.post.ssao.SSAOFilter;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Quad;
import com.jme3.shadow.DirectionalLightShadowFilter;
import com.jme3.shadow.DirectionalLightShadowRenderer;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;
import com.jme3.util.SkyFactory;

public class SkyState extends AbstractAppState {

	private static final ColorRGBA DAY_TOP = new ColorRGBA(0,0,1,1);
	private static final ColorRGBA DAY_SIDE = new ColorRGBA(0.3f,0.6f,1,1);
	private static final ColorRGBA DAY_BOTTOM = new ColorRGBA(0.3f,0.6f,1,1);

	private static final ColorRGBA NIGHT_TOP = new ColorRGBA(0,0,0.3f,1);
	private static final ColorRGBA NIGHT_SIDE = new ColorRGBA(0.15f,0.3f,0.5f,1);
	private static final ColorRGBA NIGHT_BOTTOM = new ColorRGBA(0.15f,0.3f,0.5f,1);
	
	private Geometry sun;
	private Geometry moon;
	private Node sunNode;
	private float sunRot;
	private float rotateSpeed = 0.01f; //0.01 seem ok for a game (maybe slower)
	
	public boolean isDay = true;
	
	//skymap
	Geometry skymap;
	int sideCount = 4;
	boolean ifCubeMap = false;	
	
	private DirectionalLight sunL;
	private DirectionalLight moonL;
	private AmbientLight ambL;

	//shadow stuff
	public final boolean ifShadow = true;
	public final boolean ifFancyShadow = false;
	private DirectionalLightShadowRenderer dlsr;
	private DirectionalLightShadowFilter dlsf;

	//glowing stuff
	private final boolean ifGlow = false; //TODO does this even work?
//	BloomFilter bloom;

	
	//TODO moon and moon light (basically more ambient light)
	public SkyState() { }

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		Rally r = App.rally;
		AssetManager am = r.getAssetManager();
		ViewPort vp = r.getViewPort();
		Node skyRoot = r.getRootNode(); //can't have a sub root because the lights only effect things in their tree
		r.getRootNode().attachChild(skyRoot);
		
		//lights
		ambL = new AmbientLight(); //TODO some blender objects don't take too fondly to ambient light
		ambL.setColor(new ColorRGBA(ColorRGBA.White).mult(0.2f).add(new ColorRGBA(ColorRGBA.Blue).mult(0.1f)));
		skyRoot.addLight(ambL);

		sunL = new DirectionalLight();
		sunL.setColor(new ColorRGBA(0.88f, 0.88f, 0.97f, 1f)); //maybe too bright?
		
		sunL.setDirection(new Vector3f(0f, 0.4f, 0f).normalizeLocal());
		skyRoot.addLight(sunL);

		moonL = new DirectionalLight();
		moonL.setColor(new ColorRGBA(0.4f, 0.4f, 0.4f, 1f));
		moonL.setDirection(new Vector3f(0f, -1f, 0f).normalizeLocal());
		skyRoot.addLight(moonL);
		H.p("init sun");
		
		//TODO moon object and light
		//sun 'object'
		sunRot = FastMath.QUARTER_PI; //start mid morning
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
		//end sun
		//moon
		BillboardControl moonBC = new BillboardControl();
		moon = new Geometry("Moon", new Quad(10, 10));
		mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", new ColorRGBA(ColorRGBA.White).mult(0.9f));
		moon.setMaterial(mat);
		moon.center();
		moon.move(0, 0, -200);
		moon.addControl(moonBC);
		sunNode.attachChild(moon);
		//end moon
		
		skyRoot.attachChild(sunNode);
		
		
		vp.setBackgroundColor(ColorRGBA.Blue); //incase the skymap screws up

		if (ifShadow) {
			//Shadows and lights
			dlsr = new DirectionalLightShadowRenderer(am, 2048, 4);
			dlsr.setLight(sunL);
			dlsr.setLambda(0.55f);
			dlsr.setShadowIntensity(0.6f);
			dlsr.setEdgeFilteringMode(EdgeFilteringMode.Bilinear);
			vp.addProcessor(dlsr);

			dlsf = new DirectionalLightShadowFilter(am, 2048, 4);
			dlsf.setLight(sunL);
			dlsf.setLambda(0.55f);
			dlsf.setShadowIntensity(0.6f);
			dlsf.setEdgeFilteringMode(EdgeFilteringMode.Bilinear);
			dlsf.setEnabled(false);

			FilterPostProcessor fpp = new FilterPostProcessor(am);
			fpp.addFilter(dlsf);
			fpp.setNumSamples(1);

			if (ifFancyShadow) {
				SSAOFilter ssaoFilter = new SSAOFilter(12f, 20f, 0.33f, 0.61f);
				fpp.addFilter(ssaoFilter);
			}

			if (ifGlow) {
				BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
				fpp.addFilter(bloom);
			}

			vp.addProcessor(fpp);
		}
		
		if (!ifCubeMap) {
			generateCubeMap(am, skyRoot);
			
		} else {
			//sky cube map
			boolean theirs = true; //TODO my try (note, the images need to rotate around one, so that the sky and floor match
			if (theirs) {
				Texture t = am.loadTexture("Textures/Sky/Bright/BrightSky.dds");
				skyRoot.attachChild(SkyFactory.createSky(am, t, Vector3f.UNIT_XYZ, SkyFactory.EnvMapType.CubeMap));
			} else {
				TextureKey keye = new TextureKey("assets/east.png", true);
				TextureKey keyw = new TextureKey("assets/west.png", true);
				TextureKey keyn = new TextureKey("assets/north.png", true);
				TextureKey keys = new TextureKey("assets/south.png", true);
				TextureKey keyt = new TextureKey("assets/top.png", true);
				TextureKey keyb = new TextureKey("assets/bottom.png", true);
				skyRoot.attachChild(SkyFactory.createSky(am, am.loadTexture(keyw), am.loadTexture(keye), 
						am.loadTexture(keyn), am.loadTexture(keys), am.loadTexture(keyt), am.loadTexture(keyb)));
			}
		}
	}
	
	private void generateCubeMap(AssetManager am, Node skyRoot) {
		//custom sky map
		Mesh skyM = new Mesh();
		
		Vector3f [] vs = new Vector3f[2 * sideCount * 3]; //order is inside vertex then outside, 2 because top and bottom
		Vector2f[] texCoord = new Vector2f[2 * sideCount * 3];
		float[] colours = new float[2 * sideCount * 3 * 4];
		int[] indexes = new int[2 * sideCount*3];
		
		//top half
		for (int i = 0; i < sideCount*3;) {
			float angle = FastMath.TWO_PI*i/((float)sideCount*3);
			float incAngle = FastMath.TWO_PI*3/((float)sideCount*3);
			
			//center vertex
			vs[i] = new Vector3f(0, 50, 0);
			texCoord[i] = new Vector2f(0,0);
			indexes[i] = i+2; //[=3rd vertex] im so sorry about the order

			i++;
			
			//side 1
			vs[i] = new Vector3f(FastMath.cos(angle)*50, 0, FastMath.sin(angle)*50);
			texCoord[i] = new Vector2f(1,0);
			indexes[i] = i-1; //[=1st vertex]
			
			i++;
			
			//side 2
			vs[i] = new Vector3f(FastMath.cos(angle+incAngle)*50, 0, FastMath.sin(angle+incAngle)*50);
			texCoord[i] = new Vector2f(0,1);
			indexes[i] = i-1; //[=2nd vertex]
			
			i++;
		}
		
		//bottom half
		for (int i = sideCount*3; i < 2*sideCount*3;) {
			float angle = FastMath.TWO_PI*i/((float)sideCount*3);
			float incAngle = FastMath.TWO_PI*3/((float)sideCount*3);
			
			//center vertex
			vs[i] = new Vector3f(0, -50, 0); 
			texCoord[i] = new Vector2f(0,0);
			indexes[i] = i+2; //[=3rd vertex] im so sorry about the order

			colours[i*4 + 0] = 0f;
			colours[i*4 + 1] = 0f;
			colours[i*4 + 2] = 1f;
			colours[i*4 + 3] = 1f;

			i++;
			
			//side 1
			vs[i] = new Vector3f(FastMath.cos(angle)*50, 0, FastMath.sin(angle)*50);
			texCoord[i] = new Vector2f(1,0);
			indexes[i] = i; //[=2nd vertex]
			
			colours[i*4 + 0] = 0.3f;
			colours[i*4 + 1] = 0.6f;
			colours[i*4 + 2] = 1.0f;
			colours[i*4 + 3] = 1f;
			
			i++;
			
			//side 2
			vs[i] = new Vector3f(FastMath.cos(angle+incAngle)*50, 0, FastMath.sin(angle+incAngle)*50);
			texCoord[i] = new Vector2f(0,1);
			indexes[i] = i-2; //[=1st vertex]
			
			colours[i*4 + 0] = 0.3f;
			colours[i*4 + 1] = 0.6f;
			colours[i*4 + 2] = 1.0f;
			colours[i*4 + 3] = 1f;
			
			i++;
		}
		
		skyM.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vs));
		skyM.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoord));
		skyM.setBuffer(Type.Index,    3, BufferUtils.createIntBuffer(indexes));
		skyM.setBuffer(Type.Color, 4, colours);

		skyM.updateBound();

		Material skyMat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		skyMat.setBoolean("VertexColor", true);
		skyMat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		
		skymap = new Geometry("sky", skyM);
		skymap.setQueueBucket(Bucket.Sky);
		skymap.setMaterial(skyMat);
		skyRoot.attachChild(skymap);
	}
	
	private void generateCubeMapColours() {
		float[] colours = new float[2 * sideCount * 3 * 4];
		
		ColorRGBA top, side, bottom;
		
		if (isDay) {
			top = DAY_TOP;
			side = DAY_SIDE;
			bottom = DAY_BOTTOM;
		} else {
			top = NIGHT_TOP;
			side = NIGHT_SIDE;
			bottom = NIGHT_BOTTOM;
		}
		
		
		int i = 0;
		//top
		while (i < sideCount*3) {
			colours[i*4 + 0] = top.r;
			colours[i*4 + 1] = top.g;
			colours[i*4 + 2] = top.b;
			colours[i*4 + 3] = top.a;

			i++;
			
			colours[i*4 + 0] = side.r;
			colours[i*4 + 1] = side.g;
			colours[i*4 + 2] = side.b;
			colours[i*4 + 3] = side.a;

			i++;
			
			colours[i*4 + 0] = side.r;
			colours[i*4 + 1] = side.g;
			colours[i*4 + 2] = side.b;
			colours[i*4 + 3] = side.a;

			i++;
		}
		//bottom
		while (i < 2*sideCount*3) {
			colours[i*4 + 0] = bottom.r;
			colours[i*4 + 1] = bottom.g;
			colours[i*4 + 2] = bottom.b;
			colours[i*4 + 3] = bottom.a;

			i++;
			
			colours[i*4 + 0] = side.r;
			colours[i*4 + 1] = side.g;
			colours[i*4 + 2] = side.b;
			colours[i*4 + 3] = side.a;

			i++;
			
			colours[i*4 + 0] = side.r;
			colours[i*4 + 1] = side.g;
			colours[i*4 + 2] = side.b;
			colours[i*4 + 3] = side.a;

			i++;
		}
		
		skymap.getMesh().getBuffer(VertexBuffer.Type.Color).updateData(BufferUtils.createFloatBuffer(colours));
	}
	
	public void update(float tpf) {
		super.update(tpf);
		Vector3f camPos = App.rally.getCamera().getLocation();
		sunNode.setLocalTranslation(camPos);
		
		sunRot += (tpf*rotateSpeed % FastMath.TWO_PI); //lock to one rotation
		sunNode.setLocalRotation(new Quaternion().fromAngles(-sunRot, 0, 0));

		Vector3f sunPos = sun.getWorldTranslation();
		sunL.setDirection(camPos.subtract(sunPos).normalize());
		moonL.setDirection(sunPos.subtract(camPos).normalize());
		
		float dayish = (sunPos.y-camPos.y)/(200*2) + 0.5f; //TODO use an actual field
		updateDynSky(camPos, dayish);
		
		if ((sunPos.y-camPos.y) > 0) { //the sun is generally up more when its day time
			if (isDay == false) { //changed
				App.rally.getRootNode().removeLight(moonL);
				dlsf.setLight(sunL);
				dlsr.setLight(sunL);
				App.rally.getRootNode().addLight(sunL);
			}
			isDay = true;
		} else {
			if (isDay == true) { //changed
				App.rally.getRootNode().removeLight(sunL);
				dlsf.setLight(moonL);
				dlsr.setLight(moonL);
				App.rally.getRootNode().addLight(moonL);
			}
			isDay = false;
		}
	}
	
	private void updateDynSky(Vector3f camPos, float dayTime) { //TODO find what dayTime should be
		skymap.setLocalTranslation(camPos);
		
		generateCubeMapColours();
	}
}