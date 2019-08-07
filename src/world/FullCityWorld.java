package world;

import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

import game.App;
import game.LoadModelWrapper;
import jme3tools.optimize.GeometryBatchFactory;

public class FullCityWorld extends World {

	private static final int 
			GRID_SIZE = 40,
			SPAWN_RANGE = 40,
			TILE_SIZE = 10;
	private CityPiece[][] grid;
	
	public FullCityWorld() {
		super("full city world rootNode");
		
		grid = new CityPiece[40][40];
	}
	
	@Override
	public WorldType getType() {
		return WorldType.FULLCITY;
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
		
		if (grid[y][x] != null)
			return; //already set
		
		grid[y][x] = decidePiece(x, y);
		
		Vector3f offset = new Vector3f((x-GRID_SIZE/2)*TILE_SIZE, 0, (y-GRID_SIZE/2)*TILE_SIZE);
		Spatial spat = LoadModelWrapper.create(app.getAssetManager(), grid[y][x].p.getName(), ColorRGBA.White);
		
		spat.setLocalTranslation(offset);
		CollisionShape coll = CollisionShapeFactory.createMeshShape(spat);
		spat.addControl(new RigidBodyControl(coll, 0));
		
		rootNode.attachChild(spat);
		App.rally.getPhysicsSpace().add(spat);
		
		//If you remove this line: fps = fps/n for large n
		GeometryBatchFactory.optimize(rootNode);
	}
	
	private CityPiece decidePiece(int x, int y) {
		CityPieceType[] types = CityPieceType.values();
		return new CityPiece(types[FastMath.nextRandomInt(0, types.length - 1)]);
	}

	@Override
	public Vector3f getStartPos() { return new Vector3f(0,2,0); }

	@Override
	public void update(float tpf) {
		placeTiles(App.rally.getCamera().getLocation());
	}

	@Override
	public void reset() {
		try {
			throw new Exception("TODO - Full city is not being deleted.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void cleanup() {
		super.cleanup();
		try {
			throw new Exception("Full city is not being deleted.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private class CityPiece {
		
		private CityPieceType p;
		
		public CityPiece(CityPieceType cpt) {
			p = cpt;
		}
	}
	
	private enum CityPieceType {
		STRAIGHT("straight.blend", 9), //1,8
		LEFT_T("leftT.blend", 11), //1,2,8
		RIGHT_T("rightT.blend", 13), //1,4,8
		CROSS("cross.blend", 15), //1,2,4,8
		BUILDING("building.blend", 0),
		;
		
		//straight = 1, left = 2, right = 4, back = 8
		private boolean[] cons;
		@SuppressWarnings("unused") //TODO
		public boolean Straight() { return cons[0]; }
		@SuppressWarnings("unused")
		public boolean Left() { return cons[1]; }
		@SuppressWarnings("unused")
		public boolean Right() { return cons[2]; }
		@SuppressWarnings("unused")
		public boolean Back() { return cons[3]; }
		
		private String name;
		
		CityPieceType(String name, int dirs) {
			cons = new boolean[] {(dirs&1)==1, (dirs&2)==2, (dirs&4)==4, (dirs&8)==8};
			this.name = name;
		}
		
		public String getName() {
			return "assets/fullcity/"+name;
		}
	}
}
