package world.highway;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
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

import game.App;
import helper.H;
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
- add grass, trees and rocks all TDU like
*/

//uses:
//https://github.com/JulienGreen/RoadTessalation
//https://hub.jmonkeyengine.org/t/vertexbuffer-for-specific-uv-behavior-shaders/38268/22

public class HighwayWorld extends World {
	public Terrain terrain;
	private int blockSize; //(distance between points)/tileSize
	private int tileSize; //the grid piece size
	public int getTileSize() { return tileSize; }
	
	private List<RoadMesh> roads;
	private TerrainListener tL;
	
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
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		blockSize = 128 + 1;
		tileSize = 128 + 1; //testing occured with these on 128 ish

		AssetManager am = app.getAssetManager();
		
		createWorldWithNoise(am);
	}

	
	private void createWorldWithNoise(AssetManager am) {
		// TODO change settings
		// TODO remember the ./world folder with the cached terrain pieces
		NoiseBasedWorld newWorld = new NoiseBasedWorld(App.rally, App.rally.getPhysicsSpace(), tileSize, blockSize, rootNode);

		newWorld.setWorldHeight(500); //TODO change to set the height range (needs to be scaled with the texture heights)
		newWorld.setViewDistance(2);
		newWorld.setCacheTime(5000);

		Material terrainMaterial = createTerrainMaterial(am);
		newWorld.setMaterial(terrainMaterial);

		/*
		// create a noise generator
		FractalSum base = new FractalSum();
		base.setRoughness(0.7f);
		base.setFrequency(1.0f);
		base.setAmplitude(0.5f);
		base.setLacunarity(3.12f);
		base.setOctaves(8);
		base.setScale(0.02125f);
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
		iterate.setIterations(3); //higher numbers make it really smooth

		ground.addPreFilter(iterate);
		*/

		//larger height variance filter
		FractalSum base2 = new FractalSum();
		base2.setFrequency(0.003f); //mountians aren't small
		base2.setAmplitude(1.5f); //they are big
		base2.addModulator(new NoiseModulator() {
			@Override
			public float value(float... in) {
				return ShaderUtils.clamp(in[0] * 0.5f + 0.5f, 0, 1);
			}
		});
		FilteredBasis ground2 = new FilteredBasis(base2);
		PerturbFilter perturb2 = new PerturbFilter();
		perturb2.setMagnitude(0.119f);

		OptimizedErode therm2 = new OptimizedErode();
		therm2.setRadius(10);
		therm2.setTalus(0.011f);

		SmoothFilter smooth2 = new SmoothFilter();
		smooth2.setRadius(1);
		smooth2.setEffect(0.7f);

		IterativeFilter iterate2 = new IterativeFilter();
		iterate2.addPreFilter(perturb2);
		iterate2.addPostFilter(smooth2);
		iterate2.setFilter(therm2);
		iterate2.setIterations(5); //higher numbers make it really smooth

		ground2.addPreFilter(iterate2);
		
		newWorld.setFilteredBasis(new FilteredBasis[] { /*ground,*/ ground2 });

		this.terrain = newWorld;
		App.rally.getStateManager().attach(this.terrain);
		
		//set after so the terrain exists first
		tL = new TerrainListener(this);
		newWorld.setTileListener(tL);
	}
	private Material createTerrainMaterial(AssetManager am) {
        Material terrainMaterial = new Material(am, "Common/MatDefs/Terrain/HeightBasedTerrain.j3md");

		float grassScale = 16;
		float dirtScale = 16;
		float rockScale = 16;

		//TODO lighting
		
        // GRASS texture
        Texture grass = am.loadTexture("assets/terrain/grass.jpg");
        grass.setWrap(WrapMode.Repeat);
        terrainMaterial.setTexture("region1ColorMap", grass);
        terrainMaterial.setVector3("region1", new Vector3f(58*2, 200*2, grassScale));

        // DIRT texture
        Texture dirt = am.loadTexture("assets/terrain/dirt.jpg");
        dirt.setWrap(WrapMode.Repeat);
        terrainMaterial.setTexture("region2ColorMap", dirt);
        terrainMaterial.setVector3("region2", new Vector3f(0, 60*2, dirtScale));

        // ROCK textures
        Texture rock = am.loadTexture("assets/terrain/Rock.PNG");
        rock.setWrap(WrapMode.Repeat);
        terrainMaterial.setTexture("region3ColorMap", rock);
        terrainMaterial.setVector3("region3", new Vector3f(198*2, 260*2, rockScale));

        terrainMaterial.setTexture("region4ColorMap", rock);
        terrainMaterial.setVector3("region4", new Vector3f(198*2, 260*2, rockScale));

        Texture rock2 = am.loadTexture("assets/terrain/rock.jpg");
        rock2.setWrap(WrapMode.Repeat);

        terrainMaterial.setTexture("slopeColorMap", rock2);
        terrainMaterial.setFloat("slopeTileFactor", 32);

        terrainMaterial.setFloat("terrainSize", blockSize);

        return terrainMaterial;

	}
	
	protected void generateRoad(Vector3f start, Vector3f end) {
		//remember that the chunk hasn't been loaded into the world by here yet
		if (terrain == null) {
			H.e("STOP touching the terrain thing that you don't understand.");
			return;//????, only happens when someone plays around with the terrain init order and breaks something
		}
		
		if (start == null || end == null || H.v3tov2fXZ(start).subtract(H.v3tov2fXZ(end)).length() == 0) {
			H.p("Weird road generated", start, end);
			return; //no weird roads please
		}
		
		H.e("road: ", start, "-->", end);
		
		
		//TODO suggest this goes in the terrainlistener (maybe rename to roadbuilder)
		Vector3f dir = end.subtract(start).normalize();
		float length = end.subtract(start).length();
		List<Vector3f> list = Arrays.asList(new Vector3f[] { start, start.add(dir.mult(length/3)).add(H.randV3f()), end.subtract(dir.mult(length/3)).add(H.randV3f()), end });
		RoadMesh m = new RoadMesh(5, 2, list);
		
		for (int i = 0; i < list.size(); i++)
			App.rally.getRootNode().attachChild(H.makeShapeBox(App.rally.getAssetManager(), ColorRGBA.Green, list.get(i), 0.2f));
		
		roads.add(m);
		
		Geometry geo = new Geometry("curvy", m);
		Material mat = new Material(App.rally.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		mat.setColor("Color", new ColorRGBA(0,0,0,0.5f));
		mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
		geo.setShadowMode(ShadowMode.Off);
		geo.setQueueBucket(Bucket.Transparent);
		geo.setMaterial(mat);
		rootNode.attachChild(geo);
		
		List<Vector3f[]> quads = m.getQuads();
		setHeightsFor(quads, (quad) -> {
			return new Vector3f[] { quad[0], quad[1], quad[3], quad[2] };
		});
	}
	
	private void setHeightsFor(List<Vector3f[]> quads, Function<Vector3f[], Vector3f[]> order) {
		List<Vector3f> heightList = new LinkedList<Vector3f>();
		
		for (Vector3f[] quad: quads) {
			if (App.rally.IF_DEBUG) {
				App.rally.getRootNode().attachChild(H.makeShapeBox(App.rally.getAssetManager(), ColorRGBA.Green, quad[0].add(0,0.1f,0), 0.2f));
				App.rally.getRootNode().attachChild(H.makeShapeBox(App.rally.getAssetManager(), ColorRGBA.White, quad[1].add(0,0.1f,0), 0.2f));
				App.rally.getRootNode().attachChild(H.makeShapeBox(App.rally.getAssetManager(), ColorRGBA.Blue, quad[2].add(0,-0.1f,0), 0.2f));
				App.rally.getRootNode().attachChild(H.makeShapeBox(App.rally.getAssetManager(), ColorRGBA.Red, quad[3].add(0,-0.1f,0), 0.2f));
			}
			
			quad = order.apply(quad);
			
			float[] box = H.boundingBoxXZ(quad);
			box[0] -= 1; //extend the extends so they cover it completely
			box[1] -= 1;
			box[2] += 1;
			box[3] += 1;
			
			for (int i = (int)box[0]; i < box[2]; i++) {
				for (int j = (int)box[1]; j < box[3]; j++) {
					Vector3f pos = new Vector3f(i, 0, j);
					//use the jme3 library method for point in triangle
					if (FastMath.pointInsideTriangle(H.v3tov2fXZ(quad[0]), H.v3tov2fXZ(quad[2]), H.v3tov2fXZ(quad[3]), H.v3tov2fXZ(pos)) != 0) {
						pos.y = H.heightInTri(quad[0], quad[2], quad[3], pos) - 0.01f;
						heightList.add(pos);
					} else if (FastMath.pointInsideTriangle(H.v3tov2fXZ(quad[0]), H.v3tov2fXZ(quad[2]), H.v3tov2fXZ(quad[1]), H.v3tov2fXZ(pos)) != 0) {
						pos.y = H.heightInTri(quad[0], quad[2], quad[1], pos) - 0.01f;
						heightList.add(pos);
					}
				}
			}
		}
		
		terrain.setHeights(heightList);
	}
	
	
	@SuppressWarnings("unused")
	private Vector2f getClosestPointOnLine(Vector2f a, Vector2f b, Vector2f p)
    {
        Vector2f ap = p.subtract(a);       //Vector from A to P   
        Vector2f ab = b.subtract(a);       //Vector from A to B  

        float magnitudeAB = ab.lengthSquared();     //Magnitude of AB vector (it's length squared)     
        float ABAPproduct = ap.dot(ab);    //The DOT product of a_to_p and a_to_b     
        float distance = ABAPproduct / magnitudeAB; //The normalized "distance" from a to your closest point  

        if (distance < 0)     //Check if P projection is over vectorAB     
            return null;
        else if (distance > 1)
            return null;
        else
            return a.add(ab.mult(distance));
    }
	@SuppressWarnings("unused")
	//point assumed to be inside
	private float heightOfPointXZ(Vector3f p, Vector3f a, Vector3f b) {
		Vector3f AP = p.subtract(a);       //Vector from A to P   
        Vector3f AB = b.subtract(a);       //Vector from A to B  

        float magnitudeAB = AB.lengthSquared();     //Magnitude of AB vector (it's length squared)     
        float ABAPproduct = H.dotXZ(AP, AB);    //The DOT product of a_to_p and a_to_b     
        float distance = ABAPproduct / magnitudeAB; //The normalized "distance" from a to your closest point  

        if (distance < 0)//Check if P projection is over vectorAB     
            return 0;
        else if (distance > 1)
            return 1;
        return distance;
	}
	
	// interface nodes
	@Override
	public Vector3f getStartPos() { 
		return new Vector3f(10, 260, 0);
	}
	@Override
	public void update(float tpf) { 
		//only way i have found to remove all the log messages it spews
		Logger.getLogger(PerturbFilter.class.getCanonicalName()).setLevel(Level.SEVERE);
	}
	@Override
	public void reset() { }

	@Override
	public void cleanup() {
		this.rootNode.detachAllChildren();
		this.terrain.close();
		this.tL.cleanup();
		App.rally.getStateManager().detach(this.terrain);
		
		super.cleanup();
	}
}
