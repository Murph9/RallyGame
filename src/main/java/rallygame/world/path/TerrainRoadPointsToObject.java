package rallygame.world.path;

import java.util.LinkedList;
import java.util.List;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Spline;
import com.jme3.math.Vector3f;
import com.jme3.math.Spline.SplineType;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import rallygame.effects.LoadModelWrapper;

/** Makes the road points a real thing */
public class TerrainRoadPointsToObject {
    
    private final float roadWidth;
    public TerrainRoadPointsToObject(float roadWidth) {
        this.roadWidth = roadWidth;
    }

    public List<TerrainRoad> create(List<RoadPointList> pointLists, AssetManager am) {
        var list = new LinkedList<TerrainRoad>();
        for (var pointList : pointLists) {
            var tr = new TerrainRoad();
            tr.points = pointList;
            tr.road = drawRoad(tr.points);
            pointList.road = tr.road; // TODO this is such a mess
            tr.sp = generateSpatial(tr.road, am);
            tr.col = CollisionShapeFactory.createMeshShape(tr.sp);
            list.add(tr);
        }

        return list;
    }

    private CatmullRomRoad drawRoad(List<Vector3f> list) {
        // TODO use a rolling average to smooth points so there are less hard corners

        Spline s3 = new Spline(SplineType.CatmullRom, list, 1, false); // [0-1], 1 is more smooth
        CatmullRomRoad road = new CatmullRomRoad(s3, 5, this.roadWidth);

        // add the extra things
        road.addWidth(CatmullRomRoad.SideA(this.roadWidth));
        road.addWidth(CatmullRomRoad.SideB(this.roadWidth));

        return road;
    }

    private Spatial generateSpatial(CatmullRomRoad road, AssetManager am) {
        var rootNode = new Node("road:"+road);

        CatmullRomWidth c3 = road.middle;
        Geometry g = new Geometry(road+":middle", c3);
        rootNode.attachChild(LoadModelWrapper.create(am, g, ColorRGBA.Green));

        // add the extra things
        for (CatmullRomWidth r : road.others) {
            g = new Geometry(road+":other:"+r, r);
            g.setLocalTranslation(g.getLocalTranslation().add(0, -0.0001f, 0));
            rootNode.attachChild(LoadModelWrapper.create(am, g, ColorRGBA.Brown));
        }

        return rootNode;
    }
}

class TerrainRoad {
    public CatmullRomRoad road;
    public Spatial sp;
    public CollisionShape col;
    public RoadPointList points;
}
