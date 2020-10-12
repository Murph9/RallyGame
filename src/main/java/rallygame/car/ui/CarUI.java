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

	//TODO scale it with monitor size and pixel density (forza doesn't deal with this)
    
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
	private static final String numDir = "number/"; //texture location
    private Material[] numMats = new Material[11]; //texture set
    private Material numMatBlank;
	
	private Geometry[] speedo = new Geometry[5]; //speed squares
	private Geometry gear = new Geometry(); //gear label
	
	//speedo numbers
	private static final float startAng = FastMath.PI*5/4;

	private float finalAng = 0;
	private int finalRPM; //should be more than redline
	private float redline;
	
    private int centerx;
    private int centery = 86, radius = 100;
	

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
		AssetManager am = r.getAssetManager();
		
		//////////////////////////////
		//speedo number textures
		for (int i = 0 ; i < 10; i++) {
			numMats[i] = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
			numMats[i].setTexture("ColorMap", am.loadTexture(numDir + i + ".png"));
			numMats[i].getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        }
        //a blank one
        numMatBlank = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
        numMatBlank.setTexture("ColorMap", am.loadTexture(numDir + "blank.png"));
        numMatBlank.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        
        
		makeSpeedo(am, settings);

		/////////////
		//telemetry
		InputManager i = r.getInputManager();
		
		i.addMapping("Telemetry", new KeyTrigger(KeyInput.KEY_HOME));
		i.addListener(actionListener, "Telemetry");
		
		telemetry = new CarUITelemetry(p);
		getStateManager().attach(telemetry);
		telemetry.setEnabled(false);
	}

	private void makeSpeedo(AssetManager am, AppSettings settings) {
		makeKmH(settings);
		
		Material m = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		m.setColor("Color", ColorRGBA.Black);

		///////////////
		//make the variable parts:		
		Quad qback = new Quad(270, 200);
		background = new Geometry("ui-background", qback);
		background.setCullHint(CullHint.Never);
		Material trans = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		trans.setColor("Color", new ColorRGBA(0, 0, 0, 0.5f));
		trans.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
		background.setMaterial(trans);
		background.setLocalTranslation(settings.getWidth() - 270, 0, -10);
		
		rootNode.attachChild(background);
		
		//rpm bars 2
		Quad rpmQ = new Quad(220, 220);
		rpmQuad = new Geometry("ui-background", rpmQ);
		background.setCullHint(CullHint.Never);
		rpmMat = new Material(am, "MatDefs/Radial.j3md");
		rpmMat.setTransparent(true);
		rpmMat.setTexture("ThresholdMap", am.loadTexture("image/radialgradient_large.png"));
		rpmMat.setFloat("Threshold", 0);
		rpmMat.setColor("Color", ColorRGBA.White);
		rpmMat.getAdditionalRenderState().setBlendMode(BlendMode.Additive);
		rpmQuad.setMaterial(rpmMat);
		rpmQuad.setLocalTranslation(settings.getWidth() - 240, -25, -10);
		rootNode.attachChild(rpmQuad);
		
		//rpm bars
		Quad quad = new Quad(20, 20);
		
		centerx = settings.getWidth() - 127;
        
        final int increment = 100;
		for (int i = 0; i < finalRPM+1; i += increment) {
			float angle = FastMath.interpolateLinear(i/(float)finalRPM, startAng, finalAng);
			
			if (i >= redline) {
				float angle2 = FastMath.interpolateLinear((i + increment) / (float) finalRPM, startAng, finalAng);
				
                Vector3f[] corners = new Vector3f[] {
                        new Vector3f(FastMath.cos(angle) * radius, FastMath.sin(angle) * radius, 0),
                        new Vector3f(FastMath.cos(angle) * radius * 0.89f, FastMath.sin(angle) * radius * 0.89f, 0),
                        new Vector3f(FastMath.cos(angle2) * radius, FastMath.sin(angle2) * radius, 0),
                        new Vector3f(FastMath.cos(angle2) * radius * 0.89f, FastMath.sin(angle2) * radius * 0.89f, 0),
					};
				Mesh mq = Geo.createQuad(corners);

                Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
                mat.setColor("Color", new ColorRGBA(ColorRGBA.Red));
                Geometry redLine2 = new Geometry("redline2", mq);
                redLine2.setMaterial(mat);
                redLine2.setLocalTranslation(centerx, centery, 0);
                rootNode.attachChild(redLine2);
            } 
            
            if (i % 1000 == 0) {
                Node g = addRPMNumber(angle, (int)i/1000, quad, centerx-10, centery-10);
                rootNode.attachChild(g);
            }
		}
		
		
		//nitro 
		Quad q = new Quad(10, 80);
		nitroOff = new Geometry("nitroback", q);
		Material nitroM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		nitroM.setColor("Color", new ColorRGBA(ColorRGBA.Green).mult(0.2f));
		nitroOff.setMaterial(nitroM);
		nitroOff.setLocalTranslation(centerx - 120, 10, 0);
		rootNode.attachChild(nitroOff);
		
		nitro = new Geometry("nitro", q);
		nitroM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		nitroM.setColor("Color", new ColorRGBA(ColorRGBA.Green));
		nitro.setMaterial(nitroM);
		nitro.setLocalTranslation(centerx - 120, 10, 0);
		rootNode.attachChild(nitro);
		
		//throttle
		q = new Quad(6, 60);
		throttleOff = new Geometry("throttleback", q);
		Material throttleM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		throttleM.setColor("Color", new ColorRGBA(ColorRGBA.Blue).mult(0.2f));
		throttleOff.setMaterial(throttleM);
		throttleOff.setLocalTranslation(centerx - 30, centery - 30, 0);
		rootNode.attachChild(throttleOff);
		
		throttle = new Geometry("throttle", q);
		throttleM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		throttleM.setColor("Color", new ColorRGBA(ColorRGBA.Blue));
		throttle.setMaterial(throttleM);
		throttle.setLocalTranslation(centerx - 30, centery - 30, 0);
		rootNode.attachChild(throttle);
		
		//brake
		brakeOff = new Geometry("brakeback", q);
		Material brakeM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		brakeM.setColor("Color", new ColorRGBA(ColorRGBA.Red).mult(0.2f));
		brakeOff.setMaterial(brakeM);
		brakeOff.setLocalTranslation(centerx - 45, centery - 30, 0);
		rootNode.attachChild(brakeOff);
		
		brake = new Geometry("brake", q);
		brakeM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		brakeM.setColor("Color", new ColorRGBA(ColorRGBA.Red));
		brake.setMaterial(brakeM);
		brake.setLocalTranslation(centerx - 45, centery - 30, 0);
		rootNode.attachChild(brake);
		
		//steer
		q = new Quad(60, 6);
		steerOff = new Geometry("steerback", q);
		Material steerM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		steerM.setColor("Color", new ColorRGBA(ColorRGBA.White).mult(0.2f));
		steerOff.setMaterial(steerM);
		steerOff.setLocalTranslation(centerx - 35, centery + 40, 0);
		rootNode.attachChild(steerOff);
		
		int width = 6;
		q = new Quad(width, 6);
		steer = new Geometry("steer", q);
		steerM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		steerM.setColor("Color", new ColorRGBA(ColorRGBA.White));
		steer.setMaterial(steerM);
		steer.setLocalTranslation(centerx - 35 + (60-width)/2, centery + 40, 0);
		rootNode.attachChild(steer);
	}

	private void makeKmH(AppSettings settings) {
		//speed scores
		Quad quad = new Quad(30,50);
		int width = settings.getWidth()-70;
        
        for (int i = 0; i < speedo.length; i++) {
            speedo[i] = new Geometry("speedo:"+FastMath.pow(10, i), quad);
            speedo[i].setLocalTranslation(width - (32 * i), 8, -1);
            speedo[i].setMaterial(numMats[0]);
            rootNode.attachChild(speedo[i]);
        }

		quad = new Quad(35,55);
		gear = new Geometry("gear", quad);
		gear.setLocalTranslation(width-50, 68, -1);
		gear.setMaterial(numMats[1]);
		rootNode.attachChild(gear);
	}

	private Node addRPMNumber(float angle, int i, Quad quad, float x, float y) {
		Node n = new Node("rpm "+i);
		n.setLocalTranslation(x, y, 0);
		float offset = 25;
		
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

		var gearIn = (int) FastMath.clamp(powerState.curGear(), 0, 9); // so we don't go off the end of the texture array
		gear.setMaterial(numMats[gearIn]);
		
		//rpm bar 2
		rpmMat.setFloat("Threshold", Math.min(1, 1 - (powerState.curRPM()/(float)Math.ceil(redline+1000))*(5/(float)8)));
		
		nitro.setLocalScale(1, powerState.nitro()/p.getCarData().nitro_max, 1);
		throttle.setLocalScale(1, powerState.accelCurrent(), 1);
		brake.setLocalScale(1, powerState.brakeCurrent(), 1);
		steer.setLocalTranslation(centerx - 35 + (powerState.steeringCurrent()*-1 + 0.5f)*60 - 6/2, centery + 40, 0); //steering is a translated square
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
