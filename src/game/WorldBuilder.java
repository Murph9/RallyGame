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
	 * Make world pieces
	 * use the player position 
	 * remove the pieces after we are far away
	 */
	
	float scale;
	Rally rally;
	AssetManager assetManager;
	Material mat;
	
	List<Spatial> pieces = new LinkedList<Spatial>();
	
	Vector3f start;
	
	Vector3f nextPos;
	float nextAngle;
	int count = 0;
	
	
	WorldBuilder (Rally rally, AssetManager asset) {
		this.rally = rally;
		this.scale = 10;
		this.assetManager = asset;
		this.start = new Vector3f(0,0,0);
		this.nextPos = new Vector3f(0,0,0);
		
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
        
        Box start = new Box(10, 0.25f, 10);
        Geometry startGeometry = new Geometry("Floor", start);
        startGeometry.setMaterial(matfloor);
        startGeometry.setLocalTranslation(0, -0.1f, 0);
        startGeometry.addControl(new RigidBodyControl(0));
        this.attachChild(startGeometry);
        rally.getPhysicsSpace().add(startGeometry);
        
	}
	
	public void update(Vector3f playerPos) {
		count++;
		if (count % 75 == 0) {
			WorldPiece[] a = WorldPiece.values();
			int next = (int)(Math.random()*a.length);
			WorldPiece cur = a[next];
			cur.setMirrored(FastMath.nextRandomInt()%2==0);
			addModel(a[next]);
		}
	}
	
	//test for world building
	public void addModel(WorldPiece world) {
		 //imported model		
		Spatial worldNode = assetManager.loadModel(world.getName());
		Spatial s = ((Node)worldNode).getChild(0);
		s.setMaterial(mat); //TODO double sided objects
		
		//translate, rotate, scale
		s.setLocalTranslation(nextPos);
		s.rotate(0, nextAngle, 0);
		Vector3f v = new Vector3f(1,1,1).mult(scale);
		if (world.getMirrored()) {
			if (world.equals(WorldPiece.HILL)) {
				s.scale(v.x, -v.y, v.z);
			} else {
				s.scale(v.x, v.y, -v.z);
			}
		} else {
			s.scale(v.x, v.y, v.z);
		}

		CollisionShape coll = CollisionShapeFactory.createMeshShape(s);
	
		RigidBodyControl landscape = new RigidBodyControl(coll, 0);
		s.addControl(landscape);
		rally.getPhysicsSpace().add(landscape);
		this.attachChild(s);

		if (rally.ifDebug) {
			System.err.println("Adding: "+ world.getName() + ", at: " + nextPos);
			System.err.println("Rot: " + nextAngle + ", Obj.angle: " + world.getNewAngle() + ", Obj.nextPos: " + world.getNewPos());
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

enum WorldPiece {
	
	CROSS("world/wb_cross.blend", new Vector3f(2,0,0), 0),
	STRAIGHT("world/wb_straight.blend", new Vector3f(2,0,0), 0),
	SHARP_LEFT("world/wb_sharp.blend", new Vector3f(1,0,-1), FastMath.DEG_TO_RAD*90),
	HILL("world/wb_hill.blend", new Vector3f(2,0.5f,0), 0),
	LEFT("world/wb_corner.blend", new Vector3f(1,0,-1), FastMath.DEG_TO_RAD*90),
	LONG_LEFT("world/wb_long.blend", new Vector3f(2,0,-2), FastMath.DEG_TO_RAD*90),
	CHICANE_LEFT("world/wb_diff.blend", new Vector3f(2,0,-1), 0),
	;
	
	private String name;
	private Vector3f newPos; //what the piece does to the track
	private float newAngle; //change of angle (deg) for the next peice

	private boolean mirrored;
	
	WorldPiece(String s, Vector3f a, float g) {
		this.name = s;
		this.newPos = a;
		this.newAngle = g;
		this.mirrored = false;
	}

	void setMirrored (boolean i) { mirrored = i; }
	boolean getMirrored() { return mirrored; }
	
	String getName() {
		return name;
	}
	
	//gets to update the position of the next one
	Vector3f getNewPos() {
		Vector3f out = new Vector3f(newPos.x, newPos.y, newPos.z);
		if (mirrored) {
			if (this.equals(WorldPiece.HILL)) {
				out.y *= -1;
			} else {
				out.z *= -1;
			}
		}
		return out;
	}
	
	float getNewAngle() {
		float out = newAngle;
		if (mirrored) {
			out *= -1;
		}		
		return out;
	}
}
