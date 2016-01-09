package game;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Line;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;

public class UINode {

	//TODO:
	/* the code is terrible
	 * the output for the track car is terrible
	 * red near the redline?
	 */
	
	Rally r; //want all the infos
	
	//hud stuff
	BitmapText statsText;
	BitmapText score;
	static BitmapText debugtext;
	
	Geometry rpmArrow;
	Geometry redlineArrow;
	Geometry rpmBackground;
	
	//displays the skid value, TODO make better
	Geometry gripBox0;
	Geometry gripBox1;
	Geometry gripBox2;
	Geometry gripBox3;
	
	//texture location
	final String dir = "assets/digital/";
	
	//texture set
	Material[] matset = new Material[10];
	
	//speed squares
	Geometry[] speed = new Geometry[9];
	
	//speedo numbers
	float zeroAng = -FastMath.PI;
	float maxAng = -FastMath.TWO_PI;
	int maxAngRPM;
	float redline;
	
	int centerx, centery = 64+22, radius = 118;
	
	UINode (Rally r) {
		this.r = r;
		this.redline = r.player.car.redline;
		this.maxAngRPM = (int)FastMath.ceil(this.redline) + 1000;

		BitmapFont guiFont = r.getFont();
		AppSettings settings = r.getSettings();
		Node guiNode = r.getGuiNode();
		AssetManager assetManager = r.getAssetManager();
		
		statsText = new BitmapText(r.getFont(), false);		  
		statsText.setSize(guiFont.getCharSet().getRenderedSize());	  		// font size
		statsText.setColor(ColorRGBA.White);								// font color
		statsText.setText("");												// the text
		statsText.setLocalTranslation(settings.getWidth()-200, 300, 0); // position
		guiNode.attachChild(statsText);
		
		score = new BitmapText(guiFont, false);		  
		score.setSize(guiFont.getCharSet().getRenderedSize());
		score.setColor(ColorRGBA.White);
		score.setText("");
		score.setLocalTranslation(settings.getWidth()-200, settings.getHeight(), 0); // position
		guiNode.attachChild(score);
		
		debugtext = new BitmapText(guiFont, false);		  
		debugtext.setSize(guiFont.getCharSet().getRenderedSize());
		debugtext.setColor(ColorRGBA.White);
		debugtext.setText("Hey");
		debugtext.setLocalTranslation(200, 30, 0); // position
		guiNode.attachChild(debugtext);
		
		///////////////////////////////////////////////
		Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		m.setColor("Color", ColorRGBA.Black);

		Line l = new Line(Vector3f.ZERO, Vector3f.UNIT_X);
		l.setLineWidth(4);
		
		int width = settings.getWidth()-128;
		rpmArrow = new Geometry("RPM", l);
		rpmArrow.setLocalTranslation(width, -40+128, 0);
		rpmArrow.scale(100);
		rpmArrow.setMaterial(m);
		guiNode.attachChild(rpmArrow);
		
		m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		m.setTexture("ColorMap", assetManager.loadTexture(dir+"speedo3.png"));
		m.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
		
		Quad qrpm = new Quad(128,128);
		rpmBackground = new Geometry("SpeedoBackground", qrpm);
		rpmBackground.setLocalTranslation(width-128, -40, -1);
		rpmBackground.scale(2);
		rpmBackground.setMaterial(m);
		guiNode.attachChild(rpmBackground);

		
		///////////////////////////////////
		//grip boxes
		m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		m.setColor("Color", ColorRGBA.White);
		
		width = settings.getWidth() - 350;
		Box b = new Box(10, 10, 1);
		gripBox0 = new Geometry("frontleft", b);
		gripBox0.setLocalTranslation(width, 50, 0);
		gripBox0.setMaterial(m);
		guiNode.attachChild(gripBox0);
		
		gripBox1 = new Geometry("frontright", b);
		gripBox1.setLocalTranslation(width +25, 50, 0);
		gripBox1.setMaterial(m);
		guiNode.attachChild(gripBox1);
		
		gripBox2 = new Geometry("rearleft", b);
		gripBox2.setLocalTranslation(width, 25, 0);
		gripBox2.setMaterial(m);
		guiNode.attachChild(gripBox2);
		
		gripBox3 = new Geometry("rearright", b);
		gripBox3.setLocalTranslation(width + 25, 25, 0);
		gripBox3.setMaterial(m);
		guiNode.attachChild(gripBox3);
		
		//////////////////////////////
		//seven segment textures
		for (int i = 0 ; i < 10; i++) {
			matset[i] = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
			matset[i].setTexture("ColorMap", assetManager.loadTexture(dir+i+".png"));
			matset[i].getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
		}
		
		//speed scores
		Quad quad = new Quad(29,49);
		width = settings.getWidth()-130;
		
		speed[0] = new Geometry("ones", quad);
		speed[0].setLocalTranslation(width, 8, -1);
		speed[0].scale(1);
		speed[0].setMaterial(matset[0]);
		guiNode.attachChild(speed[0]);
		
		speed[1] = new Geometry("SpeedoBackground", quad);
		speed[1].setLocalTranslation(width-32, 8, -1);
		speed[1].setMaterial(matset[0]);
		guiNode.attachChild(speed[1]);
		
		speed[2] = new Geometry("SpeedoBackground", quad);
		speed[2].setLocalTranslation(width-64, 8, -1);
		speed[2].setMaterial(matset[0]);
		guiNode.attachChild(speed[2]);
		
		makeSpeedo(r, guiNode);
	}

	private void makeSpeedo(Rally r2, Node guiNode) {
		Node speedoNode = new Node("Speedo");
		guiNode.attachChild(speedoNode);
		
		Line l = new Line(Vector3f.ZERO, Vector3f.UNIT_X.negate()); //inwards
		l.setLineWidth(2);
		
		centerx = r2.getSettings().getWidth()-128;
		
		Material m = new Material(r.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		m.setColor("Color", ColorRGBA.Black);
		
		final int MAX = 0; //3 notches inbetween
		int linecount = MAX; //start with big notch
		
		int count = 0;
		for (float i = zeroAng; i >= maxAng; i += (maxAng-zeroAng)*(1000/(float)maxAngRPM)) {
	
			Geometry line = new Geometry("RPM", l);
			line.setLocalTranslation(centerx+FastMath.cos(i)*radius, centery+FastMath.sin(i)*radius, 0);
			Quaternion q = new Quaternion();
			q.fromAngleAxis(i, Vector3f.UNIT_Z);
			line.setLocalRotation(q);
			if (linecount != MAX) {
				line.scale(10);
				linecount++;
			} else {
				line.scale(20);
				linecount = 0;
				Quad quad = new Quad(20, 20);
				Geometry g = new Geometry("speedoNumber"+count, quad);
				g.setLocalTranslation((centerx-10)+FastMath.cos(i)*(radius-20), (centery-10)+FastMath.sin(i)*(radius-20), 0);
				if (count < 10) {
					g.setMaterial(matset[count]);
				} else {
					g.setMaterial(matset[count-10]); //TODO fix
				}
				if (count*1000 > redline) {
					m = new Material(r.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
					m.setColor("Color", ColorRGBA.Red);
				}
				speedoNode.attachChild(g);
				count++;
			}
			line.setMaterial(m);
			speedoNode.attachChild(line);
		}
	}

	public void update(float tpf) {
		MyVehicleControl player = r.player;
		AssetManager assetManager = r.getAssetManager();
		
		r.player.getForwardVector(r.player.forward);
		
		float speed = player.getLinearVelocity().length();
		
		statsText.setText(speed + "m/s\ngear:" + player.curGear + "\naccel:" + player.curRPM+ "\n"); // the ui text
		int speedKMH = (int)Math.abs(player.getCurrentVehicleSpeedKmHour());

		setDigits(speedKMH);
		
		if (r.dynamicWorld) {
			score.setText("Placed: "+r.worldB.getTotalPlaced());
		}
		
		rpmArrow.setLocalRotation(Quaternion.IDENTITY);
		float angle = FastMath.interpolateLinear(player.curRPM/(float)maxAngRPM, zeroAng, maxAng);
		rpmArrow.rotate(0, 0, angle);
		
		Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		m.setColor("Color", new ColorRGBA(player.wheel[0].skid,player.wheel[0].skid,player.wheel[0].skid,1));
		gripBox0.setMaterial(m);
		m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		m.setColor("Color", new ColorRGBA(player.wheel[1].skid,player.wheel[1].skid,player.wheel[1].skid,1));
		gripBox1.setMaterial(m);
		m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		m.setColor("Color", new ColorRGBA(player.wheel[2].skid,player.wheel[2].skid,player.wheel[2].skid,1));
		gripBox2.setMaterial(m);
		m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		m.setColor("Color", new ColorRGBA(player.wheel[3].skid,player.wheel[3].skid,player.wheel[3].skid,1));
		gripBox3.setMaterial(m);
	}

	private void setDigits(int speedKMH) {
		int count = 0;
		while (count < 3) {
			speed[count].setMaterial(matset[0]);
			count++;
		}
		
		count = 0;
		speedKMH %= 1000; //not more than 999
		while (speedKMH > 0) {
			speed[count].setMaterial(matset[speedKMH % 10]);
			speedKMH /= 10;
			count++;
		}
		
	}
	
	
	public static void setDebugText(String text) {
		debugtext.setText(text);
	}
}
