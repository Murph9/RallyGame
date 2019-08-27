package car;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
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
import com.jme3.scene.Node;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Line;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;

import car.ray.RayCarControl;
import car.ray.ICarPowered;
import game.App;
import helper.H;

public class CarUI extends BaseAppState {

	//TODO scale it with monitor size (forza doesn't deal with this)
	
	private final RayCarControl p;
	private Node rootNode;
	
	//hud stuff
	Geometry background;
	BitmapText angle;
	
	//rpm
	Geometry rpmQuad;
	Material rpmMat;

	//other meters
	Geometry nitro, nitroOff; //quads that display nitro
	Geometry throttle, throttleOff; //quads that display throttle 
	Geometry brake, brakeOff; //quads that display braking
	Geometry steer, steerOff; //quads that display turn value
	
	//texture
	static final String numDir = "assets/number/"; //texture location
	Material[] numMats = new Material[10]; //texture set
	
	Geometry[] speedo = new Geometry[3]; //speed squares
	Geometry gear = new Geometry(); //gear label
	
	//speedo numbers
	float startAng = FastMath.PI*5/4;
	int startRPM = 0; //because sometimes it might not be

	float finalAng = 0;
	int finalRPM; //should be more than redline
	float redline;
	
	private int centerx, centery = 86, radius = 100;
	

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
		
		this.redline = p.getCarData().e_redline;
		this.finalRPM = (int)FastMath.ceil(this.redline) + 1000;
	}
	@Override
	protected void initialize(Application app) {
		App r = (App)app;

		BitmapFont guiFont = app.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
		AppSettings settings = app.getContext().getSettings();
		rootNode = new Node("local root");
		r.getGuiNode().attachChild(rootNode);
		AssetManager am = r.getAssetManager();

		angle = new BitmapText(guiFont, false);		  
		angle.setSize(guiFont.getCharSet().getRenderedSize());
		angle.setColor(ColorRGBA.White);
		angle.setText("blaj");
		angle.setLocalTranslation(settings.getWidth() - 300, 30, 0); // position
		rootNode.attachChild(angle);
		
		//////////////////////////////
		//speedo number textures
		for (int i = 0 ; i < 10; i++) {
			numMats[i] = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
			numMats[i].setTexture("ColorMap", am.loadTexture(numDir + i + ".png"));
			numMats[i].getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
		}
		
		makeSpeedo(am, settings);

		/////////////
		//telemetry
		InputManager i = r.getInputManager();
		
		i.addMapping("Telemetry", new KeyTrigger(KeyInput.KEY_HOME));
		i.addListener(actionListener, "Telemetry");
		
		telemetry = new CarUITelemetry(p);
		app.getStateManager().attach(telemetry);		
	}

	private void makeSpeedo(AssetManager am, AppSettings settings) {
		makeKmH(settings);
		
		Material m = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		m.setColor("Color", ColorRGBA.Black);

		///////////////
		//make the variable parts:
		Node speedoNode = new Node("Speedo");
		rootNode.attachChild(speedoNode);
		
		Quad qback = new Quad(270, 200);
		background = new Geometry("ui-background", qback);
		background.setCullHint(CullHint.Never);
		Material trans = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		trans.setColor("Color", new ColorRGBA(0, 0, 0, 0.5f));
		trans.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
		background.setMaterial(trans);
		background.setLocalTranslation(settings.getWidth() - 270, 0, -10);
		
		speedoNode.attachChild(background);
		
		//rpm bars 2
		Quad rpmQ = new Quad(220, 220);
		rpmQuad = new Geometry("ui-background", rpmQ);
		background.setCullHint(CullHint.Never);
		rpmMat = new Material(am, "assets/mat/Radial.j3md");
		rpmMat.setTransparent(true);
		rpmMat.setTexture("ThresholdMap", am.loadTexture("assets/image/radialgradient_large.png"));
		rpmMat.setFloat("Threshold", FastMath.nextRandomFloat());
		rpmMat.setColor("Color", ColorRGBA.White);
		rpmMat.getAdditionalRenderState().setBlendMode(BlendMode.Additive);
		rpmQuad.setMaterial(rpmMat);
		rpmQuad.setLocalTranslation(settings.getWidth() - 240, -25, -10);
		speedoNode.attachChild(rpmQuad);
		
		//rpm bars
		Quad quad = new Quad(20, 20);
		
		centerx = settings.getWidth() - 127;
		
		for (int i = 0; i < finalRPM+1; i += 1000) {
			float angle = FastMath.interpolateLinear(i/(float)finalRPM, startAng, finalAng);
			
			if (i >= redline) { //TODO actually show red..
				//Log.p(new Vector3f(FastMath.cos(angle)*radius, FastMath.sin(angle)*radius, 1));
				Line l = new Line(new Vector3f(FastMath.cos(angle)*radius, FastMath.sin(angle)*radius, 1)
						, new Vector3f(FastMath.cos(angle)*radius*0.9f, FastMath.sin(angle)*radius*0.9f, -1));
				Geometry redLine = new Geometry("redline", l);
				Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
				mat.setColor("Color", new ColorRGBA(ColorRGBA.Red));
				mat.getAdditionalRenderState().setLineWidth(4);
				redLine.setMaterial(mat);
	            redLine.setLocalTranslation(centerx, centery, -1);//behind other things
				speedoNode.attachChild(redLine);
			}
			Node g = addRPMNumber(angle, (int)i/1000, quad, centerx-10, centery-10);
			speedoNode.attachChild(g);
		}
		
		
		//nitro 
		Quad q = new Quad(10, 80);
		nitroOff = new Geometry("nitroback", q);
		Material nitroM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		nitroM.setColor("Color", new ColorRGBA(ColorRGBA.Green).mult(0.2f));
		nitroOff.setMaterial(nitroM);
		nitroOff.setLocalTranslation(centerx - (settings.getWidth() - centerx), 10, 0);
		speedoNode.attachChild(nitroOff);
		
		nitro = new Geometry("nitro", q);
		nitroM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		nitroM.setColor("Color", new ColorRGBA(ColorRGBA.Green));
		nitro.setMaterial(nitroM);
		nitro.setLocalTranslation(centerx - (settings.getWidth() - centerx), 10, 0);
		speedoNode.attachChild(nitro);
		
		//throttle
		q = new Quad(6, 60);
		throttleOff = new Geometry("throttleback", q);
		Material throttleM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		throttleM.setColor("Color", new ColorRGBA(ColorRGBA.Blue).mult(0.2f));
		throttleOff.setMaterial(throttleM);
		throttleOff.setLocalTranslation(centerx - 30, centery - 30, 0);
		speedoNode.attachChild(throttleOff);
		
		throttle = new Geometry("throttle", q);
		throttleM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		throttleM.setColor("Color", new ColorRGBA(ColorRGBA.Blue));
		throttle.setMaterial(throttleM);
		throttle.setLocalTranslation(centerx - 30, centery - 30, 0);
		speedoNode.attachChild(throttle);
		
		//brake
		brakeOff = new Geometry("brakeback", q);
		Material brakeM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		brakeM.setColor("Color", new ColorRGBA(ColorRGBA.Red).mult(0.2f));
		brakeOff.setMaterial(brakeM);
		brakeOff.setLocalTranslation(centerx - 45, centery - 30, 0);
		speedoNode.attachChild(brakeOff);
		
		brake = new Geometry("brake", q);
		brakeM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		brakeM.setColor("Color", new ColorRGBA(ColorRGBA.Red));
		brake.setMaterial(brakeM);
		brake.setLocalTranslation(centerx - 45, centery - 30, 0);
		speedoNode.attachChild(brake);
		
		//steer
		q = new Quad(60, 6);
		steerOff = new Geometry("steerback", q);
		Material steerM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		steerM.setColor("Color", new ColorRGBA(ColorRGBA.White).mult(0.2f));
		steerOff.setMaterial(steerM);
		steerOff.setLocalTranslation(centerx - 35, centery + 40, 0);
		speedoNode.attachChild(steerOff);
		
		int width = 6;
		q = new Quad(width, 6);
		steer = new Geometry("steer", q);
		steerM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		steerM.setColor("Color", new ColorRGBA(ColorRGBA.White));
		steer.setMaterial(steerM);
		steer.setLocalTranslation(centerx - 35 + (60-width)/2, centery + 40, 0);
		speedoNode.attachChild(steer);
	}

	private void makeKmH(AppSettings settings) {
		//speed scores
		Quad quad = new Quad(30,50);
		int width = settings.getWidth()-70;
		
		speedo[0] = new Geometry("ones", quad);
		speedo[0].setLocalTranslation(width, 8, -1);
		speedo[0].setMaterial(numMats[0]);
		rootNode.attachChild(speedo[0]);
		
		speedo[1] = new Geometry("tens", quad);
		speedo[1].setLocalTranslation(width-32, 8, -1);
		speedo[1].setMaterial(numMats[0]);
		rootNode.attachChild(speedo[1]);
		
		speedo[2] = new Geometry("hundereds", quad);
		speedo[2].setLocalTranslation(width-64, 8, -1);
		speedo[2].setMaterial(numMats[0]);
		rootNode.attachChild(speedo[2]);
		
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
	
	//main update method
	public void update(float tpf) {
		ICarPowered powerState = p.getPoweredState();
		
		int speedKMH = (int)Math.abs(p.getCurrentVehicleSpeedKmHour());
		setSpeedDigits(speedKMH);

		setGearDigit(powerState.curGear());
		
		//rpm bar 2
		rpmMat.setFloat("Threshold", Math.min(1, 1 - (powerState.curRPM()/(float)Math.ceil(redline+1000))*(5/(float)8)));
		
		angle.setText(H.roundDecimal(powerState.driftAngle(), 1)+"'");
		nitro.setLocalScale(1, powerState.nitro()/p.getCarData().nitro_max, 1);
		throttle.setLocalScale(1, powerState.accelCurrent(), 1);
		brake.setLocalScale(1, powerState.brakeCurrent(), 1);
		steer.setLocalTranslation(centerx - 35 + (powerState.steeringCurrent()*-1 + 0.5f)*60 - 6/2, centery + 40, 0); //steering is a translated square
	}

	private void setSpeedDigits(int speedKMH) {
		int count = 0;
		while (count < 3) {
			speedo[count].setMaterial(numMats[0]);
			count++;
		}
		
		count = 0;
		speedKMH %= 1000; //not more than 999
		while (speedKMH > 0) {
			speedo[count].setMaterial(numMats[speedKMH % 10]);
			speedKMH /= 10;
			count++;
		}
	}
	
	private void setGearDigit(int gearIn) {
		gearIn = (int)FastMath.clamp(gearIn, 0, 9); //so we don't go off the end of the texture array
		gear.setMaterial(numMats[gearIn]);
	}

	@Override
	protected void cleanup(Application app) {
		InputManager i = app.getInputManager();
		i.deleteMapping("Telemetry");
		i.removeListener(actionListener);
		
		((SimpleApplication)app).getGuiNode().detachChild(rootNode);
		((SimpleApplication)app).getStateManager().detach(telemetry);
	}

	@Override
	protected void onEnable() {}
	@Override
	protected void onDisable() {}
}
