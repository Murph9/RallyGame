package game;

import java.util.LinkedList;
import java.util.List;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

public class WorldBuilder extends Node {

	//designed to generate the world infront of the player dynamically.
	
	/* TODO:
	 * use the player position 
	 * remove the pieces after we are far away
	 */
	
	
	Rally rally;
	AssetManager assetManager;
	
	List<Spatial> pieces = new LinkedList<Spatial>();
	
	Vector3f start;
	Vector3f nextPos;
	float scale;
	float nextAngle;
	int count = 0;

	Material mat;
	boolean needsMaterial;
	
	WorldBuilder (Rally rally, AssetManager asset) {
		this.rally = rally;
		this.scale = 15;
		this.assetManager = asset;
		this.start = new Vector3f(0,0,0);
		this.nextPos = new Vector3f(0,0,0);
		this.needsMaterial = false;
		
		this.mat = new Material(assetManager, "Common/MatDefs/Misc/ShowNormals.j3md");
		this.mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		
		//Just get a generic floor for testing
		Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		mat.setColor("Color", ColorRGBA.Blue);
		
		Box floorBox = new Box(140, 0.25f, 140);
        Geometry floorGeometry = new Geometry("Floor", floorBox);
        floorGeometry.setMaterial(mat);
        floorGeometry.setLocalTranslation(0, -10, 0);
        floorGeometry.addControl(new RigidBodyControl(0));
        this.attachChild(floorGeometry);
        rally.getPhysicsSpace().add(floorGeometry);
        
        Material matfloor = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        matfloor.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        matfloor.setColor("Color", ColorRGBA.Red);
        
        Box start = new Box(10, 0.25f, 10); //Something to spawn on (or in hint hint)
        Geometry startGeometry = new Geometry("Starting Box", start);
        startGeometry.setMaterial(matfloor);
        startGeometry.setLocalTranslation(0, -0.1f, 0);
        startGeometry.addControl(new RigidBodyControl(0));
        this.attachChild(startGeometry);
        rally.getPhysicsSpace().add(startGeometry);
        
	}
	
	public void update(Vector3f playerPos) {
		count++;
		if (count % 10 == 0) { //don't check that often
			switch ("WorldPiece") {
			/*case "WorldPiece": //timed track is cool
				WorldPiece[] a = WorldPiece.values();
				int next = (int)(Math.random()*a.length);
				addModel(a[next]);
				break;*/
			case "WorldPiece":
				WorldPiece[] b = WorldPiece.values();
				while (nextPos.subtract(playerPos).length() < 50) {
					int nextb = (int)(Math.random()*b.length);
					addModel(b[nextb]);	
				}
				break;
			case "WPCity":
				WPCity[] c = WPCity.values();
				while (nextPos.subtract(playerPos).length() < 50) {
					int nextc = (int)(Math.random()*c.length);
					addModel(c[nextc]);
				}
				break;
			default:
				break;
			}
		}
	}
	
	//test for world building
	public void addModel(Blocks world) {
		 //imported model		
		Spatial worldNode = assetManager.loadModel(world.getName());
		Spatial s = ((Node)worldNode).getChild(0); //there is only one object in there (hopefully)
		if (needsMaterial) {
			s.setMaterial(mat); //TODO double sided objects
		}
		
		//translate, rotate, scale
		s.setLocalTranslation(nextPos);
		s.rotate(0, nextAngle, 0);
		s.scale(scale);

		CollisionShape coll = CollisionShapeFactory.createMeshShape(s);
	
		RigidBodyControl landscape = new RigidBodyControl(coll, 0);
		s.addControl(landscape);
		rally.getPhysicsSpace().add(landscape);
		this.attachChild(s);

		System.out.println("Adding: "+world.getName());
		if (rally.ifDebug) {
			System.err.println("at: "+nextPos+", Rot: "+nextAngle+", Obj.angle: "+world.getNewAngle()+", Obj.nextPos: "+world.getNewPos());
		}
		pieces.add(s);
		
		if (pieces.size() > 20) {
			Spatial sp = pieces.get(0);
			rally.getPhysicsSpace().remove(sp.getControl(0));
			this.detachChild(sp);
			pieces.remove(sp);
		}
		
		///////////////////////////////////////
		//setup the position of the next object
		Quaternion q = new Quaternion();
		q.fromAngleAxis(nextAngle, Vector3f.UNIT_Y);

		Vector3f cur = world.getNewPos().mult(scale);
		nextPos.addLocal(q.mult(cur));
		
		nextAngle += world.getNewAngle();
		nextAngle = nextAngle % 360; //so it doesn't get large
	}
}

enum WorldPiece implements Blocks {
	
	CROSS("wbsimple/cross.blend", new Vector3f(2,0,0), 0),
	STRAIGHT("wbsimple/straight.blend", new Vector3f(2,0,0), 0),
	
	LEFT("wbsimple/left.blend", new Vector3f(1,0,-1), FastMath.DEG_TO_RAD*90),
	LEFT_SHARP("wbsimple/left_sharp.blend", new Vector3f(1,0,-1), FastMath.DEG_TO_RAD*90),
	LEFT_LONG("wbsimple/left_long.blend", new Vector3f(2,0,-2), FastMath.DEG_TO_RAD*90),
	LEFT_CHICANE("wbsimple/left_chicane.blend", new Vector3f(2,0,-1), 0),
	
	RIGHT("wbsimple/right.blend", new Vector3f(1,0,1), FastMath.DEG_TO_RAD*-90),
	RIGHT_SHARP("wbsimple/right_sharp.blend", new Vector3f(1,0,1), FastMath.DEG_TO_RAD*-90),
	RIGHT_LONG("wbsimple/right_long.blend", new Vector3f(2,0,2), FastMath.DEG_TO_RAD*-90),
	RIGHT_CHICANE("wbsimple/right_chicane.blend", new Vector3f(2,0,1), 0),
	
	HILL_UP("wbsimple/hill_up.blend", new Vector3f(4,0.5f,0), 0),
	HILL_DOWN("wbsimple/hill_down.blend", new Vector3f(4,-0.5f,0), 0),
	;
	
	String name;
	Vector3f newPos; //what the piece does to the track
	float newAngle; //change of angle (deg) for the next peice

	WorldPiece(String s, Vector3f a, float g) {
		this.name = s;
		this.newPos = a;
		this.newAngle = g;
	}
	public String getName() {
		return name;
	}
	public Vector3f getNewPos() {
		return newPos;
	}
	public float getNewAngle() {
		return newAngle;
	}
}


enum WPCity implements Blocks { //TODO finish all the models here
	CROSS("wbcity/cross.blend", new Vector3f(2,0,0), 0),
	STRAIGHT("wbcity/straight.blend", new Vector3f(2,0,0), 0),
	
	LEFT("wbcity/left.blend", new Vector3f(1,0,-1), FastMath.DEG_TO_RAD*90),
	LEFT_SHARP("wbcity/left_sharp.blend", new Vector3f(1,0,-1), FastMath.DEG_TO_RAD*90),
	LEFT_LONG("wbcity/left_long.blend", new Vector3f(2,0,-2), FastMath.DEG_TO_RAD*90),
	LEFT_CHICANE("wbcity/left_chicane.blend", new Vector3f(2,0,-1), 0),
	
	RIGHT("wbcity/right.blend", new Vector3f(1,0,1), FastMath.DEG_TO_RAD*-90),
	RIGHT_SHARP("wbcity/right_sharp.blend", new Vector3f(1,0,1), FastMath.DEG_TO_RAD*-90),
	RIGHT_LONG("wbcity/right_long.blend", new Vector3f(2,0,2), FastMath.DEG_TO_RAD*-90),
	RIGHT_CHICANE("wbcity/right_chicane.blend", new Vector3f(2,0,1), 0),
	
	HILL_UP("wbcity/hill_up.blend", new Vector3f(4,0.5f,0), 0),
	HILL_DOWN("wbcity/hill_down.blend", new Vector3f(4,-0.5f,0), 0),
	;
	
	String name;
	Vector3f newPos; //what the piece does to the track
	float newAngle; //change of angle (deg) for the next piece

	WPCity(String s, Vector3f a, float g) {
		this.name = s;
		this.newPos = a;
		this.newAngle = g;
	}
	public String getName() {
		return name;
	}
	public Vector3f getNewPos() {
		return newPos;
	}
	public float getNewAngle() {
		return newAngle;
	}
}


interface Blocks {
	
	String getName();
	Vector3f getNewPos();
	float getNewAngle();
}