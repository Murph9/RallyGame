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

	/*TODO:
	 * red near the redline?
	 */
	
	Rally r; //want all the infos
	
	//hud stuff
	BitmapText statsText;
	BitmapText score;
	static BitmapText debugtext;
	static BitmapText angle;
	
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
	Geometry[] speedo = new Geometry[3];
	//gear label
	Geometry gear = new Geometry();
	
	//speedo numbers
	float startAng = -FastMath.PI;
	int startRPM = 0; //because sometimes it might not be
	
	float finalAng = -FastMath.TWO_PI;
	int finalRPM; //should be more than redline
	float redline;
	
	int centerx, centery = 64+22, radius = 118;
	
	UINode (Rally r) {
		this.r = r;
		this.redline = r.player.car.redline;
		this.finalRPM = (int)FastMath.ceil(this.redline) + 1000;

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
		
		angle = new BitmapText(guiFont, false);		  
		angle.setSize(guiFont.getCharSet().getRenderedSize());
		angle.setColor(ColorRGBA.White);
		angle.setText("blaj");
		angle.setLocalTranslation(settings.getWidth()-300, 30, 0); // position
		guiNode.attachChild(angle);
		
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
		
		speedo[0] = new Geometry("ones", quad);
		speedo[0].setLocalTranslation(width, 8, -1);
		speedo[0].scale(1);
		speedo[0].setMaterial(matset[0]);
		guiNode.attachChild(speedo[0]);
		
		speedo[1] = new Geometry("tens", quad);
		speedo[1].setLocalTranslation(width-32, 8, -1);
		speedo[1].setMaterial(matset[0]);
		guiNode.attachChild(speedo[1]);
		
		speedo[2] = new Geometry("hundereds", quad);
		speedo[2].setLocalTranslation(width-64, 8, -1);
		speedo[2].setMaterial(matset[0]);
		guiNode.attachChild(speedo[2]);
		
		gear = new Geometry("gear", quad);
		gear.setLocalTranslation(width-64, 68, -1);
		gear.setMaterial(matset[1]);
		guiNode.attachChild(gear);
		
		makeSpeedo(r, guiNode);
		
	}

	private void makeSpeedo(Rally r2, Node guiNode) {
		Node speedoNode = new Node("Speedo");
		guiNode.attachChild(speedoNode);
		
		Line l = new Line(Vector3f.ZERO, Vector3f.UNIT_X.negate()); //inwards
		l.setLineWidth(2);
		Quad quad = new Quad(20, 20);
		
		centerx = r2.getSettings().getWidth()-128;
		
		Material m = new Material(r.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		m.setColor("Color", ColorRGBA.Black);
		
		for (int i = 0; i <= finalRPM; i += 1000) {
			float angle = FastMath.interpolateLinear(i/(float)finalRPM, startAng, finalAng);;
			
			Geometry line = new Geometry("RPM", l);
			line.setLocalTranslation(centerx+FastMath.cos(angle)*radius, centery+FastMath.sin(angle)*radius, 0);
			Quaternion q = new Quaternion();
			q.fromAngleAxis(angle, Vector3f.UNIT_Z);
			line.setLocalRotation(q);
			line.scale(20);
			line.setMaterial(m);
			speedoNode.attachChild(line);
			
			Node g = addNumber(angle, (int)i/1000, quad);
			speedoNode.attachChild(g);
		}
	}
	
	private Node addNumber(float angle, int i, Quad quad) {
		Node n = new Node("rpm "+i);
		n.setLocalTranslation(centerx-10, centery-10, 0);
		
		Geometry g = new Geometry("speedoNumber "+i, quad);
		if (i > 9) { //multinumber
			g.setLocalTranslation(FastMath.cos(angle)*(radius-20), FastMath.sin(angle)*(radius-20), 0);
			g.setMaterial(matset[i % 10]);
			n.attachChild(g);
			
			Geometry g2 = new Geometry("speedoNumber "+i+"+", quad);
			g2.setLocalTranslation(-20+FastMath.cos(angle)*(radius-20), FastMath.sin(angle)*(radius-20), 0);
			g2.setMaterial(matset[i/10]);
			n.attachChild(g2);
			
		} else { //normal number
			g.setLocalTranslation(FastMath.cos(angle)*(radius-20), FastMath.sin(angle)*(radius-20), 0);
			g.setMaterial(matset[i]);
			n.attachChild(g);
		}
		
		return n;
	}

	public void update(float tpf) {
		MyVehicleControl player = r.player;
		AssetManager assetManager = r.getAssetManager();
		
		r.player.getForwardVector(r.player.forward);
		
		float speed = player.getLinearVelocity().length();
		
		statsText.setText("speed:"+speed + "m/s\nRPM:" + player.curRPM);
		int speedKMH = (int)Math.abs(player.getCurrentVehicleSpeedKmHour());

		setSpeedDigits(speedKMH);
		setGearDigit(player.curGear);
		
		if (r.dynamicWorld) {
			score.setText("Placed: "+r.worldB.getTotalPlaced());
		}
		
		//TODO slightly fancier?
		rpmArrow.setLocalRotation(Quaternion.IDENTITY);
		float angle = FastMath.interpolateLinear(player.curRPM/(float)finalRPM, startAng, finalAng);
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

	private void setSpeedDigits(int speedKMH) {
		int count = 0;
		while (count < 3) {
			speedo[count].setMaterial(matset[0]);
			count++;
		}
		
		count = 0;
		speedKMH %= 1000; //not more than 999
		while (speedKMH > 0) {
			speedo[count].setMaterial(matset[speedKMH % 10]);
			speedKMH /= 10;
			count++;
		}
	}
	
	private void setGearDigit(int gearIn) {
		gearIn = (int)FastMath.clamp(gearIn, 0, 9); //so we don't go off the end of the texture array
		gear.setMaterial(matset[gearIn]);
	}
	
	
	public static void setDebugText(String text) {
		debugtext.setText(text);
	}
}
