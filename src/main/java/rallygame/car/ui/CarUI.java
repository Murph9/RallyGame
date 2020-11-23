package rallygame.car.ui;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;

import rallygame.car.ray.RayCarControl;
import rallygame.car.ray.ICarPowered;
import rallygame.helper.Geo;

public class CarUI extends BaseAppState {

    private static final float startAng = FastMath.PI * 5 / 4;
    private static final float finalAng = 0;

    private static final int SPEEDO_HEIGHT = 200;
    private static final int SPEEDO_WIDTH = 200;
    private static final int radius = 100;

    private static final String numImageFolder = "number/"; // texture location

    private final RayCarControl p;
    private Node rootNode;
    
    //hud stuff
    private Geometry background;
    
    //rpm
    private Geometry rpmQuad;
    private Material rpmMat;

    //other meters
    private Geometry nitro, nitroOff; //quads that display nitro
    private Geometry throttle, throttleOff; //quads that display throttle 
    private Geometry brake, brakeOff; //quads that display braking
    private Geometry steer, steerOff; //quads that display turn value
    
    //texture
    private Material[] numMats = new Material[11]; //texture set
    private Material numMatBlank;
    
    private Geometry[] speedo = new Geometry[5]; //speed squares
    private Geometry gear = new Geometry(); //gear label
    
    //speedo numbers
    private int finalRPM; //should be more than redline
    private float redline;

    /////telemetry
    private boolean showTelemetry;
    private CarUITelemetry telemetry;
    
    private ActionListener actionListener = new ActionListener() {
        public void onAction(String name, boolean keyPressed, float tpf) {
            if (keyPressed) return; 
            if (name.equals("Telemetry")) {
                toggleTelemetry();
            }
        }
    };
    
    public CarUI(RayCarControl p) {
        this.p = p;
    }
    
    @Override
    protected void initialize(Application app) {
        this.redline = p.getCarData().e_redline;
        this.finalRPM = (int) FastMath.ceil(this.redline) + 1000;

        SimpleApplication r = (SimpleApplication)app;

        AppSettings settings = app.getContext().getSettings();
        rootNode = new Node("local root");
        r.getGuiNode().attachChild(rootNode);
        
        // the normal size is a 1200 screen
        float scale = settings.getWidth()/1200f;
        rootNode.setLocalTranslation(settings.getWidth() - SPEEDO_WIDTH*scale, 0, 0);
        rootNode.scale(scale);
        AssetManager am = r.getAssetManager();
        
        //////////////////////////////
        //speedo number textures
        for (int i = 0 ; i < 10; i++) {
            numMats[i] = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
            numMats[i].setTexture("ColorMap", am.loadTexture(numImageFolder + i + ".png"));
            numMats[i].getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        }
        //a blank one
        numMatBlank = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        numMatBlank.setTexture("ColorMap", am.loadTexture(numImageFolder + "blank.png"));
        numMatBlank.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        
        
        makeSpeedo(am, settings);

        /////////////
        //telemetry
        InputManager i = r.getInputManager();
        
        i.addMapping("Telemetry", new KeyTrigger(KeyInput.KEY_HOME));
        i.addListener(actionListener, "Telemetry");
        
        boolean telemetryEnabled = false;
        if (telemetry != null) // if this was previously loaded 
            telemetryEnabled = telemetry.isEnabled();
        
        telemetry = new CarUITelemetry(p);
        getStateManager().attach(telemetry);
        telemetry.setEnabled(telemetryEnabled);
    }

    private void makeSpeedo(AssetManager am, AppSettings settings) {
        makeKmH(settings);
        
        ///////////////
        //make the variable parts:        
        Quad qback = new Quad(SPEEDO_WIDTH, SPEEDO_HEIGHT);
        background = new Geometry("ui-background", qback);
        background.setCullHint(CullHint.Never);
        Material trans = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        trans.setColor("Color", new ColorRGBA(0, 0, 0, 0.5f));
        trans.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        background.setMaterial(trans);
        background.setLocalTranslation(0, 0, -5);
        
        rootNode.attachChild(background);
        
        //rpm bars 2
        Quad rpmQ = new Quad(SPEEDO_WIDTH, SPEEDO_HEIGHT);
        rpmQuad = new Geometry("ui-background", rpmQ);
        background.setCullHint(CullHint.Never);
        rpmMat = new Material(am, "MatDefs/Radial.j3md");
        rpmMat.setTransparent(true);
        rpmMat.setTexture("ThresholdMap", am.loadTexture("image/radialgradient_large.png"));
        rpmMat.setFloat("Threshold", 0);
        rpmMat.setColor("Color", ColorRGBA.White);
        rpmMat.getAdditionalRenderState().setBlendMode(BlendMode.Additive);
        rpmQuad.setMaterial(rpmMat);
        rpmQuad.setLocalTranslation(0, 0, -4);
        rootNode.attachChild(rpmQuad);
        
        //rpm bars
        final int quadXSize = 20;
        final int quadYSize = 20;
        Quad quad = new Quad(quadXSize, quadYSize);
        
        final float innerArc = 0.81f * radius;
        final float outerArc = 0.94f * radius;        

        final int increment = 100;
        for (int i = (int)Math.floor(redline / increment) * increment; i < finalRPM+1; i += increment) {
            float angle = FastMath.interpolateLinear(i / (float) finalRPM, startAng, finalAng);
            float angle2 = FastMath.interpolateLinear((i + increment) / (float) finalRPM, startAng, finalAng);
            
            Vector3f[] corners = new Vector3f[] {
                    new Vector3f(FastMath.cos(angle) * outerArc, FastMath.sin(angle) * outerArc, 0),
                    new Vector3f(FastMath.cos(angle) * innerArc, FastMath.sin(angle) * innerArc, 0),
                    new Vector3f(FastMath.cos(angle2) * outerArc, FastMath.sin(angle2) * outerArc, 0),
                    new Vector3f(FastMath.cos(angle2) * innerArc, FastMath.sin(angle2) * innerArc, 0),
                };
            Mesh mq = Geo.createQuad(corners);

            Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
            mat.setColor("Color", new ColorRGBA(ColorRGBA.Red));
            Geometry redLine2 = new Geometry("redline2", mq);
            redLine2.setMaterial(mat);
            redLine2.setLocalTranslation(SPEEDO_WIDTH/2, SPEEDO_HEIGHT/2, 0);
            rootNode.attachChild(redLine2);
        }

        // generate numbers around the arc and 1k notches
        for (int i = 0; i < finalRPM+1; i+=1000) {
            float angle = FastMath.interpolateLinear(i / (float) finalRPM, startAng, finalAng);
            Node g = addRPMNumber(angle, (int)i/1000, quad, SPEEDO_WIDTH/2 - quadXSize/2, SPEEDO_HEIGHT/2 - quadYSize/2);
            rootNode.attachChild(g);

            Vector3f innerPoint = new Vector3f(FastMath.cos(angle) * innerArc, FastMath.sin(angle) * innerArc, 0);
            Vector3f outerPoint = new Vector3f(FastMath.cos(angle) * (innerArc + (outerArc - innerArc) / 2), FastMath.sin(angle) * (innerArc + (outerArc - innerArc)/2), 0);
            Geometry line = Geo.makeShapeLine(am, ColorRGBA.White, innerPoint, outerPoint);
            line.setLocalTranslation(SPEEDO_WIDTH / 2, SPEEDO_HEIGHT / 2, -12);
            rootNode.attachChild(line);
        }
        
        
        //nitro 
        Quad q = new Quad(80, 10);
        nitroOff = new Geometry("nitroback", q);
        Material nitroM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        nitroM.setColor("Color", new ColorRGBA(ColorRGBA.Green).mult(0.3f));
        nitroOff.setMaterial(nitroM);
        nitroOff.setLocalTranslation(5, 10, 0);
        rootNode.attachChild(nitroOff);
        
        nitro = new Geometry("nitro", q);
        nitroM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        nitroM.setColor("Color", new ColorRGBA(ColorRGBA.Green));
        nitro.setMaterial(nitroM);
        nitro.setLocalTranslation(5, 10, 0);
        rootNode.attachChild(nitro);
        
        //throttle
        int width = 6;
        int height = 60;
        q = new Quad(width, height);
        throttleOff = new Geometry("throttleback", q);
        Material throttleM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        throttleM.setColor("Color", new ColorRGBA(ColorRGBA.Blue).mult(0.3f));
        throttleOff.setMaterial(throttleM);
        throttleOff.setLocalTranslation(SPEEDO_WIDTH/2 + 30 - width / 2, SPEEDO_HEIGHT/2 - height/2, 0);
        rootNode.attachChild(throttleOff);
        
        throttle = new Geometry("throttle", q);
        throttleM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        throttleM.setColor("Color", new ColorRGBA(ColorRGBA.Blue));
        throttle.setMaterial(throttleM);
        throttle.setLocalTranslation(SPEEDO_WIDTH/2 + 30 - width / 2, SPEEDO_HEIGHT/2 - height/2, 0);
        rootNode.attachChild(throttle);
        
        //brake
        brakeOff = new Geometry("brakeback", q);
        Material brakeM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        brakeM.setColor("Color", new ColorRGBA(ColorRGBA.Red).mult(0.3f));
        brakeOff.setMaterial(brakeM);
        brakeOff.setLocalTranslation(SPEEDO_WIDTH/2 - 30 - width / 2, SPEEDO_HEIGHT/2 - height/2, 0);
        rootNode.attachChild(brakeOff);
        
        brake = new Geometry("brake", q);
        brakeM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        brakeM.setColor("Color", new ColorRGBA(ColorRGBA.Red));
        brake.setMaterial(brakeM);
        brake.setLocalTranslation(SPEEDO_WIDTH/2 - 30 - width / 2, SPEEDO_HEIGHT/2 - height/2, 0);
        rootNode.attachChild(brake);
        
        //steer
        q = new Quad(60, 6);
        steerOff = new Geometry("steerback", q);
        Material steerM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        steerM.setColor("Color", new ColorRGBA(ColorRGBA.White).mult(0.3f));
        steerOff.setMaterial(steerM);
        steerOff.setLocalTranslation(SPEEDO_WIDTH/2 - 60/2, SPEEDO_HEIGHT/2 + 40, 0);
        rootNode.attachChild(steerOff);
        
        width = 6;
        q = new Quad(width, 6);
        steer = new Geometry("steer", q);
        steerM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        steerM.setColor("Color", new ColorRGBA(ColorRGBA.White));
        steer.setMaterial(steerM);
        steer.setLocalTranslation(SPEEDO_WIDTH/2 - width/2, SPEEDO_HEIGHT/2 + 40, 0);
        rootNode.attachChild(steer);
    }

    private void makeKmH(AppSettings settings) {
        //speed scores
        Quad quad = new Quad(30,50);
        int width = SPEEDO_WIDTH - 50;
        
        for (int i = 0; i < speedo.length; i++) {
            speedo[i] = new Geometry("speedo:"+FastMath.pow(10, i), quad);
            speedo[i].setLocalTranslation(width - (32 * i), 8, -1);
            speedo[i].setMaterial(numMats[0]);
            rootNode.attachChild(speedo[i]);
        }

        quad = new Quad(30*1.3f,50*1.3f);
        gear = new Geometry("gear", quad);
        gear.setLocalTranslation(SPEEDO_WIDTH/2-30*1.3f/2, SPEEDO_HEIGHT/2 - 50*1.3f/2, -1);
        gear.setMaterial(numMats[1]);
        rootNode.attachChild(gear);
    }

    private Node addRPMNumber(float angle, int i, Quad quad, float x, float y) {
        Node n = new Node("rpm "+i);
        n.setLocalTranslation(x, y, 0);
        float offset = 35;
        
        Geometry g = new Geometry("speedoNumber "+i, quad);
        if (i > 9) { //multinumber
            g.setLocalTranslation(FastMath.cos(angle)*(radius-offset), FastMath.sin(angle)*(radius-offset), 0);
            g.setMaterial(numMats[i % 10]);
            n.attachChild(g);
            
            Geometry g2 = new Geometry("speedoNumber "+i+"+", quad);
            g2.setLocalTranslation(-20+FastMath.cos(angle)*(radius-offset), FastMath.sin(angle)*(radius-offset), 0);
            g2.setMaterial(numMats[i/10]);
            n.attachChild(g2);
            
        } else { //normal number
            g.setLocalTranslation(FastMath.cos(angle)*(radius-offset), FastMath.sin(angle)*(radius-offset), 0);
            g.setMaterial(numMats[i]);
            n.attachChild(g);
        }
        
        return n;
    }

    public void toggleTelemetry() {
        showTelemetry = !showTelemetry;
        telemetry.setEnabled(showTelemetry);
    }
    
    @Override
    public void update(float tpf) {
        ICarPowered powerState = p.getPoweredState();
        
        int speedKMH = (int)Math.abs(p.getCurrentVehicleSpeedKmHour());
        setSpeedDigits(speedKMH);

        int gearIn = (int) FastMath.clamp(powerState.curGear(), 0, 9); // so we don't go off the end of the texture array
        gear.setMaterial(numMats[gearIn]);
        
        //rpm bar
        rpmMat.setFloat("Threshold", Math.min(1, 1 - (powerState.curRPM()/(float)Math.ceil(redline+1000))*(5/(float)8)));
        
        nitro.setLocalScale(powerState.nitro() / p.getCarData().nitro_max, 1, 1);
        throttle.setLocalScale(1, powerState.accelCurrent(), 1);
        brake.setLocalScale(1, powerState.brakeCurrent(), 1);

        float steeringValue = powerState.steeringCurrent()*60;
        steer.setLocalTranslation(SPEEDO_WIDTH/2 - 6/2 - steeringValue, SPEEDO_HEIGHT/2 + 40, 0); //steering is a translated square
    }

    private void setSpeedDigits(int speedKMH) {
        for (int i = 0; i < speedo.length; i++) {
            speedo[i].setMaterial(numMats[0]);
            if (i > 2) {
                speedo[i].setMaterial(numMatBlank);
            }
        }
        
        for (int i = 0; i < speedo.length; i++) {
            speedo[i].setMaterial(numMats[speedKMH % 10]);
            speedKMH /= 10;
            if (speedKMH < 1)
                break;
        }
    }

    @Override
    protected void cleanup(Application app) {
        InputManager i = app.getInputManager();
        i.deleteMapping("Telemetry");
        i.removeListener(actionListener);
        
        rootNode.removeFromParent();
        getStateManager().detach(telemetry);
    }

    @Override
    protected void onEnable() {}
    @Override
    protected void onDisable() {}
}
