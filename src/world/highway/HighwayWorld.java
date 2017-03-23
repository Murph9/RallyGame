package world.highway;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.terrain.noise.ShaderUtils;
import com.jme3.terrain.noise.basis.FilteredBasis;
import com.jme3.terrain.noise.filter.*;
import com.jme3.terrain.noise.fractal.FractalSum;
import com.jme3.terrain.noise.modulator.NoiseModulator;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;

import game.App;
import terrainWorld.NoiseBasedWorld;
import terrainWorld.Terrain;
import world.World;
import world.WorldType;

/*
Basic idea is:
Make road our of random beizer curves
- these beizer curves to have road width (use the code from the 3d course)
  + WIDTH of basic road, similar to TDU: 
  		{45'(edge) 2.5(shoulder) | 4.5 (lane) || 4.5 | 2.5 45'}
- eventually overlay it on terrain
 + flatten the terrain under the road so it 'fits'
- add grass and trees all TDU like
*/

public class HighwayWorld extends World {
	private Terrain terrain;
	private int terrainSize;

	public HighwayWorld() {
		super("curveWorldRoot");
	}

	@Override
	public WorldType getType() {
		return WorldType.HIGHWAY;
	}

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		terrainSize = 128 + 1;

		createWorldWithNoise(app.getAssetManager());
	}

	private Material createTerrainMaterial(AssetManager am) {
		Material terrainMaterial = new Material(am, "Common/MatDefs/Terrain/HeightBasedTerrain.j3md");

		float grassScale = 16;
		float dirtScale = 16;
		float rockScale = 16;

		// GRASS texture
		Texture grass = am.loadTexture("assets/terrain/grass.jpg");
		grass.setWrap(WrapMode.Repeat);
		terrainMaterial.setTexture("region1ColorMap", grass);
		terrainMaterial.setVector3("region1", new Vector3f(58, 200, grassScale));

		// DIRT texture
		Texture dirt = am.loadTexture("assets/terrain/dirt.jpg");
		dirt.setWrap(WrapMode.Repeat);
		terrainMaterial.setTexture("region2ColorMap", dirt);
		terrainMaterial.setVector3("region2", new Vector3f(0, 60, dirtScale));

		// ROCK textures
		Texture rock = am.loadTexture("assets/terrain/Rock.PNG");
		rock.setWrap(WrapMode.Repeat);
		terrainMaterial.setTexture("region3ColorMap", rock);
		terrainMaterial.setVector3("region3", new Vector3f(198, 260, rockScale));

		terrainMaterial.setTexture("region4ColorMap", rock);
		terrainMaterial.setVector3("region4", new Vector3f(198, 260, rockScale));

		Texture rock2 = am.loadTexture("assets/terrain/rock.jpg");
		rock2.setWrap(WrapMode.Repeat);

		terrainMaterial.setTexture("slopeColorMap", rock2);
		terrainMaterial.setFloat("slopeTileFactor", 32);

		terrainMaterial.setFloat("terrainSize", terrainSize);

		return terrainMaterial;
	}

	private void createWorldWithNoise(AssetManager am) {
		// TODO change settings
		// TODO remember the ./world folder with the cached terrain pieces
		NoiseBasedWorld newWorld = new NoiseBasedWorld(App.rally, App.rally.getPhysicsSpace(), terrainSize, terrainSize);

		newWorld.setWorldHeight(192f);
		newWorld.setViewDistance(2);
		newWorld.setCacheTime(5000);

		Material terrainMaterial = createTerrainMaterial(am);
		newWorld.setMaterial(terrainMaterial);

		// create a noise generator
		FractalSum base = new FractalSum();

		base.setRoughness(0.7f);
		base.setFrequency(1.0f);
		base.setAmplitude(0.5f);
		base.setLacunarity(3.12f);
		base.setOctaves(8);
		base.setScale(0.02125f);
		// base.setScale(1f);
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
		therm.setRadius(5);
		therm.setTalus(0.011f);

		SmoothFilter smooth = new SmoothFilter();
		smooth.setRadius(1);
		smooth.setEffect(0.7f);

		IterativeFilter iterate = new IterativeFilter();
		iterate.addPreFilter(perturb);
		iterate.addPostFilter(smooth);
		iterate.setFilter(therm);
		iterate.setIterations(1);

		ground.addPreFilter(iterate);

		newWorld.setFilteredBasis(ground);

		this.terrain = newWorld;
		App.rally.getStateManager().attach(this.terrain);
	}

	// interface nodes
	@Override
	public Vector3f getStartPos() { return new Vector3f(0, 100, 0); }
	@Override
	public void update(float tpf) { }
	@Override
	public void reset() {  }

	@Override
	public void cleanup() {
		rootNode.detachAllChildren();
		terrain.close();
		App.rally.getStateManager().detach(terrain);
		/*
		java.util.concurrent.RejectedExecutionException: Task java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask@33944466 rejected from java.util.concurrent.ScheduledThreadPoolExecutor@a2243b4[Terminated, pool size = 0, active threads = 0, queued tasks = 0, completed tasks = 1021]
			at java.util.concurrent.ThreadPoolExecutor$AbortPolicy.rejectedExecution(Unknown Source)
			at java.util.concurrent.ThreadPoolExecutor.reject(Unknown Source)
			at java.util.concurrent.ScheduledThreadPoolExecutor.delayedExecute(Unknown Source)
			at java.util.concurrent.ScheduledThreadPoolExecutor.schedule(Unknown Source)
		*/
		
		super.cleanup();
	}
}
