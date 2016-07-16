package car;

import java.util.ArrayList;
import java.util.List;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.shape.Line;
import com.jme3.scene.shape.Quad;
import com.jme3.system.AppSettings;
import com.jme3.util.BufferUtils;

import game.App;
import game.Rally;

public class UINode {

	/*TODO:
	 * get it all to kinda look like the forza 6 one
	 * - their text is white infront of black so that you can see it always
	 */
	MyPhysicsVehicle p;
	
	Node localRootNode;
	
	//hud stuff
	Geometry background;
	BitmapText score;
	BitmapText angle;
	
	//rpm
	List<Geometry> rpmBar;
	float rpmBarStep = 50;
	private Material rpmBarOff; //default state
	
	private Material rpmBarOn; //less than rpm
	private Material rpmBarOnAlt; //for the increment marking (and in rpm)
	
	private Material rpmBarRedLine; //marking the rpm
	private Material rpmBarRedLineOn; //for past redline
	
	//quad that displays nitro
	Geometry nitro;
	Geometry nitroOff;
	
	//texture
	final String numDir = "assets/number/"; //texture location
	Material[] numMats = new Material[10]; //texture set
	
	//speed squares
	Geometry[] speedo = new Geometry[3];
	//gear label
	Geometry gear = new Geometry();
	
	//speedo numbers
	float startAng = FastMath.PI*5/4;
	int startRPM = 0; //because sometimes it might not be

	float finalAng = 0;
	int finalRPM; //should be more than redline
	float redline;
	
	int centerx, centery = 64+22, radius = 100;
	
	public UINode () {
		Rally r = App.rally;	
		this.p = r.drive.cb.get(0);
		
		this.redline = p.car.e_redline;
		this.finalRPM = (int)FastMath.ceil(this.redline) + 1000;

		BitmapFont guiFont = r.getFont();
		AppSettings settings = r.getSettings();
		localRootNode = new Node("local root");
		r.getGuiNode().attachChild(localRootNode);
		AssetManager assetManager = r.getAssetManager();
		
		score = new BitmapText(guiFont, false);
		score.setSize(guiFont.getCharSet().getRenderedSize());
		score.setColor(ColorRGBA.White);
		score.setText("");
		score.setLocalTranslation(settings.getWidth()-200, settings.getHeight(), 0); // position
		localRootNode.attachChild(score);
		
		angle = new BitmapText(guiFont, false);		  
		angle.setSize(guiFont.getCharSet().getRenderedSize());
		angle.setColor(ColorRGBA.White);
		angle.setText("blaj");
		angle.setLocalTranslation(settings.getWidth()-300, 30, 0); // position
		localRootNode.attachChild(angle);
		
		//////////////////////////////
		//speedo number textures
		for (int i = 0 ; i < 10; i++) {
			numMats[i] = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
			numMats[i].setTexture("ColorMap", assetManager.loadTexture(numDir+i+".png"));
			numMats[i].getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
		}
		
		makeSpeedo(assetManager, settings);
	}
	
	private void makeSpeedo(AssetManager am, AppSettings settings) {
		makeKmH(settings);
		
		Material m = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		m.setColor("Color", ColorRGBA.Black);

		Line l = new Line(Vector3f.ZERO, Vector3f.UNIT_X);
		l.setLineWidth(4);
		
		///////////////
		//make the variable parts:
		Node speedoNode = new Node("Speedo");
		localRootNode.attachChild(speedoNode);
		
		//TODO better contrast on speedo
		Quad qback = new Quad(270, 200);
		background = new Geometry("ui-background", qback);
//		Material trans = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
//		trans.setColor("Color", new ColorRGBA(0,0,0,0.3f));
//		background.setMaterial(trans);
//		background.setLocalTranslation(settings.getWidth()-270, 0, -10);
//		speedoNode.attachChild(background);
		
		//rpm bars
		l = new Line(Vector3f.ZERO, Vector3f.UNIT_X.negate()); //inwards
		l.setLineWidth(2);
		Quad quad = new Quad(20, 20);
		
		centerx = App.rally.getSettings().getWidth()-127;
		
		rpmBarOn = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		rpmBarOn.setColor("Color", ColorRGBA.White);
		
		rpmBarOnAlt = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		rpmBarOnAlt.setColor("Color", ColorRGBA.LightGray);
		
		rpmBarOff = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		rpmBarOff.setColor("Color", ColorRGBA.Gray);
		
		rpmBarRedLine = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		rpmBarRedLine.setColor("Color", new ColorRGBA(ColorRGBA.Red).mult(0.7f));
		
		rpmBarRedLineOn = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		rpmBarRedLineOn.setColor("Color", ColorRGBA.Red);
		
		this.rpmBar = new ArrayList<Geometry>();
		
		for (int i = 0; i < finalRPM+1; i += rpmBarStep) {
			float angle = FastMath.interpolateLinear(i/(float)finalRPM, startAng, finalAng);
			float angle2 = FastMath.interpolateLinear((i+rpmBarStep)/(float)finalRPM, startAng, finalAng);
			
			Mesh rpmM = new Mesh();
			Vector3f [] vs = new Vector3f[4]; //order is inside vertex then outside
			vs[0] = new Vector3f(FastMath.cos(angle)*radius*0.9f, FastMath.sin(angle)*radius*0.9f, -1);
			vs[1] = new Vector3f(FastMath.cos(angle2)*radius*0.9f, FastMath.sin(angle2)*radius*0.9f, -1);
			vs[2] = new Vector3f(FastMath.cos(angle)*radius, FastMath.sin(angle)*radius, 1);
			vs[3] = new Vector3f(FastMath.cos(angle2)*radius, FastMath.sin(angle2)*radius, 1);
			
			Vector2f[] texCoord = new Vector2f[4];
			texCoord[0] = new Vector2f(0,0);
			texCoord[1] = new Vector2f(1,0);
			texCoord[2] = new Vector2f(0,1);
			texCoord[3] = new Vector2f(1,1);
			int [] indexes = { 2,0,1, 1,3,2 };
			
			rpmM.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vs));
			rpmM.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoord));
			rpmM.setBuffer(Type.Index,    3, BufferUtils.createIntBuffer(indexes));
			rpmM.updateBound();

			//add physics space and mesh
			Geometry rpmB = new Geometry("rpmBar"+i, rpmM);
			rpmB.setLocalTranslation(centerx, centery, -1);//behind other things
			speedoNode.attachChild(rpmB);
			rpmBar.add(rpmB);
			
			if (i % 1000 == 0) {
				Node g = addRPMNumber(angle, (int)i/1000, quad, centerx-10, centery-10);
				speedoNode.attachChild(g);
			} 
			
			rpmB.setMaterial(rpmBarOff); //no idea until the physics starts
		}
		
		
		//nitro 
		Quad q = new Quad(10, 80);
		nitroOff = new Geometry("nitroback", q);
		Material nitroM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		nitroM.setColor("Color", new ColorRGBA(ColorRGBA.Green).mult(0.5f));
		nitroOff.setMaterial(nitroM);
		nitroOff.setLocalTranslation(centerx - (settings.getWidth() - centerx), 10, 0);
		speedoNode.attachChild(nitroOff);
		
		nitro = new Geometry("nitro", q);
		nitroM = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		nitroM.setColor("Color", new ColorRGBA(ColorRGBA.Green));
		nitro.setMaterial(nitroM);
		nitro.setLocalTranslation(centerx - (settings.getWidth() - centerx), 10, 0);
		speedoNode.attachChild(nitro);
	}

	private void makeKmH(AppSettings settings) {
		//speed scores
		Quad quad = new Quad(30,50);
		int width = settings.getWidth()-70;
		
		speedo[0] = new Geometry("ones", quad);
		speedo[0].setLocalTranslation(width, 8, -1);
		speedo[0].setMaterial(numMats[0]);
		localRootNode.attachChild(speedo[0]);
		
		speedo[1] = new Geometry("tens", quad);
		speedo[1].setLocalTranslation(width-32, 8, -1);
		speedo[1].setMaterial(numMats[0]);
		localRootNode.attachChild(speedo[1]);
		
		speedo[2] = new Geometry("hundereds", quad);
		speedo[2].setLocalTranslation(width-64, 8, -1);
		speedo[2].setMaterial(numMats[0]);
		localRootNode.attachChild(speedo[2]);
		
		gear = new Geometry("gear", quad);
		gear.setLocalTranslation(width-64, 68, -1);
		gear.setMaterial(numMats[1]);
		localRootNode.attachChild(gear);
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

	
	//main update method
	public void update(float tpf) {
		int speedKMH = (int)Math.abs(p.getCurrentVehicleSpeedKmHour());

		setSpeedDigits(speedKMH);
		setGearDigit(p.curGear);
		
		if (App.rally.drive.dynamicWorld) {
			score.setText("Placed: "+App.rally.drive.worldB.getTotalPlaced());
		}
		
		//highlight the rpmBar the right amout
		for (int i = 0; i < rpmBar.size(); i++) {
			if (i*rpmBarStep >= redline) { //its a red one
				if (p.curRPM < i*rpmBarStep + rpmBarStep/2)
					rpmBar.get(i).setMaterial(rpmBarRedLine);
				else
					rpmBar.get(i).setMaterial(rpmBarRedLineOn);
			} else {
				if (p.curRPM < i*rpmBarStep + rpmBarStep/2)
					rpmBar.get(i).setMaterial(rpmBarOff);
				else
					if (i*rpmBarStep % 500 == 0)
						rpmBar.get(i).setMaterial(rpmBarOnAlt);
					else 
						rpmBar.get(i).setMaterial(rpmBarOn);
			}
		}
		
		angle.setText(p.getAngle()+"'");
		nitro.setLocalScale(1, p.nitro/p.car.nitro_max, 1);
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
	
	public void cleanup() {
		App.rally.getGuiNode().detachChild(localRootNode);
	}
}
