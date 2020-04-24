package rallygame.world.highway;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.terrain.noise.ShaderUtils;
import com.jme3.terrain.noise.basis.FilteredBasis;
import com.jme3.terrain.noise.filter.*;
import com.jme3.terrain.noise.fractal.FractalSum;
import com.jme3.terrain.noise.modulator.NoiseModulator;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

import rallygame.game.App;
import rallygame.game.DebugAppState;
import rallygame.helper.H;
import rallygame.helper.Log;
import rallygame.helper.Trig;
import rallygame.terrainWorld.NoiseBasedWorld;
import rallygame.world.WorldType;
import rallygame.world.World;

/*
Basic idea is:
Make road our of random beizer curves
- these beizer curves to have road width (use the code from the 3d course)
  + WIDTH of basic road, similar to TDU: 
  		{45'(edge) 2.5(shoulder) | 4.5 (lane) || 4.5 | 2.5 45'}
- add grass, trees and rocks all TDU like

Later Things:
- intesections
- other kinds of road
 */

//uses:
//https://github.com/JulienGreen/RoadTessalation
//https://hub.jmonkeyengine.org/t/vertexbuffer-for-specific-uv-behavior-shaders/38268/22


//Large TODO:
//Set the height in the filteredBasis

public class HighwayWorld extends World {
	public NoiseBasedWorld terrain;
	private int blockSize; //(distance between points)/tileSize
	private int tileSize; //the grid piece size
	public int getTileSize() { return tileSize; }
	
	private List<RoadMesh> roads;
	private RoadMaker rM;
	private TreeMaker tM;
	
	public HighwayWorld() {
		super("highwayWorldRoot");

		//uses the TerrainListener class to get the road bits
		roads = new LinkedList<RoadMesh>();
	}

	@Override
	public WorldType getType() {
		return WorldType.HIGHWAY;
	}

	@Override
	public void initialize(Application app) {
		super.initialize(app);
		
		blockSize = 128 + 1;
		tileSize = 128 + 1; //testing occured with these on 128 ish

		AssetManager am = app.getAssetManager();
		
		createWorldWithNoise((App)app, am);
	}

	
	private void createWorldWithNoise(App app, AssetManager am) {
		// TODO change settings
		// TODO remember the ./world folder with the cached terrain pieces
		NoiseBasedWorld newWorld = new NoiseBasedWorld(app, getState(BulletAppState.class).getPhysicsSpace(), tileSize, blockSize, rootNode);

		newWorld.setWorldHeight(500); //TODO change to set the height range (needs to be scaled with the texture heights)
		newWorld.setViewDistance(2);
		newWorld.setCacheTime(5000);

		Material terrainMaterial = createTerrainMaterial(am);
		newWorld.setMaterial(terrainMaterial);

		//larger height variance filter
		FractalSum base = new FractalSum();
		base.setFrequency(0.003f); //mountians aren't small
		base.setAmplitude(1.5f); //they are big
		base.addModulator(new NoiseModulator() {
			@Override
			public float value(float... in) {
				return ShaderUtils.clamp(in[0] * 0.5f + 0.5f, 0, 1);
			}
		});
		FilteredBasis ground = new FilteredBasis(base);
		PerturbFilter perturb = new PerturbFilter();
		perturb.setMagnitude(0.119f);

		OptimizedErode therm = new OptimizedErode();
		therm.setRadius(10);
		therm.setTalus(0.011f);

		SmoothFilter smooth = new SmoothFilter();
		smooth.setRadius(1);
		smooth.setEffect(0.7f);

		IterativeFilter iterate = new IterativeFilter();
		iterate.addPreFilter(perturb);
		iterate.addPostFilter(smooth);
		iterate.setFilter(therm);
		iterate.setIterations(5); //higher numbers make it really smooth

		ground.addPreFilter(iterate);
		
		newWorld.setFilteredBasis(ground);
		//TODO for height generation, suggest creating my own FilteredBasis
		//which can have points individually set so that i don't have to look at the terrain heights

		this.terrain = newWorld;
		getStateManager().attach(this.terrain);
		
		//set after so the terrain exists first
		rM = new RoadMaker((App)app, this);
		newWorld.addTileListener(rM);
		
		tM = new TreeMaker((App)app, this);
		newWorld.addTileListener(tM);
	}
	private Material createTerrainMaterial(AssetManager am) {
        Material terrainMaterial = new Material(am, "Common/MatDefs/Terrain/HeightBasedTerrain.j3md");

		float grassScale = 16;
		float dirtScale = 16;
		float rockScale = 16;
		
        // GRASS texture
        Texture grass = am.loadTexture("terrain/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        terrainMaterial.setTexture("region1ColorMap", grass);
        terrainMaterial.setVector3("region1", new Vector3f(58*2, 200*2, grassScale));

        // DIRT texture
        Texture dirt = am.loadTexture("terrain/dirt.jpg");
        dirt.setWrap(WrapMode.Repeat);
        terrainMaterial.setTexture("region2ColorMap", dirt);
        terrainMaterial.setVector3("region2", new Vector3f(0, 60*2, dirtScale));

        // ROCK textures
        Texture rock = am.loadTexture("terrain/Rock.PNG");
        rock.setWrap(WrapMode.Repeat);
        terrainMaterial.setTexture("region3ColorMap", rock);
        terrainMaterial.setVector3("region3", new Vector3f(198*2, 260*2, rockScale));

        terrainMaterial.setTexture("region4ColorMap", rock);
        terrainMaterial.setVector3("region4", new Vector3f(198*2, 260*2, rockScale));

        Texture rock2 = am.loadTexture("terrain/rock.jpg");
        rock2.setWrap(WrapMode.Repeat);

        terrainMaterial.setTexture("slopeColorMap", rock2);
        terrainMaterial.setFloat("slopeTileFactor", 32);

        terrainMaterial.setFloat("terrainSize", blockSize);

        return terrainMaterial;

	}
	
	protected void generateRoad(RoadMesh road) {
		//remember that the chunk hasn't been loaded into the world by here yet
		if (terrain == null) {
			Log.e("STOP touching the terrain thing that you don't understand.");
			return;//????, only happens when someone plays around with the terrain init order and breaks something
		}
		
		if (road == null) {
			Log.p("Road null");
			return;
		}
		
		
		List<Vector3f> list = road.getControlPoints(); //No editing this object
		Log.e("road: ", list);

		DebugAppState debug = getState(DebugAppState.class);
		for (int i = 0; i < list.size(); i++) {
			debug.drawBox("HighwayWorld" + road + i, ColorRGBA.Green, list.get(i), 0.2f);
		}

		roads.add(road);
		
		Geometry geo = new Geometry("highway", road);
		Material mat = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		mat.setColor("Color", new ColorRGBA(0,0,0,0.5f));
		mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
		geo.setShadowMode(ShadowMode.Off);
		geo.setQueueBucket(Bucket.Transparent);
		geo.setMaterial(mat);
		rootNode.attachChild(geo);
		
		List<Vector3f[]> quads = road.getQuads();
		setHeightsFor(quads, (quad) -> {
			return new Vector3f[] { quad[0], quad[1], quad[3], quad[2] };
		});
	}
	
	private void setHeightsFor(List<Vector3f[]> quads, Function<Vector3f[], Vector3f[]> order) {
		List<Vector3f> heightList = new LinkedList<Vector3f>();
		
		for (Vector3f[] quad: quads) {
			DebugAppState debug = getState(DebugAppState.class);
			debug.drawBox("HighwayWorld" + quad + "0", ColorRGBA.Green, quad[0].add(0, 0.1f, 0), 0.2f);
			debug.drawBox("HighwayWorld" + quad + "1", ColorRGBA.White, quad[1].add(0, 0.1f, 0), 0.2f);
			debug.drawBox("HighwayWorld" + quad + "2", ColorRGBA.Blue, quad[2].add(0, -0.1f, 0), 0.2f);
			debug.drawBox("HighwayWorld" + quad + "3", ColorRGBA.Red, quad[3].add(0, -0.1f, 0), 0.2f);
			

			quad = order.apply(quad);
			
			float[] box = Trig.boundingBoxXZ(quad);
			box[0] -= 1; //extend the extends so they cover it completely
			box[1] -= 1;
			box[2] += 1;
			box[3] += 1;
			
			for (int i = (int)box[0]; i < box[2]; i++) {
				for (int j = (int)box[1]; j < box[3]; j++) {
					Vector3f pos = new Vector3f(i, 0, j);
					//use the jme3 library method for point in triangle
					if (FastMath.pointInsideTriangle(H.v3tov2fXZ(quad[0]), H.v3tov2fXZ(quad[2]), H.v3tov2fXZ(quad[3]), H.v3tov2fXZ(pos)) != 0) {
						pos.y = Trig.heightInTri(quad[0], quad[2], quad[3], pos) - 0.01f;
						heightList.add(pos);
					} else if (FastMath.pointInsideTriangle(H.v3tov2fXZ(quad[0]), H.v3tov2fXZ(quad[2]), H.v3tov2fXZ(quad[1]), H.v3tov2fXZ(pos)) != 0) {
						pos.y = Trig.heightInTri(quad[0], quad[2], quad[1], pos) - 0.01f;
						heightList.add(pos);
					}
				}
			}
		}
		
		terrain.setHeights(heightList);
	}
	
	// interface nodes
	@Override
	public Transform getStart() {
		return new Transform(new Vector3f(10, 260, 0));
	}
	@Override
	public void update(float tpf) { 
		//only way i have found to remove all the log messages it spews
		Logger.getLogger(PerturbFilter.class.getCanonicalName()).setLevel(Level.SEVERE);
	}
	@Override
	public void reset() { }

	@Override
	public void cleanup(Application app) {
		this.rootNode.detachAllChildren();
		this.terrain.close();
		this.tM.cleanup();
		getStateManager().detach(this.terrain);

		super.cleanup(app);
	}
}
