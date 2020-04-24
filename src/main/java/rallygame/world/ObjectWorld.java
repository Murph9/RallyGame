package rallygame.world;

import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

import rallygame.effects.LoadModelWrapper;
import rallygame.helper.Geo;
import rallygame.service.ObjectPlacer;
import rallygame.service.ObjectPlacer.ObjectId;
import jme3tools.optimize.GeometryBatchFactory;

public class ObjectWorld extends World {
	
	private static final int 
		COUNT_A_TILE = 10,
		GRID_SIZE = 10,
		TILE_SIZE = 40,
		SPAWN_RANGE = 40;
	
	private static final String ITEM = "objects/tree_0.blend.glb";
	
	private Spatial floor;
	
	private Spatial geomI;
	private List<ObjectPlacer.ObjectId> addedObjects;
	
	private boolean[][] grid;
	
	public ObjectWorld() {
		super("object world rootNode");
		
		addedObjects = new LinkedList<>();
		grid = new boolean[GRID_SIZE][GRID_SIZE];
	}
	
	@Override
	public WorldType getType() {
		return WorldType.OBJECT;
	}

	@Override
	public void initialize(Application app) {
		super.initialize(app);
		
		AssetManager am = app.getAssetManager();
		
		Material matfloor = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		matfloor.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		matfloor.setColor("Color", ColorRGBA.Green);
		
		Box floorBox = new Box(TILE_SIZE/2, 0.5f, TILE_SIZE/2);
		
		floor = new Geometry("floor", floorBox);
		floor.setMaterial(matfloor);
		floor.setLocalTranslation(0, 0, 0);
		floor = LoadModelWrapper.create(app.getAssetManager(), floor, ColorRGBA.White);
		
		Spatial spat = LoadModelWrapper.create(app.getAssetManager(), ITEM);
		if (spat instanceof Node) {
			for (Spatial s: ((Node) spat).getChildren()) {
				geomI = s;
			}
		} else {
			this.geomI = (Geometry) spat;
		}
		
		placeTiles(new Vector3f(0,0,0));
	}
	
	private void placeTiles(Vector3f pos) {
		int x = Math.round(((pos.x+GRID_SIZE/2)/TILE_SIZE) + GRID_SIZE/2);
		int y = Math.round(((pos.z+GRID_SIZE/2)/TILE_SIZE) + GRID_SIZE/2);

		//try to place center
		placeTile(x, y);

		//try to place everything inside the bounding box of SPAWN_RANGE
		
		Vector3f plusXY = pos.add(SPAWN_RANGE, 0, SPAWN_RANGE);
		Vector3f minusXY = pos.add(-SPAWN_RANGE, 0, -SPAWN_RANGE);
		

		int plusX = Math.round(((plusXY.x+GRID_SIZE/2)/TILE_SIZE) + GRID_SIZE/2);
		int plusY = Math.round(((plusXY.z+GRID_SIZE/2)/TILE_SIZE) + GRID_SIZE/2);
		
		int minusX = Math.round(((minusXY.x+GRID_SIZE/2)/TILE_SIZE) + GRID_SIZE/2);
		int minusY = Math.round(((minusXY.z+GRID_SIZE/2)/TILE_SIZE) + GRID_SIZE/2);
		
		for (int i = minusX; i <= plusX; i++) {
			for (int j = minusY; j <= plusY; j++) {
				placeTile(i,j);
			}
		}
	}
	
	private void placeTile(int x, int y) {
		if ((x < 0 || x >= GRID_SIZE) || (y < 0 || y >= GRID_SIZE)) {
			//out of bounds
			return;
		}
		
		if (grid[y][x])
			return; //already set
		
		grid[y][x] = true;
		
		Vector3f offset = new Vector3f((x-GRID_SIZE/2)*TILE_SIZE, 0, (y-GRID_SIZE/2)*TILE_SIZE);
		
		Spatial f = floor.clone();
		for (Geometry g: Geo.getGeomList(f))
			g.getMaterial().setColor("Color", ColorRGBA.randomColor());
		
		ObjectPlacer op = getState(ObjectPlacer.class);
		addedObjects.add(op.add(f, offset));
				
		for (int i = 0; i < COUNT_A_TILE; i++) {
			Spatial s = geomI.clone();
			
			Vector3f tilePos = new Vector3f((float)(2*Math.random()-1)*TILE_SIZE/2, 0, (float)(2*Math.random()-1)*TILE_SIZE/2);
			Vector3f pos = offset.add(tilePos);
			addedObjects.add(op.add(s, pos));
		}
		
		//If you remove this line: fps = fps/n for large n
		GeometryBatchFactory.optimize(rootNode);
	}

	@Override
	public Transform getStart() {
		return new Transform(new Vector3f(0, 2, 0));
	}

	@Override
	public void update(float tpf) {
		placeTiles(getApplication().getCamera().getLocation());
	}

	@Override
	public void reset() {
		rootNode.detachAllChildren();

		ObjectPlacer op = getState(ObjectPlacer.class);
		for (ObjectId g : addedObjects) {
			op.remove(g);
		}
		addedObjects.clear();
		
		for (int i = 0 ; i < grid.length; i++)
			for (int j = 0; j < grid.length; j++)
				grid[i][j] = false;
		
		placeTiles(new Vector3f(0,0,0));
	}
	
	@Override
	public void cleanup(Application app) {
		super.cleanup(app);
		
		ObjectPlacer op = getState(ObjectPlacer.class);
		for (ObjectId g : addedObjects) {
			op.remove(g);
		}
	}
}
