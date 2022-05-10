package rallygame.world;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

import rallygame.effects.LoadModelWrapper;
import rallygame.helper.Geo;
import rallygame.service.ObjectPlacer;
import rallygame.service.ObjectPlacer.ObjectId;

public class TiledFlatWorld extends World {
    
    private static final int GRID_SIZE = 10;
    private static final int TILE_SIZE = 40;
    private static float SPAWN_RANGE = TILE_SIZE*3;
    
	private final HashSet<Vector2f> positions = new HashSet<>();

    private Spatial floor;
    
    private List<ObjectPlacer.ObjectId> addedObjects;
        
    public TiledFlatWorld() {
        super("tiled flat world rootNode");
        
        addedObjects = new LinkedList<>();
    }
    
    @Override
    public WorldType getType() {
        return WorldType.TILEDFLAT;
    }

    @Override
    public void initialize(Application app) {
        super.initialize(app);
        
        Box floorBox = new Box(TILE_SIZE/2, 0.5f, TILE_SIZE/2);
        
        floor = new Geometry("floor", floorBox);
        floor.setLocalTranslation(0, 0, 0);
        floor = LoadModelWrapper.createWithColour(app.getAssetManager(), floor, ColorRGBA.White);
		
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
        var vec = new Vector2f(x, y);
        if (positions.contains(vec))
            return; //already set
        
		positions.add(vec);
        
        Vector3f offset = new Vector3f((vec.x-GRID_SIZE/2)*TILE_SIZE, 0, (vec.y-GRID_SIZE/2)*TILE_SIZE);
        
        Spatial f = floor.clone();
        for (Geometry g: Geo.getGeomList(f))
            g.getMaterial().setColor("Color", ColorRGBA.randomColor());
        
        ObjectPlacer op = getState(ObjectPlacer.class);
        addedObjects.add(op.add(f, offset));
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
