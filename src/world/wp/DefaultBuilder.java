package world.wp;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
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
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

import effects.LoadModelWrapper;
import world.World;import world.IWorld;
import world.WorldType;
import world.wp.WP.NodeType;

/** Generates the world infront of the player dynamically from static set pieces */
public abstract class DefaultBuilder extends World implements IWorld {

    protected static final float SPAWN_DISTANCE = 500;

	protected final WP[] type;
	protected final List<Spatial> curPieces = new ArrayList<Spatial>();
    
    private Geometry startGeometry;
    protected List<WPObject> wpos;
	
	protected Vector3f nextPos;
	protected Quaternion nextRot;
	protected NodeType nextNode;
	
	protected Random rand;
	
	protected int count = 0;
	
	DefaultBuilder(WP[] type) {
		this(type, (long)(Math.random()*Long.MAX_VALUE));
	}
	DefaultBuilder(WP[] type, long seed) {
		super("builder root node");
		this.type = type;
		
        this.rand = new Random(seed);
        
        reset();
    }
    
	@Override
	public void initialize(Application app) {
		super.initialize(app);
		
		this.wpos = new ArrayList<WPObject>();
		for (int i = 0; i < type.length; i++) {
			WPObject wpo = new WPObject();
			wpo.wp = type[i];
			wpo.sp = LoadModelWrapper.create(app.getAssetManager(), wpo.wp.getName(), null);

			//scale and unscale spatials so that the collision shape size is correct
			wpo.sp.scale(wpo.wp.getScale());
			wpo.col = CollisionShapeFactory.createMeshShape(wpo.sp);
			wpo.sp.scale(1 / wpo.wp.getScale());
            
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
		getState(BulletAppState.class).getPhysicsSpace().add(startGeometry);
	}

	@Override
	public void update(float tpf) {
		Vector3f camPos = getApplication().getCamera().getLocation();

		try {
			placeNewPieces(camPos);
		} catch (IllegalStateException e) {
			//probably couldn't find something to place
			e.printStackTrace();
		}
		
		List<Spatial> temp = new LinkedList<Spatial>(curPieces);
		for (Spatial sp: temp) {
			Vector3f endSpPos = sp.getWorldTranslation();
			if (endSpPos.subtract(camPos).length() > SPAWN_DISTANCE/2) {
				//2 because don't delete the ones we just placed
				getState(BulletAppState.class).getPhysicsSpace().remove(sp.getControl(0));
				rootNode.detachChild(sp);
				curPieces.remove(sp);
			} else {
				break; //this means only remove pieces in order
			}
		}
	}

	protected void placeNewPieces(Vector3f camPos) throws IllegalStateException {

		//get list of valid pieces (i.e. pieces with the correct node type)
		List<WPObject> wpoList = new ArrayList<>();
		for (WPObject w: wpos) {
			if (nextNode == null || nextNode == w.wp.startNode()) {
				wpoList.add(w);
			}
		}

		if (wpoList.isEmpty()) {
			throw new IllegalStateException("No pieces with the node start " + nextNode.name() + " found on type " + WPObject.class.getName());
		}

		//if there is no piece to place then this gets stuck in an infinite loop
		//probably because there is no piece that satisfies the angle constraint
		while (nextPos.subtract(camPos).length() < SPAWN_DISTANCE) {
            int i = rand.nextInt(wpoList.size());
			WPObject wpo = wpoList.get(i);

			if (testConstraints(wpo))
				placePiece(wpo);
			else
				wpoList.remove(wpo);

			if (wpoList.isEmpty())
                throw new IllegalStateException("No piece matched the constraints :(. Type: " + WPObject.class.getName());
		}
	}

	protected boolean testConstraints(WPObject wpo) {
		Quaternion newRot = this.nextRot.mult(wpo.wp.getNewAngle());
		
		// Prevent it turning back onto itself - try PI*3/2 if its not interesting enough
		Vector3f newForward = newRot.mult(Vector3f.UNIT_X);
		if (newForward.angleBetween(Vector3f.UNIT_X) > FastMath.PI)
			return false;
		
		// checking the difference from vertical helps from the weird ever increasing hill
		Vector3f newUp = newRot.mult(Vector3f.UNIT_Y);
		if (newUp.angleBetween(Vector3f.UNIT_Y) > 0.3f)
			return false;
		return true;
	}
	
	// Please attempt to use the wonderful getRotation().toAngles() and fromAngles() methods
	// which should stop the slowly getting off track if you just create the quaternion everytime
	// there will always be floating point errors though, unless we really care about that...
	protected void placePiece(WPObject wpo) {
		WP worldPiece = wpo.wp;
		Spatial s = wpo.sp.clone();
		CollisionShape coll = wpo.col;

		float scale = worldPiece.getScale();
		//translate, rotate, scale
		s.setLocalTranslation(nextPos);
		s.rotate(nextRot);
		s.scale(scale);

		RigidBodyControl landscape = new RigidBodyControl(coll, 0);
		landscape.setKinematic(false);
		s.addControl(landscape);

		getState(BulletAppState.class).getPhysicsSpace().add(landscape);
		rootNode.attachChild(s);

		curPieces.add(s);

		//setup the start for the next piece
		Vector3f cur = worldPiece.getNewPos().mult(scale);
		nextPos.addLocal(nextRot.mult(cur));

        nextRot.multLocal(worldPiece.getNewAngle());
		
		nextNode = worldPiece.endNode();
		count++;
	}
	
	public void reset() {
		for (Spatial s: curPieces) {
			getState(BulletAppState.class).getPhysicsSpace().remove(s.getControl(0));
			rootNode.detachChild(s);
        }
        curPieces.clear();

        nextPos = new Vector3f();
        nextRot = new Quaternion();
        nextNode = null;
	}

	@Override
	public Vector3f getStartPos() {
		return new Vector3f(0,1,0);
	}
	
	@Override
	public Quaternion getStartRot() {
		Quaternion rot = new Quaternion();
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
	
	@Override
	public void cleanup(Application app) {
        // TODO this removes things that are already not in the physics space
        PhysicsSpace space = getState(BulletAppState.class).getPhysicsSpace();
		for (Spatial s: curPieces) {
			space.remove(s.getControl(0));
			rootNode.detachChild(s);
        }
        space.remove(startGeometry);

		super.cleanup(app);
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
