package rallygame;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Spline;
import com.jme3.math.Vector3f;
import com.jme3.math.Spline.SplineType;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.terrain.geomipmap.TerrainQuad;

import rallygame.effects.FilterManager;
import rallygame.helper.Log;
import rallygame.service.PerlinNoise;
import rallygame.world.path.CatmullRomRoad;

public class FilterTransparencyTest extends SimpleApplication {
    public static void main(String[] args) {
        FilterTransparencyTest app = new FilterTransparencyTest();
        app.setDisplayStatView(true);
        app.start();
    }

    @Override
    public void simpleInitApp() {
        inputManager.setCursorVisible(true);

        FilterManager fm = new FilterManager();
        getStateManager().attach(fm);

        FilterTransparencyState state = new FilterTransparencyState();
        getStateManager().attach(state);
    }
}

class FilterTransparencyState extends BaseAppState {
    
    private Node rootNode;

    // This class can be used to figure out why the outlines of some mesh can be seen through others.
    // generally its because it doesn't have any normals

    @Override
    protected void initialize(Application app) {
        rootNode = new Node("root node");
        ((SimpleApplication)app).getRootNode().attachChild(rootNode);
        AssetManager am = app.getAssetManager();

        Material baseMat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        baseMat.setColor("Color", ColorRGBA.Green);
        baseMat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);

        Vector3f[] list = new Vector3f[] { new Vector3f(0, 0, -10), new Vector3f(0, 0, 10) };
        Spline s = new Spline(SplineType.CatmullRom, list, 1, false);
        CatmullRomRoad road = new CatmullRomRoad(s, 1, 5);
        Geometry g = new Geometry("road", road);
        g.setMaterial(baseMat);
        this.rootNode.attachChild(g);

        Box b = new Box(1,1,1);
        Geometry boxG = new Geometry("box", b);
        Material boxMat = baseMat.clone();
        boxMat.setColor("Color", ColorRGBA.Blue);
        boxG.setMaterial(boxMat);
        this.rootNode.attachChild(boxG);

        app.getCamera().setLocation(new Vector3f(3, 3, 3));
        app.getCamera().lookAt(new Vector3f(0,0,0), Vector3f.UNIT_Y);


        int sideLength = 9;
        PerlinNoise noise = new PerlinNoise(sideLength, 0);
        noise.load();

        float[] heightMap = noise.getHeightMap();
        TerrainQuad terrain = new TerrainQuad("path terrain", sideLength, sideLength, heightMap);
        Material tMat = new Material(am, "MatDefs/terrainheight/TerrainColorByHeight.j3md");
        baseMat.setColor("LowColor", new ColorRGBA(1.0f, 0.55f, 0.0f, 1.0f));
        baseMat.setColor("HighColor", new ColorRGBA(0.0f, 0.0f, 1.0f, 1.0f));
        tMat.setFloat("Scale", 0.8f); // margin of 0.1f
        tMat.setFloat("Offset", 0.1f);
        terrain.setMaterial(tMat);
        
        this.rootNode.attachChild(terrain);
    }

    @Override
    protected void cleanup(Application app) {
        ((SimpleApplication) app).getRootNode().detachChild(rootNode);
        Log.p("TestTransparencyStateTest end");
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }
}