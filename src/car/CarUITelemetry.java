package car;

import com.jme3.app.state.AbstractAppState;
import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Line;

import car.ray.RayCarControl;
import car.ray.RayWheel;
import game.App;
import game.Main;
import helper.H;

public class CarUITelemetry extends AbstractAppState {

	private RayCarControl p;
	
	private Node rootNode;

	private final Vector3f[] ps = new Vector3f[4];
	private Geometry gripBox[];
	private Geometry gripDir[];
	private BitmapText gripValue[];
	private BitmapText wheelRot[];
	
	
	//the g force meter circles
	private Geometry g1;
	private Geometry g2;
	private Vector3f gcenter;
	private BitmapText gText;
	
	//some debug text
	private BitmapText statsText;
	
	private ActionListener actionListener = new ActionListener() {
		public void onAction(String name, boolean keyPressed, float tpf) {
			//TODO add any key action to listen to?
		}
	};
	
	public CarUITelemetry(RayCarControl p) {
		Main r = App.rally;
		this.p = p;
		
		InputManager i = App.rally.getInputManager();
		
		i.addMapping("Telemetry", new KeyTrigger(KeyInput.KEY_HOME));
		i.addListener(actionListener, "Telemetry");
		
		rootNode = new Node("telemetry");

		//set the positions of the wheel grid
		int height = App.rally.getCamera().getHeight();
		ps[0] = new Vector3f(80, height*0.9f, 0);
		ps[1] = new Vector3f(200, height*0.9f, 0);
		ps[2] = new Vector3f(80, height*0.75f, 0);
		ps[3] = new Vector3f(200, height*0.75f, 0);
		
		makeTelemetry(r.getAssetManager());
	}
	
	private void makeTelemetry(AssetManager am) {
		Material white = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		white.setColor("Color", ColorRGBA.White);
		white.getAdditionalRenderState().setLineWidth(3);
		Material black = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		black.setColor("Color", ColorRGBA.Black);
		black.getAdditionalRenderState().setLineWidth(3);
		
		BitmapFont guiFont = App.rally.getFont();
		
		this.gripBox = new Geometry[4];
		this.gripDir = new Geometry[4];
		this.gripValue = new BitmapText[4];
		this.wheelRot = new BitmapText[4];
		

		Box b = new Box(20, 20, 1);
		Line l = new Line(new Vector3f(0,0,10), new Vector3f(1,0,10));
		
		for (int i = 0; i < 4; i++) {
			gripBox[i] = new Geometry("box-"+i, b);
			gripBox[i].setLocalTranslation(ps[i]);
			gripBox[i].setMaterial(white);
			rootNode.attachChild(gripBox[i]);

			gripDir[i] = new Geometry("line-"+i, l);
			gripDir[i].setLocalTranslation(ps[i]);
			gripDir[i].setMaterial(black);
			rootNode.attachChild(gripDir[i]);
			
			gripValue[i] = new BitmapText(guiFont, false);
			gripValue[i].setSize(guiFont.getCharSet().getRenderedSize());
			gripValue[i].setColor(ColorRGBA.Black);
			gripValue[i].setText("");
			gripValue[i].setLocalTranslation(ps[i].add(new Vector3f(-80, 0 ,0)));
			rootNode.attachChild(gripValue[i]);
			
			wheelRot[i] = new BitmapText(guiFont, false);
			wheelRot[i].setSize(guiFont.getCharSet().getRenderedSize());
			wheelRot[i].setColor(ColorRGBA.DarkGray);
			wheelRot[i].setText("");
			wheelRot[i].setLocalTranslation(ps[i].add(new Vector3f(-80,20,0)));
			rootNode.attachChild(wheelRot[i]);
		}
		
		//stats text 
		statsText = new BitmapText(guiFont, false);		  
		statsText.setSize(guiFont.getCharSet().getRenderedSize());	  		// font size
		statsText.setColor(ColorRGBA.White);								// font color
		statsText.setText("");												// the text
		statsText.setLocalTranslation(200, 150, 0); // position
		rootNode.attachChild(statsText);
		
		//g force
		gcenter = new Vector3f(100, App.rally.getCamera().getHeight()*0.5f, 0);
		
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
		gText.setSize(guiFont.getCharSet().getRenderedSize());	  		// font size
		gText.setColor(ColorRGBA.Black);								// font color
		gText.setText("...");												// the text
		gText.setLocalTranslation(gcenter.subtract(40, 5, 0)); 			// position
		rootNode.attachChild(gText);
	}

	
	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		if (enabled) {
			App.rally.getGuiNode().attachChild(rootNode);			
		} else {
			App.rally.getGuiNode().detachChild(rootNode);			
		}
	}
	
	@Override
	public void update(float tpf) {
		if (!isEnabled())
			return; //don't do any expensive updates pls
		
		//stats
		statsText.setText(p.statsString());
		
		//grips
		for (int i = 0 ; i < 4; i++) {
			RayWheel wheel = p.getWheel(i).getRayWheel();
			
			Material m = new Material(App.rally.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
			gripValue[i].setText(String.format("%.2f", wheel.skidFraction));
			wheelRot[i].setText(String.format("%.2f", wheel.radSec));
			m.setColor("Color", getGripBoxColour(wheel.skidFraction));
			gripBox[i].setMaterial(m);
			
			Vector3f dir = wheel.gripDir;
			if (dir != null) {
				gripBox[i].setLocalScale(wheel.susForce/(p.getCarData().mass*2));
				
				gripDir[i].setLocalScale(wheel.susForce/p.getCarData().susByWheelNum(i).max_force*150);
				
				float angle = FastMath.atan2(dir.z, dir.x);
				Quaternion q = new Quaternion();
				q.fromAngleAxis(angle, Vector3f.UNIT_Z);
				gripDir[i].setLocalRotation(q);
			}
		}
		
		//g forces
		//needs to be translated from local into screen axis
		Vector3f gs = p.planarGForce.mult(1/p.getPhysicsObject().getGravity().length());
		gs.y = gs.z; //z is front back
		gs.z = 0; //screen has no depth
		g2.setLocalTranslation(gcenter.add(gs.mult(25))); //because screen pixels
		gText.setText("x: " + H.roundDecimal(gs.x, 2) +", y: " + H.roundDecimal(gs.y, 2));
		
	}
	
	private ColorRGBA getGripBoxColour(float value) {
		//0 is white, 1 is green, 2 is red, 5 is blue
		value = Math.abs(value);
		if (value < 1)
			return H.lerpColor(value, ColorRGBA.White, ColorRGBA.Green);
		else if (value < 2)
			return H.lerpColor((value - 1f), ColorRGBA.Green, ColorRGBA.Red);
		else if (value < 5.0)
			return H.lerpColor((value - 2f)/(3f), ColorRGBA.Red, ColorRGBA.Blue);
		
		return ColorRGBA.Blue;
	}

	public void cleanup() {
		InputManager i = App.rally.getInputManager();
		i.deleteMapping("Telemetry");
		i.removeListener(actionListener);
		
		App.rally.getGuiNode().detachChild(rootNode);
	}
}
