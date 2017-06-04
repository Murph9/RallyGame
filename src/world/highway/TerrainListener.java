package world.highway;

import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;

import game.App;
import helper.H;
import terrainWorld.TerrainChunk;
import terrainWorld.TileListener;

public class TerrainListener implements TileListener {

	private HighwayWorld world;
	
	private Vector3f lastPoint;
	private Vector3f nextPoint;
	private float angle = -90*FastMath.DEG_TO_RAD;
	public TerrainListener(HighwayWorld world) {
		this.world = world;
		
		nextPoint = new Vector3f(Vector3f.ZERO);
		setNextPoint(null); //init next point
	}
	
	/* TODO: a test for the terrain heights
	private int alreadyDone;
	public boolean tileLoaded(TerrainChunk chunk) {
		if (App.rally.IF_DEBUG) {
			Vector3f center = chunk.getLocalTranslation();
			float size = chunk.getTerrainSize();
			Vector3f positiveXZ = center.add(size/2,105,size/2);
			App.rally.getRootNode().attachChild(H.makeShapeArrow(App.rally.getAssetManager(), ColorRGBA.Red, new Vector3f(0,0,-size), positiveXZ));
			App.rally.getRootNode().attachChild(H.makeShapeArrow(App.rally.getAssetManager(), ColorRGBA.Red, new Vector3f(-size,0,0), positiveXZ));
		}
		if (alreadyDone >= 1)
			return true;
		
		world.generateRoad(new Vector3f(0,100,30), new Vector3f(0,95,20));
		
		alreadyDone++;
		return true;
	}
//	*/
	
	public boolean tileLoaded(TerrainChunk chunk) {
		if (App.rally.IF_DEBUG)
			App.rally.getRootNode().attachChild(H.makeShapeBox(App.rally.getAssetManager(), ColorRGBA.Green, chunk.getLocalTranslation().add(0,110,0), 2));

		while (true) {
			float height = chunk.getHeight(new Vector2f(nextPoint.x,nextPoint.z));
			if (Float.isNaN(height)) {
				break;
			}
			nextPoint.y = height;
			
			setNextPoint(chunk);
		}
		//TODO sometimes this will just stop placing road
		
		return true;
	}
	private void setNextPoint(TerrainChunk chunk) {
		lastPoint = lastPoint == null ? new Vector3f(Vector3f.ZERO) : lastPoint;
		if (chunk == null) {
			//first load. ignore for now
			H.p("Initial terrain load");
			return;
		}

		world.generateRoad(lastPoint, nextPoint);
		
		angle += FastMath.DEG_TO_RAD*FastMath.nextRandomFloat()*3*(FastMath.nextRandomFloat() < 0.5f ? -1 : 1);
		Quaternion rot = new Quaternion().fromAngleAxis(angle, Vector3f.UNIT_Y);
		
		Vector3f next = new Vector3f(FastMath.nextRandomFloat()*20 + 15, 0, 0);
		Vector3f point = nextPoint.add(rot.mult(next));
		
//		Vector3f point = nextPoint.add(20*FastMath.nextRandomFloat(), 0, 20*FastMath.nextRandomFloat());
		lastPoint = nextPoint;
		nextPoint = point;
		nextPoint.y = 0;
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