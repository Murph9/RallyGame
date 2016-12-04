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
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import car.MyPhysicsVehicle;

public class DriveMenu extends AbstractAppState {

	private Node localRootNode = new Node("Pause Screen RootNode");
	private Node localGuiNode = new Node("Pause Screen GuiNode");
	
	private MyPhysicsVehicle p;
	private boolean showTelemetry;
	private Node telemetry;

	//displays the skid value, TODO make better
	private Geometry gripBox[];
	private Geometry gripDir[];
	private BitmapText gripValue[];
	private Vector3f[] ps;
	
	//the g force meter circles
	private Geometry g1;
	private Geometry g2;
	private Vector3f gcenter;
	BitmapText gText;
	
	//some debug text
	BitmapText statsText;
	
	//GUI objects
	Container pauseMenu;
	Container infoHint;
	Container info;
	
	public DriveMenu() {
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
		white.getAdditionalRenderState().setLineWidth(3);
		Material black = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		black.setColor("Color", ColorRGBA.Black);
		black.getAdditionalRenderState().setLineWidth(3);
		
		BitmapFont guiFont = App.rally.getFont();


		int height = App.rally.getCamera().getHeight();
		ps = new Vector3f[] {
				new Vector3f(60, height*0.4f, 0), //60,250,0
				new Vector3f(140, height*0.4f, 0),
				new Vector3f(60, height*0.3f, 0),
				new Vector3f(140, height*0.3f, 0), //140,180,0
			};
		
		this.gripBox = new Geometry[4];
		this.gripDir = new Geometry[4];
		this.gripValue = new BitmapText[4];

		Box b = new Box(20, 20, 1);
		Line l = new Line(new Vector3f(0,0,10), new Vector3f(1,0,10));
		
		for (int i = 0; i < 4; i++) {
			gripBox[i] = new Geometry("box-"+i, b);
			gripBox[i].setLocalTranslation(ps[i]);
			gripBox[i].setMaterial(white);
			n.attachChild(gripBox[i]);

			gripDir[i] = new Geometry("line-"+i, l);
			gripDir[i].setLocalTranslation(ps[i]);
			gripDir[i].setMaterial(black);
			n.attachChild(gripDir[i]);
			
			gripValue[i] = new BitmapText(guiFont, false);
			gripValue[i].setSize(guiFont.getCharSet().getRenderedSize());
			gripValue[i].setColor(ColorRGBA.Black);
			gripValue[i].setText("");
			gripValue[i].setLocalTranslation(ps[i]);
			n.attachChild(gripValue[i]);
		}
		
		//stats text 
		statsText = new BitmapText(guiFont, false);		  
		statsText.setSize(guiFont.getCharSet().getRenderedSize());	  		// font size
		statsText.setColor(ColorRGBA.White);								// font color
		statsText.setText("");												// the text
		statsText.setLocalTranslation(App.rally.getSettings().getWidth()-200, 500, 0); // position
		n.attachChild(statsText);
		
		//g force
		gcenter = new Vector3f(100, height*0.5f, 0);
		
		b = new Box(5, 5, 1);
		g1 = new Geometry("g-circle1", b);
		g1.setLocalTranslation(gcenter);
		g1.setMaterial(white);
		n.attachChild(g1);
		
		g2 = new Geometry("g-circle2", b);
		g2.setLocalTranslation(gcenter);
		g2.setMaterial(white);
		n.attachChild(g2);
		
		gText = new BitmapText(guiFont, false);
		gText.setSize(guiFont.getCharSet().getRenderedSize());	  		// font size
		gText.setColor(ColorRGBA.Black);								// font color
		gText.setText("...");												// the text
		gText.setLocalTranslation(gcenter.subtract(40, 5, 0)); 			// position
		n.attachChild(gText);
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

	@SuppressWarnings("unchecked")
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
		
		//init gui
		pauseMenu = new Container();
		Button button = pauseMenu.addChild(new Button("UnPause"));
		button.addClickCommands(new Command<Button>() {
            @Override
            public void execute( Button source ) {
            	togglePause();
            }
        });
		
		Button button2 = pauseMenu.addChild(new Button("MainMenu"));
		button2.addClickCommands(new Command<Button>() {
            @Override
            public void execute( Button source ) {
            	mainMenu();
            	App.rally.getGuiNode().detachChild(pauseMenu);
            }
        });
		pauseMenu.setLocalTranslation(H.screenMiddle().add(pauseMenu.getPreferredSize().mult(-0.5f)));
		
		infoHint = new Container();
		infoHint.attachChild(new Label("TAB for info"));
		infoHint.setLocalTranslation(H.screenTopLeft());
		App.rally.getGuiNode().attachChild(infoHint);
		
		info = new Container();
		info.attachChild(new Label("Controls: move: wasd and arrows , flip: f, handbrake: space, reverse: leftshift, camera: e,z, tab: this, pause: esc, reset: enter, jump: q, nitro: leftcontrol, telemetry: home"));
		info.setLocalTranslation(H.screenTopLeft());
	}

	public void togglePause() {
		Node guiRoot = App.rally.getGuiNode();
		if (guiRoot.hasChild(pauseMenu)) {
			guiRoot.detachChild(pauseMenu);
            App.rally.drive.setEnabled(true);
		} else {
			guiRoot.attachChild(pauseMenu);
			App.rally.drive.setEnabled(false);
		}
	}
	public void toggleMenu() {
		Node guiRoot = App.rally.getGuiNode();
		if (guiRoot.hasChild(info)) {
			guiRoot.attachChild(infoHint);
			guiRoot.detachChild(info);
		} else {
			guiRoot.attachChild(info);
			guiRoot.detachChild(infoHint);
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
		App.rally.next(this);
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
					"\nengine:"+p.engineTorque+"\ntraction:"+p.totalTraction + "\nwheelRot:"+p.totalWheelRot +"\nisDay:"+App.rally.sky.isDay
					+ "\nG Forces:"+p.gForce);
			
			float curGrip = 0;
			//grips
			for (int i = 0 ; i < 4; i++) {
				Material m = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
				gripValue[i].setText(String.format("%.2f", p.wheel[i].skid));
				curGrip = FastMath.clamp(p.wheel[i].skid, 0, 1);
				m.setColor("Color", new ColorRGBA(curGrip, curGrip, curGrip, 1));
				gripBox[i].setMaterial(m);
				
				Vector3f dir = p.wheel[i].gripDir;
				if (dir != null) {
					gripBox[i].setLocalScale(p.wheel[i].susForce/(p.car.mass*2));
					
					gripDir[i].setLocalScale(dir.length()*75);
					
					float angle = FastMath.atan2(dir.z, dir.x);
					Quaternion q = new Quaternion();
					q.fromAngleAxis(angle, Vector3f.UNIT_Z);
					gripDir[i].setLocalRotation(q);
				}
			}
			
			//gees (g forces)
			//needs to be translated from local into screen axis
			Vector3f gs = p.gForce;
			gs.y = gs.z; //z is front back
			gs.z = 0; //screen has no depth 
			g2.setLocalTranslation(gcenter.add(gs.mult(50))); //because screen pixels
			
			gText.setText("x: " + H.roundDecimal(gs.x, 2) +", y: " + H.roundDecimal(gs.y, 2));
		}
	}
	
	@Override
	public void cleanup() {
		super.cleanup();
		
		Rally r = App.rally;
		r.getRootNode().detachChild(localRootNode);
		
		localGuiNode.detachChild(telemetry);
		r.getGuiNode().detachChild(localGuiNode);
		
		
		InputManager i = App.rally.getInputManager();
		i.deleteMapping("Pause");
		i.deleteMapping("TabMenu");
		i.deleteMapping("Telemetry");
		
		i.removeListener(actionListener);
	}
}
