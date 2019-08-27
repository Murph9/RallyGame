package world.wp;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Box;

import game.App;
import effects.LoadModelWrapper;
import helper.Log;
import world.World;
import world.WorldType;
import world.wp.WP.NodeType;

/** Generates the world infront of the player dynamically from static set pieces */
public abstract class DefaultBuilder extends World {

	private Geometry startGeometry;
	protected WP[] type;
	
	protected List<Spatial> curPieces = new ArrayList<Spatial>();
	protected List<WPObject> wpos;
	
	protected Vector3f start = new Vector3f();
	protected Vector3f nextPos = new Vector3f();
	protected Quaternion nextRot = new Quaternion();
	protected NodeType nextNode = null;
	
	protected Random rand;
	
	protected int count = 0;
	protected float distance = 500;
	
	DefaultBuilder(WP[] type) {
		this(type, (long)(Math.random()*Long.MAX_VALUE));
	}
	DefaultBuilder(WP[] type, long seed) {
		super("builder root node");
		this.type = type;
		
		this.rand = new Random(seed);
	}
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		if (isInitialized()) {
			Log.e("init again, too keen");
			return;
		}
		super.initialize(stateManager, app);
		
		this.wpos = new ArrayList<WPObject>();
		for (int i = 0; i < type.length; i++) {
			WPObject wpo = new WPObject();
			wpo.wp = type[i];
			
			Spatial piece = LoadModelWrapper.create(app.getAssetManager(), type[i].getName(), ColorRGBA.Green);
			piece.setCullHint(CullHint.Never);
			wpo.sp = ((Node)piece).getChild(0); //there is only one object in there (hopefully)

			//scale and unscale spatials so that the collision shape size is correct
			wpo.sp.scale(type[i].getScale());
			wpo.col = CollisionShapeFactory.createMeshShape(wpo.sp);
			wpo.sp.scale(1/type[i].getScale());

			this.wpos.add(wpo);
		}
		
		Material matfloor = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		matfloor.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		matfloor.setColor("Color", ColorRGBA.Green);
		
		Box start = new Box(10, 0.25f, 10); //Something to spawn on (or in hint hint)
		startGeometry = new Geometry("Starting Box", start);
		startGeometry.setMaterial(matfloor);
		startGeometry.setLocalTranslation(0, -0.1f, 0);
		startGeometry.addControl(new RigidBodyControl(0));
		
		this.rootNode.attachChild(startGeometry);
		((App)this.app).getPhysicsSpace().add(startGeometry);
	}

	@Override
	public void update(float tpf) {
		if (!isEnabled())
			return;
		
		Vector3f pos = this.app.getCamera().getLocation();
		
		try {
			while (nextPos.subtract(pos).length() < distance)
				selectNewPiece();
		} catch (Exception e) {
			//probably couldn't find something to place
			e.printStackTrace();
		}
		
		List<Spatial> temp = new LinkedList<Spatial>(curPieces);
		for (Spatial sp: temp) {
			Vector3f endSpPos = sp.getWorldTranslation();
			if (endSpPos.subtract(pos).length() > distance/2) {
				//2 because don't delete the ones we just placed
				((App)this.app).getPhysicsSpace().remove(sp.getControl(0));
				rootNode.detachChild(sp);
				curPieces.remove(sp);
			} else {
				break; //this means only remove pieces in order
			}
		}
	}

	protected void selectNewPiece() throws Exception {
		//get list of valid pieces (i.e. pieces with the correct node type)
		List<WPObject> wpoList = new ArrayList<>();
		for (WPObject w: wpos) {
			if (nextNode == null || nextNode == w.wp.startNode()) {
				wpoList.add(w);
			}
		}

		if (wpoList.isEmpty())
			throw new Exception("No pieces with the node start " + nextNode.name() + " found.");

		int i = (int)(rand.nextDouble()*wpoList.size());
		WPObject wpo = wpoList.get(i);
		boolean success = PlacePiece(wpo);
		if (!success) {
			// if it fails, try from the remaining list
			// and remove items until there is nothing to select
			List<WPObject> pieceList = new LinkedList<WPObject>(wpoList);
			while (!success) {
				pieceList.remove(wpo);
				if (pieceList.isEmpty())
					throw new Exception("DefaultBuilder ran out of pieces that fit. Type: " + WPObject.class);

				i = rand.nextInt(pieceList.size());
				wpo = pieceList.get(i);

				success = PlacePiece(wpo);
			}
		}
	}
	
	// Please attempt to use the wonderful getRotation().toAngles() and fromAngles() methods
	// which should stop the slowly getting off track if you just create the quaternion everytime
	// there will always be floating point errors though, unless we really care about that...
	protected boolean PlacePiece(WPObject wpo) {
		WP world = wpo.wp;
		Spatial s = wpo.sp.clone();
		CollisionShape coll = wpo.col;

		Quaternion inv = nextRot.mult(world.getNewAngle()).inverse();
		Quaternion result = Quaternion.IDENTITY.mult(inv);
		float angle = FastMath.acos(result.getW())*2; //believe me that this gets the angle between them

		// Prevent it turning back onto itself - try PI*3/2 if its not interesting enough
		if (!(FastMath.abs(angle) < FastMath.PI))
			return false;
		
		// checking the difference from vertical helps from the weird ever increasing hill
		Vector3f newUp = nextRot.mult(Vector3f.UNIT_Y);
		if (newUp.angleBetween(Vector3f.UNIT_Y) > 0.5f)
			return false;

		float scale = world.getScale();
		//translate, rotate, scale
		s.setLocalTranslation(nextPos);
		s.rotate(nextRot);
		s.scale(scale);

		RigidBodyControl landscape = new RigidBodyControl(coll, 0);
		landscape.setKinematic(false);
		s.addControl(landscape);

		((App)this.app).getPhysicsSpace().add(landscape);
		rootNode.attachChild(s);

		curPieces.add(s);

		//setup the start for the next piece
		Vector3f cur = world.getNewPos().mult(scale);
		nextPos.addLocal(nextRot.mult(cur));
		
		Quaternion rot = world.getNewAngle();
		rot.set(rot);
		nextRot.multLocal(rot);
		
		nextNode = world.endNode();
		
		return true;
	}
	
	public void reset() {
		List<Spatial> ne = new LinkedList<Spatial>(curPieces);
		for (Spatial s: ne) {
			((App)this.app).getPhysicsSpace().remove(s.getControl(0));
			rootNode.detachChild(s);
			curPieces.remove(s);
		}

		start = new Vector3f(0,0,0);
		nextPos = new Vector3f(0,0,0);
		nextRot = new Quaternion();
	}

	@Override
	public Vector3f getStartPos() {
		return new Vector3f(0,1,0);
	}
	
	@Override
	public Matrix3f getStartRot() {
		Matrix3f rot = new Matrix3f();
		rot.fromAngleAxis(FastMath.DEG_TO_RAD*90, new Vector3f(0,1,0));
		return rot;
	}
	
	@Override
	public Vector3f getNextPieceClosestTo(Vector3f myPos) {
		float minDistance = Float.MAX_VALUE;
		int index = -1;
		for (int i = 0; i < curPieces.size() - 1; i++) {
			Vector3f pos = curPieces.get(i).getWorldTranslation();
			float len = pos.subtract(myPos).length();
			if (len < minDistance) {
				minDistance = len;
				index = i;
			}
		}
		if (index != -1) {
			return curPieces.get(index + 1).getWorldTranslation();
		}
		
		return null;
	}
	
	public void cleanup() {
		for (Spatial s: curPieces) {
			((App)this.app).getPhysicsSpace().remove(s.getControl(0));
			rootNode.detachChild(s);
		}
		
		((App) this.app).getPhysicsSpace().remove(startGeometry);

		super.cleanup();
	}
	
	public WorldType getType() {
		return WorldType.DYNAMIC;
	}
	
	public abstract DefaultBuilder copy();
	
	protected class WPObject {
		WP wp;
		Spatial sp;
		CollisionShape col;
	}
}
