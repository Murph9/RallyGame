package world.highway;

import java.util.Arrays;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

import game.App;
import helper.H;
import helper.Log;
import terrainWorld.NoiseBasedWorld;
import terrainWorld.TerrainChunk;
import terrainWorld.TileListener;

//TODO rename to road maker or something
//TODO should fetch the heights based on the noise generator, instead of the terrain
//	should help generation code, and also allow preperation for more world at once
public class RoadMaker implements TileListener {

	private enum CurveTypes {
		//defined as going straight is +z(y) only
		Straight(new Vector2f[] { new Vector2f(0,0), new Vector2f(0,40), new Vector2f(0,40), new Vector2f(0,80) }),
		
//		Right(new Vector2f[] { new Vector2f(0,0), new Vector2f(0,40), new Vector2f(-40,80), new Vector2f(-80,80) }),
//		Left(new Vector2f[] { new Vector2f(0,0), new Vector2f(0,40), new Vector2f(40,80), new Vector2f(80,80) }),
		;
		//TODO make more on: https://www.desmos.com/calculator/cahqdxeshd
		//note that a line from 0,0 to 20,30 on that should be 0,0 to -20,30
		
		public final Vector2f[] points; 
		CurveTypes(Vector2f[] ps) {
			this.points = ps;
		}
	}
	
	private App app;
	private HighwayWorld world;
	private NoiseBasedWorld terrain;
	
	//you can calculate the last pos and the last rot based on these points
	private Vector3f[] lastPoints = new Vector3f[] { new Vector3f(0,0,0), new Vector3f(0,0,0), new Vector3f(0,0,0), new Vector3f(0,0,1) };
	
	private int totalChunks;
	private int totalLoaded;
	private boolean terrainDoneLoading;
	
	public RoadMaker(App app, HighwayWorld world) {
		this.app = app;
		this.world = world;
		this.terrain = world.terrain;
		this.totalChunks = terrain.getTotalVisibleChunks();
		
		//TODO random start angle Log.p("Terrain started with direction: ", angle);
		generateRoadBit(); //init first road
	}
	
	private float getHeightArray(Vector3f pos) {
		return getHeightArray(H.v3tov2fXZ(pos));
	}
	private float getHeightArray(Vector2f pos) {
		int bs = world.terrain.blockSize;
		float[] heights = world.terrain.getHeightmap((int)pos.x/(bs - 1), (int)pos.y/(bs - 1)); // `/(bs -1)` because it does the opposite
		return heights[(heights.length/2)+1] * terrain.getWorldHeight();
	}
	
	private void initRoads(TerrainChunk chunk) {
		Log.p("initing roads");
		
		while (true) {
			float height = 0;
			Vector2f pos = H.v3tov2fXZ(lastPoints[3]);
			TerrainChunk tc = terrain.chunkFor(pos);
			if (tc != null)
				height = tc.getHeight(pos);
			
			if (Float.isNaN(height) || height == 0) {
				height = chunk.getHeight(pos); //try last chunk (that won't be loaded at the moment)
				if (!Float.isNaN(height) || height != 0) {
					break;
				}
			}

			generateRoadBit();
		}
	}
	
	@Override
	public boolean tileLoaded(TerrainChunk chunk) {
		totalLoaded++;
		
		if (App.IF_DEBUG)
			app.getRootNode().attachChild(H.makeShapeBox(app.getAssetManager(), ColorRGBA.Green, chunk.getLocalTranslation().add(0,110,0), 2));
		
		//terrain needs to load all of its tiles before we will use the grow method
		//this prevents the order of the tiles screwing with placements
		if (totalLoaded == totalChunks) {
			terrainDoneLoading = true;
			initRoads(chunk);
		}

		if (!terrainDoneLoading)
			return true;
		
		while (true) {
			float height = chunk.getHeight(H.v3tov2fXZ(lastPoints[3]));
			if (Float.isNaN(height) || height == 0) {
				break; //no more space to load road on
			}
			
			generateRoadBit();
		}
		
		return true;
	}
	
	private void generateRoadBit() {
		//pick from the list of biezer curves
		Vector2f[] points = H.randFromArray(CurveTypes.values()).points;

		//transform points based of the last points
		Vector3f[] newPoints = new Vector3f[] {
				H.v2tov3fXZ(points[0]),
				H.v2tov3fXZ(points[1]),
				H.v2tov3fXZ(points[2]),
				H.v2tov3fXZ(points[3]),
			};
		
		float oldAngle = FastMath.atan2(lastPoints[3].z - lastPoints[2].z, lastPoints[3].x - lastPoints[2].x) - FastMath.HALF_PI;
		Quaternion q = new Quaternion().fromAngleAxis(-oldAngle, Vector3f.UNIT_Y); 
		for (int i = 0; i < newPoints.length; i++) {
			newPoints[i] = q.mult(newPoints[i]).add(lastPoints[3]);
			newPoints[i].y = getHeightArray(newPoints[i]);
		}
		
		RoadMesh m = new RoadMesh(5, 2, Arrays.asList(newPoints));
		world.generateRoad(m);
		
		app.getRootNode().attachChild(H.makeShapeArrow(app.getAssetManager(), ColorRGBA.White, q.mult(new Vector3f(10,0,0)), lastPoints[3]));
		app.getRootNode().attachChild(H.makeShapeArrow(app.getAssetManager(), ColorRGBA.Red, newPoints[3].subtract(newPoints[0]), newPoints[0]));
		
		lastPoints = newPoints;
		
		Log.p("next road point:", lastPoints[3]);
	}
	
	@Override
	public void tileLoadedThreaded(TerrainChunk terrainChunk) {
		//Do not add anything to the scene with this method as its on another thread
		//TODO use this method to keep it on the other thread?
	}
	@Override
	public boolean tileUnloaded(TerrainChunk terrainChunk) { return true; }
	
	@Override
	public String imageHeightmapRequired(int x, int z) { return null; }
}