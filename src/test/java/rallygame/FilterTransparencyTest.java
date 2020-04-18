package rallygame;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Spline;
import com.jme3.math.Vector3f;
import com.jme3.math.Spline.SplineType;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;

import rallygame.effects.FilterManager;
import rallygame.helper.Log;
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
        inputManager.deleteMapping(INPUT_MAPPING_EXIT); // no esc close pls

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

        Material baseMat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
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