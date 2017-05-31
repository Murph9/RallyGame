package world.highway;

import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.terrain.noise.ShaderUtils;
import com.jme3.terrain.noise.basis.FilteredBasis;
import com.jme3.terrain.noise.filter.*;
import com.jme3.terrain.noise.fractal.FractalSum;
import com.jme3.terrain.noise.modulator.NoiseModulator;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;
import com.jme3.util.BufferUtils;

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
- eventually overlay it on terrain
 + flatten the terrain under the road so it 'fits'
- add grass and trees all TDU like
*/

//try and use:
//https://github.com/JulienGreen/RoadTessalation
//https://hub.jmonkeyengine.org/t/vertexbuffer-for-specific-uv-behavior-shaders/38268/22

public class HighwayWorld extends World {
	private Terrain terrain;
	private int blockSize; //(distance between points)/tileSize
	private int tileSize; //the grid piece size
	
	private static final int[] indexes = { 2,0,1, 1,3,2 }; //tyre marks vertex order
	private static final Vector2f[] texCoord = new Vector2f[] { //texture of quad with order
		new Vector2f(0, 0), new Vector2f(0, 1), new Vector2f(1, 0), new Vector2f(1, 1),
	};
	
	public HighwayWorld() {
		super("highwayWorldRoot");
		//TODO this needs the road part, suggest something really simple to start with
		//uses the TerrainListener class
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
		// TODO remember the ./world folder with the cached terrain pieces, always cache to memory
		NoiseBasedWorld newWorld = new NoiseBasedWorld(App.rally, App.rally.getPhysicsSpace(), tileSize, blockSize, rootNode);

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
		therm.setTalus(0.011f); //0.011

		SmoothFilter smooth = new SmoothFilter();
		smooth.setRadius(1);
		smooth.setEffect(0.7f);

		IterativeFilter iterate = new IterativeFilter();
		iterate.addPreFilter(perturb);
		iterate.addPostFilter(smooth);
		iterate.setFilter(therm);
		iterate.setIterations(1); //higher numbers make it really smooth

		ground.addPreFilter(iterate);

		newWorld.setFilteredBasis(ground);

		this.terrain = newWorld;
		App.rally.getStateManager().attach(this.terrain);
		
		//set after so the terrain exists first
		newWorld.setTileListener(new TerrainListener(this));
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

		terrainMaterial.setFloat("terrainSize", blockSize);

		return terrainMaterial;
	}
	
	protected void generateRoad(Vector3f start, Vector3f end) {
		H.p("New arrow", start, "-->", end);
		float roadWidth = 6;
		Vector3f[] rect = H.rectFromLine(start, end, roadWidth);
		if (rect == null)
			return;
		
		//draw it
		rootNode.attachChild(H.makeShapeArrow(App.rally.getAssetManager(), ColorRGBA.Cyan, end.add(0,0.01f,0).subtract(start), start.add(0,0.01f,0)));
		
		Mesh mesh = new Mesh();		
		mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(new Vector3f[] {rect[0],rect[1],rect[3],rect[2]}));
		mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoord));
		mesh.setBuffer(Type.Index,    3, BufferUtils.createIntBuffer(indexes));
		mesh.updateBound();
		
		Geometry geo = new Geometry("road", mesh);
		Material mat = new Material(App.rally.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		mat.setColor("Color", new ColorRGBA(0,0,0,0.5f));
		mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
		geo.setShadowMode(ShadowMode.Off);
		geo.setQueueBucket(Bucket.Transparent);
		geo.setMaterial(mat);
		rootNode.attachChild(geo);
		//end draw it
		
		//remember that the chunk hasn't been loaded into the world by here yet
		if (terrain == null) {
			H.e("STOP touching the terrain thing that you don't understand.");
			return;//????, only happens when someone plays around with the terrain init order and breaks something
		}
		
		//a1, b2, c3, d0 {index to char map} 
		Vector3f AB = rect[2].subtract(rect[1]); //(2 - 1)
		Vector3f BC = rect[3].subtract(rect[2]); //(3 - 2)
		
		List<Vector3f> heightList = new LinkedList<Vector3f>();
		float[] box = H.boundingBoxXZ(rect);
		box[0] -= 1; //extend the extends so they cover it completely
		box[1] -= 1;
		box[2] += 1;
		box[3] += 1;
		
		for (int i = (int)box[0]; i < box[2]; i++) {
			for (int j = (int)box[1]; j < box[3]; j++) {
				Vector3f pos = new Vector3f(i, 0, j);

				//http://stackoverflow.com/a/2763387 points in rectangle
				Vector3f AM = pos.subtract(rect[1]); //(m - 1)
				Vector3f BM = pos.subtract(rect[2]); //(m - 2)
				float abamd = H.dotXZ(AB,AM);
				float bcbmd = H.dotXZ(BC,BM);
				
				if (0 <= abamd && abamd <= H.dotXZ(AB,AB) && 0 <= bcbmd && bcbmd <= H.dotXZ(BC,BC)) {
					float t = heightOfPointXZ(pos, start, end);
					pos.y = (end.y - start.y)*t + start.y - 0.01f; //TODO this is not 'perfect', assumes its a line not a box
					heightList.add(pos);
				}
			}
		}
		
		terrain.setHeights(heightList);
	}
	
	//TODO bilinear interoplation instead
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
		Vector3f pos = new Vector3f(0, 103, 0);
		if (terrain == null)
			return pos;
		if (!terrain.isLoaded())
			return pos;
		return null; //else never again
	}
	@Override
	public void update(float tpf) { }
	@Override
	public void reset() { }

	@Override
	public void cleanup() {
		this.rootNode.detachAllChildren();
		this.terrain.close();
		App.rally.getStateManager().detach(this.terrain);
		
		super.cleanup();
	}
}
