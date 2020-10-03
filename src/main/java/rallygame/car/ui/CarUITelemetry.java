package rallygame.car.ui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Line;
import com.jme3.scene.shape.Quad;

import rallygame.car.data.CarDataConst;
import rallygame.car.ray.GripHelper;
import rallygame.car.ray.RayCarControl;
import rallygame.car.ray.RayWheel;
import rallygame.helper.Colours;
import rallygame.helper.H;
import rallygame.service.averager.AverageV3f;
import rallygame.service.averager.IAverager;
import rallygame.service.averager.IAverager.Type;

public class CarUITelemetry extends BaseAppState {

    private static final float G_FORCE_DOT_SCALE = 75;

    private RayCarControl p;
    private Node rootNode;

    private final WheelUI[] w = new WheelUI[4];
        
    //the g force meter circles
    private Geometry gForceDotCenter;
    private Geometry gForceDotPosition;
    private Geometry gForceGeom;
    private Vector3f gcenter;
    private BitmapText gForceText;
    private final IAverager<Vector3f> gForceAverager;
    
    //some debug text
    private BitmapText statsText;
    
    public CarUITelemetry(RayCarControl p) {
        this.rootNode = new Node("telemetry");
        this.p = p;

        this.gForceAverager = new AverageV3f(30, Type.Simple);
    }
    
    @Override
    public void initialize(Application app) {
        //set the positions of the wheel grid
        int height = app.getCamera().getHeight();
        w[0] = new WheelUI(new Vector3f(80, height*0.9f, 0));
        w[1] = new WheelUI(new Vector3f(200, height*0.9f, 0));
        w[2] = new WheelUI(new Vector3f(80, height*0.75f, 0));
        w[3] = new WheelUI(new Vector3f(200, height*0.75f, 0));
        
        Camera c = app.getCamera();
        AssetManager am = app.getAssetManager();
        
        Material white = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        white.setColor("Color", ColorRGBA.White);
        white.getAdditionalRenderState().setLineWidth(3);
        
        Material black = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        black.setColor("Color", ColorRGBA.Black);
        black.getAdditionalRenderState().setLineWidth(3);

        Material blackThin = black.clone();
        blackThin.getAdditionalRenderState().setLineWidth(1);
        blackThin.getAdditionalRenderState().setWireframe(true);

        Material susM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        susM.setColor("Color", new ColorRGBA(ColorRGBA.White));

        Material susMOff = susM.clone();
        susMOff.setColor("Color", new ColorRGBA(ColorRGBA.White).mult(0.2f));
        
        BitmapFont guiFont = am.loadFont("Interface/Fonts/Default.fnt");
        
        Box b = new Box(120, 120, 1);
        Quad q = new Quad(5, 60);
        Line l = new Line(new Vector3f(0, 0, 10), new Vector3f(1, 0, 10));
        
        for (int i = 0; i < 4; i++) {
            WheelUI w = this.w[i];

            w.gripBox = createGeometry("box-" + i, b, w.pos, white, 1);
            rootNode.attachChild(w.gripBox);

            w.gripBoxOutline = createGeometry("boxOutline-" + i, b, w.pos, blackThin, 1 / 4f);
            rootNode.attachChild(w.gripBoxOutline);

            w.gripDir = createGeometry("line-" + i, l, w.pos, black, 1);
            rootNode.attachChild(w.gripDir);

            w.gripValue = createText(guiFont, ColorRGBA.Black, w.pos.add(new Vector3f(-80, 0, 0)));
            rootNode.attachChild(w.gripValue);

            w.wheelRot = createText(guiFont, ColorRGBA.DarkGray,w.pos.add(new Vector3f(-80, 20, 0)));
            rootNode.attachChild(w.wheelRot);

            w.engineTorque = createText(guiFont, ColorRGBA.Magenta, w.pos.add(new Vector3f(-80, -20, 0)));
            rootNode.attachChild(w.engineTorque);

            w.susBacking = createGeometry("susOff" + i, q, w.pos.add(new Vector3f(30, 30, 0)), susMOff, 1);
            w.susBacking.rotate(0, 0, FastMath.PI);
            rootNode.attachChild(w.susBacking);

            w.sus = createGeometry("sus" + i, q, w.pos.add(new Vector3f(30, 30, 0)), susM, 1);
            w.sus.rotate(0, 0, FastMath.PI);
            rootNode.attachChild(w.sus);
        }
        
        statsText = createText(guiFont, ColorRGBA.White, new Vector3f(200, 150, 0));
        rootNode.attachChild(statsText);
        
        //g force dots
        gcenter = new Vector3f(100, c.getHeight()*0.5f, 0);
        
        b = new Box(5, 5, 1);
        gForceDotCenter = createGeometry("g-circle1", b, gcenter, white, 1);
        rootNode.attachChild(gForceDotCenter);
        
        gForceDotPosition = createGeometry("g-circle2", b, gcenter, white, 1);
        rootNode.attachChild(gForceDotPosition);

        // traction circle (for the g dots)
        float gravity = p.getPhysicsObject().getGravity().length();
        float maxLat = GripHelper.calcMaxLoad(p.getCarData().wheelData[0].pjk_lat);
        float maxLong = GripHelper.calcMaxLoad(p.getCarData().wheelData[0].pjk_long);
        gForceGeom = rallygame.helper.Geo.getXYCircleGeometry(24);
        gForceGeom.setLocalTranslation(gcenter);
        Material m = white.clone();
        m.getAdditionalRenderState().setWireframe(true);
        gForceGeom.setMaterial(m);
        gForceGeom.setLocalScale(2 * G_FORCE_DOT_SCALE * maxLat / (gravity * p.getCarData().mass),
                2 * G_FORCE_DOT_SCALE * maxLong / (gravity * p.getCarData().mass), 1);
        rootNode.attachChild(gForceGeom);
        
        gForceText = createText(guiFont, ColorRGBA.Black, gcenter.subtract(40, 5, 0));
        rootNode.attachChild(gForceText);
    }

    private static Geometry createGeometry(String name, Mesh m, Vector3f pos, Material mat, float scale) {
        Geometry geom = new Geometry(name, m);
        geom.setLocalTranslation(pos);
        geom.setMaterial(mat);
        geom.setLocalScale(scale);
        return geom;
    }
    private static BitmapText createText(BitmapFont font, ColorRGBA colour, Vector3f pos) {
        BitmapText text = new BitmapText(font, false);
        text.setSize(font.getCharSet().getRenderedSize());
        text.setColor(colour);
        text.setText("...");
        text.setLocalTranslation(pos);
        return text;
    }

    @Override
    protected void onEnable() {
        ((SimpleApplication)getApplication()).getGuiNode().attachChild(rootNode);
    }
    @Override
    protected void onDisable() {
        ((SimpleApplication)getApplication()).getGuiNode().detachChild(rootNode);
    }
    
    @Override
    public void update(float tpf) {
        float gravity = p.getPhysicsObject().getGravity().length();

        //stats
        statsText.setText(p.statsString());
        
        // grips
        CarDataConst data = p.getCarData();
        for (int i = 0; i < 4; i++) {
            WheelUI w = this.w[i];
            RayWheel wheel = p.getWheel(i);

            Material m = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            w.gripValue.setText(String.format("%.2f slip", wheel.skidFraction));
            w.wheelRot.setText(String.format("%.2f rad/s", wheel.radSec));
            w.engineTorque.setText(H.decimalFormat(p.getWheelTorque(i), "0000.0") + " Nm");
            m.setColor("Color", getGripBoxColour(wheel.skidFraction));
            w.gripBox.setMaterial(m);

            Vector3f dir = wheel.gripDir;
            if (dir != null) {
                w.gripBox.setLocalScale(wheel.susForce / (p.getCarData().mass * gravity));
                w.gripDir.setLocalScale(wheel.susForce / p.getCarData().susByWheelNum(i).max_force * 1000);

                float angle = FastMath.atan2(dir.z, dir.x);
                Quaternion q = new Quaternion();
                q.fromAngleAxis(angle, Vector3f.UNIT_Z);
                w.gripDir.setLocalRotation(q);
            }

            w.sus.setLocalScale(1, wheel.susRayLength / data.susByWheelNum(i).travelTotal(), 1);
        }
        
        // g forces
        // needs to be translated from local into screen axis
        Vector3f gs = p.getPlanarGForce().mult(1 / gravity);
        gs.y = gs.z; // z is front back
        gs.z = 0; // screen has no depth
        gs = gForceAverager.get(gs);
        gForceDotPosition.setLocalTranslation(gcenter.add(gs.mult(G_FORCE_DOT_SCALE))); // because screen pixels
        gForceText.setText("x: " + H.roundDecimal(gs.x, 2) + ", y: " + H.roundDecimal(gs.y, 2));
    }
    private ColorRGBA getGripBoxColour(float value) {
        return Colours.getOnRGBScale(value / 3f);
    }

    @Override
    public void cleanup(Application app) {
        ((SimpleApplication)app).getGuiNode().detachChild(rootNode);
    }
    
    private class WheelUI {
        final Vector3f pos;
        Geometry gripBox;
        Geometry gripBoxOutline;
        Geometry gripDir;

        BitmapText gripValue;
        BitmapText wheelRot;
        BitmapText engineTorque;

        Geometry sus;
        Geometry susBacking; //background color
        
        public WheelUI(Vector3f pos) {
            this.pos = pos;
        }
    }
}
