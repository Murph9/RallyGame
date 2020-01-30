package car;

import java.text.DecimalFormat;

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
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Line;
import com.jme3.scene.shape.Quad;

import car.data.CarDataConst;
import car.ray.RayCarControl;
import car.ray.RayWheel;
import helper.Colours;
import helper.H;
import service.averager.AverageV3f;
import service.averager.IAverager;
import service.averager.IAverager.Type;

public class CarUITelemetry extends BaseAppState {

	private RayCarControl p;
	
	private Node rootNode;

	private final WheelUI[] w = new WheelUI[4];
	private static final DecimalFormat Force_Format = new DecimalFormat("00000");
		
	//the g force meter circles
	private Geometry g1;
	private Geometry g2;
	private Vector3f gcenter;
	private BitmapText gText;
	private final IAverager<Vector3f> gAverage;
	
	//some debug text
	private BitmapText statsText;
	
	public CarUITelemetry(RayCarControl p) {
		this.rootNode = new Node("telemetry");
		this.p = p;

		this.gAverage = new AverageV3f(10, Type.Simple);
	}
	
	@Override
	public void initialize(Application app) {
		//set the positions of the wheel grid
		int height = app.getCamera().getHeight();
		w[0] = new WheelUI(new Vector3f(80, height*0.9f, 0));
		w[1] = new WheelUI(new Vector3f(200, height*0.9f, 0));
		w[2] = new WheelUI(new Vector3f(80, height*0.75f, 0));
		w[3] = new WheelUI(new Vector3f(200, height*0.75f, 0));
		
		makeTelemetry(app.getAssetManager(), app.getCamera());
	}
	
	private void makeTelemetry(AssetManager am, Camera c) {
		Material white = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		white.setColor("Color", ColorRGBA.White);
		white.getAdditionalRenderState().setLineWidth(3);
		Material black = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		black.setColor("Color", ColorRGBA.Black);
		black.getAdditionalRenderState().setLineWidth(3);
		
		BitmapFont guiFont = am.loadFont("Interface/Fonts/Default.fnt");
		
		Box b = new Box(20, 20, 1);
		Line l = new Line(new Vector3f(0,0,10), new Vector3f(1,0,10));
		
		for (int i = 0; i < 4; i++) {
			WheelUI w = this.w[i];
			
			w.gripBox = new Geometry("box-"+i, b);
			w.gripBox.setLocalTranslation(w.ps);
			w.gripBox.setMaterial(white);
			rootNode.attachChild(w.gripBox);

			w.gripDir = new Geometry("line-"+i, l);
			w.gripDir.setLocalTranslation(w.ps);
			w.gripDir.setMaterial(black);
			rootNode.attachChild(w.gripDir);
			
			w.gripValue = new BitmapText(guiFont, false);
			w.gripValue.setSize(guiFont.getCharSet().getRenderedSize());
			w.gripValue.setColor(ColorRGBA.Black);
			w.gripValue.setText("");
			w.gripValue.setLocalTranslation(w.ps.add(new Vector3f(-80, 0 ,0)));
			rootNode.attachChild(w.gripValue);
			
			w.wheelRot = new BitmapText(guiFont, false);
			w.wheelRot.setSize(guiFont.getCharSet().getRenderedSize());
			w.wheelRot.setColor(ColorRGBA.DarkGray);
			w.wheelRot.setText("");
			w.wheelRot.setLocalTranslation(w.ps.add(new Vector3f(-80,20,0)));
			rootNode.attachChild(w.wheelRot);
			
			w.engineTorque = new BitmapText(guiFont, false);
			w.engineTorque.setSize(guiFont.getCharSet().getRenderedSize());
			w.engineTorque.setColor(ColorRGBA.Magenta);
			w.engineTorque.setText("");
			w.engineTorque.setLocalTranslation(w.ps.add(new Vector3f(-80,-20,0)));
			rootNode.attachChild(w.engineTorque);
			
			Quad q = new Quad(5, 60);
			w.susOff = new Geometry("susOff", q);
			Material susM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
			susM.setColor("Color", new ColorRGBA(ColorRGBA.White).mult(0.2f));
			w.susOff.setMaterial(susM);
			w.susOff.rotate(0, 0, FastMath.PI);
			w.susOff.setLocalTranslation(w.ps.add(new Vector3f(30,30,0)));
			rootNode.attachChild(w.susOff);
			
			w.sus = new Geometry("sus", q);
			susM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
			susM.setColor("Color", new ColorRGBA(ColorRGBA.White));
			w.sus.setMaterial(susM);
			w.sus.rotate(0, 0, FastMath.PI);
			w.sus.setLocalTranslation(w.ps.add(new Vector3f(30,30,0)));
			rootNode.attachChild(w.sus);
		}
		
		//stats text 
		statsText = new BitmapText(guiFont, false);		  
		statsText.setSize(guiFont.getCharSet().getRenderedSize());	  		// font size
		statsText.setColor(ColorRGBA.White);								// font color
		statsText.setText("");												// the text
		statsText.setLocalTranslation(200, 150, 0); // position
		rootNode.attachChild(statsText);
		
		//g force
		gcenter = new Vector3f(100, c.getHeight()*0.5f, 0);
		
		b = new Box(5, 5, 1);
		g1 = new Geometry("g-circle1", b);
		g1.setLocalTranslation(gcenter);
		g1.setMaterial(white);
		rootNode.attachChild(g1);
		
		g2 = new Geometry("g-circle2", b);
		g2.setLocalTranslation(gcenter);
		g2.setMaterial(white);
		rootNode.attachChild(g2);
		
		gText = new BitmapText(guiFont, false);
		gText.setSize(guiFont.getCharSet().getRenderedSize());
		gText.setColor(ColorRGBA.Black);
		gText.setText("...");
		gText.setLocalTranslation(gcenter.subtract(40, 5, 0));
		rootNode.attachChild(gText);
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
		if (!isEnabled())
			return; //don't do any expensive updates pls
		
		//stats
		statsText.setText(p.statsString());
		
		//grips
		CarDataConst data = p.getCarData();
		for (int i = 0 ; i < 4; i++) {
			WheelUI w = this.w[i];
			RayWheel wheel = p.getWheel(i).getRayWheel();
			
			Material m = new Material(getApplication().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
			w.gripValue.setText(String.format("%.2f slip", wheel.skidFraction));
			w.wheelRot.setText(String.format("%.2f rad/s", wheel.radSec));
			w.engineTorque.setText(Force_Format.format(Math.abs(p.getWheelTorque(i)))+" Nm");
			m.setColor("Color", getGripBoxColour(wheel.skidFraction));
			w.gripBox.setMaterial(m);
			
			Vector3f dir = wheel.gripDir;
			if (dir != null) {
				w.gripBox.setLocalScale(wheel.susForce/(p.getCarData().mass*2));
				w.gripDir.setLocalScale(wheel.susForce/p.getCarData().susByWheelNum(i).max_force*1000);
				
				float angle = FastMath.atan2(dir.z, dir.x);
				Quaternion q = new Quaternion();
				q.fromAngleAxis(angle, Vector3f.UNIT_Z);
				w.gripDir.setLocalRotation(q);
			}
			
			w.sus.setLocalScale(1, wheel.susRayLength/data.susByWheelNum(i).travelTotal(), 1);
		}
		
		//g forces
		//needs to be translated from local into screen axis
		Vector3f gs = p.planarGForce.mult(1/p.getPhysicsObject().getGravity().length());
		gs.y = gs.z; //z is front back
		gs.z = 0; //screen has no depth
		gs = gAverage.get(gs);
		g2.setLocalTranslation(gcenter.add(gs.mult(25))); //because screen pixels
		gText.setText("x: " + H.roundDecimal(gs.x, 2) +", y: " + H.roundDecimal(gs.y, 2));
	}

	private ColorRGBA getGripBoxColour(float value) {
		return Colours.getOnRGBScale(value/3f);
	}

	@Override
	public void cleanup(Application app) {
		((SimpleApplication)app).getGuiNode().detachChild(rootNode);
	}
	
	class WheelUI {
		public final Vector3f ps;
		public Geometry gripBox;
		public Geometry gripDir;
		public BitmapText gripValue;
		public BitmapText wheelRot;
		public BitmapText engineTorque;
		
		public Geometry sus; //value
		public Geometry susOff; //background color
		
		public WheelUI(Vector3f pos) {
			this.ps = pos;
		}
	}
}
