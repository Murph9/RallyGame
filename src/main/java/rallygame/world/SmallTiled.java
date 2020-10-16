package rallygame.world;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

import rallygame.effects.LoadModelWrapper;
import rallygame.helper.H;
import rallygame.helper.Rand;
import rallygame.helper.Trig;

public class SmallTiled extends World {

    private final Map<Vector2f, CityPiece> tiles;

    public SmallTiled() {
        super("Small Tiled");

        tiles = new HashMap<>();
    }

    private static final int 
            SPAWN_RANGE = 4,
            TILE_SIZE = 20;

    private void placeTiles(Vector3f pos) {
        int x = ((int) ((pos.x + 0.5f)/TILE_SIZE)) * TILE_SIZE;
        int y = ((int) ((pos.z + 0.5f) / TILE_SIZE)) * TILE_SIZE;

        //try to place center
        placeTile(x, y);

        var pos2 = H.v3tov2fXZ(pos);

        //try to place everything inside the bounding box of SPAWN_RANGE
        Vector2f plusXY = pos2.add(new Vector2f(SPAWN_RANGE * TILE_SIZE, SPAWN_RANGE * TILE_SIZE));
        Vector2f minusXY = pos2.add(new Vector2f(-SPAWN_RANGE * TILE_SIZE, -SPAWN_RANGE * TILE_SIZE));
        
        for (var v: SmallTiled.getGridPosBoundingQuad(TILE_SIZE, new Vector2f[] { plusXY, minusXY })) {
            placeTile((int)v.x, (int)v.y);
        }
    }
    
    private void placeTile(int x, int y) {
        var pos = new Vector2f(x, y);
        if (tiles.containsKey(pos))
            return;
        
        var piece = loadPiece(x, y);
        CollisionShape coll = CollisionShapeFactory.createMeshShape(piece.sp);
        piece.sp.addControl(new RigidBodyControl(coll, 0));
        
        rootNode.attachChild(piece.sp);
        getState(BulletAppState.class).getPhysicsSpace().add(piece.sp);

        tiles.put(pos, piece);
    }
    
    private CityPiece loadPiece(int x, int y) {
        var piece = Rand.randFromArray(CityPieceType.values());
        var am = getApplication().getAssetManager();
        Spatial spat = LoadModelWrapper.createWithColour(am, am.loadModel(piece.getName()), ColorRGBA.Green);
        spat.setLocalTranslation(new Vector3f(x, 0, y));
        spat.setLocalScale(TILE_SIZE/10);
        return new CityPiece(piece, spat);
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
        
    }

    @Override
    public void cleanup(Application app) {
        super.cleanup(app);

        for (var t: tiles.values()) {
            t.sp.removeFromParent(); // removes them from the physics space
        }
    }
    
    private class CityPiece {
        
        // private final CityPieceType type;
        private final Spatial sp;
        
        public CityPiece(CityPieceType cpt, Spatial sp) {
            // this.type = cpt;
            this.sp = sp;
        }
    }
    
    private enum CityPieceType {
        STRAIGHT("straight.blend.glb", 9), //1,8
        LEFT_T("leftT.blend.glb", 11), //1,2,8
        RIGHT_T("rightT.blend.glb", 13), //1,4,8
        CROSS("cross.blend.glb", 15), //1,2,4,8
        BUILDING("building.blend.glb", 0),
        ;
        
        //straight = 1, left = 2, right = 4, back = 8
        private boolean[] cons;
        @SuppressWarnings("unused")
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
            return "fullcity/"+name;
        }
    }

    @Override
    public WorldType getType() {
        return WorldType.SMALLTILED;
    }
    

    private static List<Vector2f> getGridPosBoundingQuad(float scale, Vector2f[] extents) {
        List<Vector2f> results = new LinkedList<>();

        float[] box = getTerrainGridBoundingBox(scale, extents);
        for (int i = (int) box[0]; i <= box[2]; i += scale) {
            for (int j = (int) box[1]; j <= box[3]; j += scale) {
                results.add(new Vector2f(i, j));
            }
        }

        return results;
    }

    private static float[] getTerrainGridBoundingBox(float scale, Vector2f[] quad) {
        float[] box = Trig.boundingBoxXZ(quad);
        box[0] = FastMath.floor(box[0] / scale) * scale; // find the closest lower grid point
        box[1] = FastMath.floor(box[1] / scale) * scale;
        box[2] = FastMath.ceil(box[2] / scale) * scale;
        box[3] = FastMath.ceil(box[3] / scale) * scale;

        return box;
    }
}
