package rallygame.world;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.HeightfieldCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.control.Control;
import com.jme3.terrain.geomipmap.TerrainQuad;

import rallygame.service.PerlinNoise;

public class PathWorld extends World {

    private PerlinNoise noise;
    private TerrainQuad terrain;
    private Control collision;

    private final int sideLength;

    public PathWorld() {
        super("PathWorld");

        sideLength = 65;
    }

    @Override
    public void reset() {
    }

    @Override
    public WorldType getType() {
        return WorldType.PATH;
    }

    @Override
    public void initialize(Application app) {
        super.initialize(app);

        //TODO copy "Common/MatDefs/Terrain/Terrain.j3md" for proper height material colour stuff

        noise = new PerlinNoise(sideLength);
        noise.load();

        terrain = new TerrainQuad("path terrain", sideLength, sideLength, noise.getHeightMap());
        Material baseMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        baseMat.setColor("Color", ColorRGBA.Magenta);
        terrain.setMaterial(baseMat);

        this.rootNode.attachChild(terrain);

        collision = new RigidBodyControl(new HeightfieldCollisionShape(terrain.getHeightMap(), terrain.getLocalScale()), 0);
        terrain.addControl(collision);
        getState(BulletAppState.class).getPhysicsSpace().add(collision);

        //TODO calc based on this http://blog.runevision.com/2016/03/note-on-creating-natural-paths-in.html
        
    }

    @Override
    protected void cleanup(Application app) {

        this.rootNode.detachChild(terrain);
        super.cleanup(app);
    }
}