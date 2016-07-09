package game;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
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

import car.MyPhysicsVehicle;
import de.lessvoid.nifty.Nifty;
import de.lessvoid.nifty.screen.Screen;
import de.lessvoid.nifty.screen.ScreenController;

public class DriveMenuState extends AbstractAppState implements ScreenController {

	private Node localRootNode = new Node("Pause Screen RootNode");
	private Node localGuiNode = new Node("Pause Screen GuiNode");
	
	private MyPhysicsVehicle p;
	private boolean showTelemetry;
	private Node telemetry;
	//displays the skid value, TODO make better
	private Geometry gripBox[];
	private Geometry gripDir[];
	private Vector3f[] ps = new Vector3f[] { 
			new Vector3f(60, 250, 0),
			new Vector3f(140, 250, 0),
			new Vector3f(60, 180, 0),
			new Vector3f(140, 180, 0),
		};
	
	//some debug text
	BitmapText statsText;
	
	public DriveMenuState() {
		super();
		AssetManager am = App.rally.getAssetManager();
		
		showTelemetry = true;
		telemetry = new Node("telemetry");
		
		if (showTelemetry) {
			localGuiNode.attachChild(telemetry);
		}
		
		makeTelemetry(am, telemetry);
	}
	
	private void makeTelemetry(AssetManager am, Node n) {
		Material white = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		white.setColor("Color", ColorRGBA.White);
		Material black = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		black.setColor("Color", ColorRGBA.Black);
		
		this.gripBox = new Geometry[4];
		this.gripDir = new Geometry[4];

		Box b = new Box(20, 20, 1);
		Line l = new Line(new Vector3f(0,0,10), new Vector3f(1,0,10));
		l.setLineWidth(3);
		
		for (int i = 0; i < 4; i++) {
			gripBox[i] = new Geometry("box-"+i, b);
			gripBox[i].setLocalTranslation(ps[i]);
			gripBox[i].setMaterial(white);
			n.attachChild(gripBox[i]);

			gripDir[i] = new Geometry("line-"+i, l);
			gripDir[i].setLocalTranslation(ps[i]);
			gripDir[i].setMaterial(black);
			n.attachChild(gripDir[i]);
		}
		
		//stats text 
		BitmapFont guiFont = App.rally.getFont();
		statsText = new BitmapText(guiFont, false);		  
		statsText.setSize(guiFont.getCharSet().getRenderedSize());	  		// font size
		statsText.setColor(ColorRGBA.White);								// font color
		statsText.setText("");												// the text
		statsText.setLocalTranslation(App.rally.getSettings().getWidth()-200, 500, 0); // position
		n.attachChild(statsText);
	}

	private ActionListener actionListener = new ActionListener() {
		public void onAction(String name, boolean keyPressed, float tpf) {
			if (keyPressed) return; 
			if (name.equals("Pause")) {
				togglePause();
			}
			if (name.equals("TabMenu")) {
				toggleMenu();
			}
			if (name.equals("Telemetry")) {
				toggleTelemetry();
			}
		}
	};

	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);

		InputManager i = App.rally.getInputManager();
		
		i.addMapping("Pause", new KeyTrigger(KeyInput.KEY_ESCAPE));
		i.addMapping("TabMenu", new KeyTrigger(KeyInput.KEY_TAB));
		i.addMapping("Telemetry", new KeyTrigger(KeyInput.KEY_HOME));
		
		i.addListener(actionListener, "Pause");
		i.addListener(actionListener, "TabMenu");
		i.addListener(actionListener, "Telemetry");
		
		Rally r = App.rally;
		r.getRootNode().attachChild(localRootNode);
		r.getGuiNode().attachChild(localGuiNode);
	}

	public void togglePause() {
		Screen cur = App.nifty.getCurrentScreen();
		if (cur.getScreenId().equals("drive-paused")) {
			//then un pause
			App.nifty.gotoScreen("drive-noop");
			App.rally.drive.setEnabled(true);
		} else {
			//then pause
			App.nifty.gotoScreen("drive-paused");
			App.rally.drive.setEnabled(false);
		}
	}
	public void toggleMenu() {
		Screen cur = App.nifty.getCurrentScreen();
		if (cur.getScreenId().equals("drive-pause")) return; //can't open the menu on the pause screen
		
		if (cur.getScreenId().equals("drive-tabmenu")) {
			App.nifty.gotoScreen("drive-noop");
		} else {
			App.nifty.gotoScreen("drive-tabmenu");
		}
	}
	
	public void toggleTelemetry() {
		showTelemetry = !showTelemetry;
		if (showTelemetry) {
			localGuiNode.attachChild(telemetry);
		} else {
			localGuiNode.detachChild(telemetry);
		}
	}
	
	public void mainMenu() {
		//call rally.mainmenu() and cleanup()
	}

	public void update(float tpf) {
		super.update(tpf);
		
		if (p == null) p = App.rally.drive.cb.get(0); //yay
		AssetManager am = App.rally.getAssetManager();
		
		// http://forum.projectcarsgame.com/showthread.php?23037-Telemetry-detail&p=892187&viewfull=1#post892187
		if (showTelemetry) {
			//stats
			float speed = p.getLinearVelocity().length();
			statsText.setText("speed:"+speed + "m/s\nRPM:" + p.curRPM +"\na:"+p.accelCurrent+"\nb:"+p.brakeCurrent+
					"\nengine:"+p.engineTorque+"\ntraction:"+p.totalTraction + "\nwheelRot:"+p.totalWheelRot +"\nisDay:"+App.rally.sky.isDay);
			
			//grips
			for (int i = 0 ; i < 4; i++) {
				Material m = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
				m.setColor("Color", new ColorRGBA(p.wheel[i].skid, p.wheel[i].skid, p.wheel[i].skid, 1));
				gripBox[i].setMaterial(m);
				
				Vector3f dir = p.wheel[i].gripF;
				if (dir != null) {
					gripBox[i].setLocalScale(p.wheel[i].susF/(p.car.mass*2));
					
					gripDir[i].setLocalScale(dir.length()*75);
					
					float angle = FastMath.atan2(dir.z, dir.x);
					Quaternion q = new Quaternion();
					q.fromAngleAxis(angle, Vector3f.UNIT_Z);
					gripDir[i].setLocalRotation(q);
				}
			}
		}
	}
	
	@Override
	public void cleanup() {
		Rally r = App.rally;
		r.getRootNode().detachChild(localRootNode);
		r.getGuiNode().detachChild(localGuiNode);
	}

	public void bind(Nifty arg0, Screen arg1) { }
	public void onEndScreen() { }
	public void onStartScreen() { }
}
