package rallygame.world.wp;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

import rallygame.effects.LoadModelWrapper;
import rallygame.helper.H;
import rallygame.helper.Rand;
import rallygame.world.World;
import rallygame.world.WorldType;
import rallygame.world.wp.WP.NodeType;

/** Generates the world infront of the player dynamically from static set pieces */
public abstract class DefaultBuilder extends World {

    protected static final float SPAWN_DISTANCE = 1000;

	protected final WP[] type;
    protected final List<Spatial> curPieces;
    private Supplier<Vector3f[]> objectDistFunction;
    
    private Spatial startGeometry;
    protected List<WPObject> wpos;
	
	protected Vector3f nextPos;
	protected Quaternion nextRot;
	protected NodeType nextNode;
	
	protected Random rand;
	
	protected int count = 0;
    
    protected IPieceChanged changedListener;

	DefaultBuilder(WP[] type) {
		this(type, (long)FastMath.nextRandomInt());
    }
	DefaultBuilder(WP[] type, long seed) {
		super("builder root node");
        this.type = type;
        this.curPieces = new ArrayList<Spatial>();
        this.rand = new Random(seed);

        this.objectDistFunction = defaultDistFunction();
        reset();
    }
    
    public void reset() {
        for (Spatial s : curPieces) {
            getState(BulletAppState.class).getPhysicsSpace().remove(s);
            rootNode.detachChild(s);
        }
        curPieces.clear();

        nextPos = new Vector3f();
        nextRot = new Quaternion();
        nextNode = null;
    }
    
    public void setDistFunction(Supplier<Vector3f[]> objectDistFunction) {
        this.objectDistFunction = objectDistFunction;
    }

	@Override
	public void initialize(Application app) {
		super.initialize(app);
		
		this.wpos = new ArrayList<WPObject>();
		for (int i = 0; i < type.length; i++) {
			WPObject wpo = new WPObject();
			wpo.wp = type[i];
			wpo.sp = LoadModelWrapper.create(app.getAssetManager(), wpo.wp.getFileName());

			//scale and unscale spatials so that the collision shape size is correct
			wpo.sp.scale(wpo.wp.getScale());
			wpo.col = CollisionShapeFactory.createMeshShape(wpo.sp);
			wpo.sp.scale(1 / wpo.wp.getScale());
            
			this.wpos.add(wpo);
		}
		
		Box start = new Box(10, 0.25f, 10); //Something to spawn on (or in hint hint)
		startGeometry = new Geometry("Starting Box", start);
		startGeometry = LoadModelWrapper.createWithColour(app.getAssetManager(), startGeometry, ColorRGBA.Green);
		startGeometry.setLocalTranslation(0, -0.1f, 0);
		startGeometry.addControl(new RigidBodyControl(0));
		
		this.rootNode.attachChild(startGeometry);
		getState(BulletAppState.class).getPhysicsSpace().add(startGeometry);
	}

	@Override
	public void update(float tpf) {
		Vector3f[] posList = objectDistFunction.get();

		placeNewPieces(posList);
        removePieces(posList);
	}

    private boolean oneCloserThen(Vector3f from, float distance, Vector3f[] list) {
        return H.oneTrue((pos) -> from.distance(pos) < distance, list);
    }
    private boolean allFurtherAwayThen(Vector3f from, float distance, Vector3f[] list) {
        return H.allTrue((pos) -> from.distance(pos) > distance, list);
    }
    
	protected void placeNewPieces(Vector3f[] posList) {

		//get list of valid pieces (i.e. pieces with the correct node type)
		List<WPObject> wpoList = new LinkedList<>();
		for (WPObject w: wpos) {
			if (nextNode == null || nextNode == w.wp.startNode()) {
				wpoList.add(w);
			}
		}
		if (wpoList.isEmpty())
			throw new IllegalStateException("No pieces with the node start " + nextNode.name() + " found on type " + WPObject.class.getName());

		//if there is no piece to place then this gets stuck in an infinite loop
        //probably because there is no piece that satisfies the angle constraint
		while (oneCloserThen(nextPos, SPAWN_DISTANCE, posList)) {
            WPObject wpo = Rand.randFromList(wpoList);

			if (testConstraints(wpo)) {
                Vector3f pos = placePiece(wpo);
                if (changedListener != null)
                    changedListener.pieceAdded(pos);
            } else
				wpoList.remove(wpo);

			if (wpoList.isEmpty())
                throw new IllegalStateException("No piece matched the constraints :( - Type: " + WPObject.class.getName());
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
		if (newUp.angleBetween(Vector3f.UNIT_Y) > 0.2f)
			return false;
		return true;
	}
	
	// Please attempt to use the wonderful getRotation().toAngles() and fromAngles() methods
	// which should stop the slowly getting off track if you just create the quaternion everytime
	// there will always be floating point errors though, unless we really care about that...
	protected Vector3f placePiece(WPObject wpo) {
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
        
        return s.getWorldTranslation();
	}
    
    protected void removePieces(Vector3f[] posList) {
        List<Spatial> temp = new LinkedList<Spatial>(curPieces);
        PhysicsSpace space = getState(BulletAppState.class).getPhysicsSpace();
        for (Spatial sp : temp) {
            Vector3f endSpPos = sp.getWorldTranslation();
            if (allFurtherAwayThen(endSpPos, SPAWN_DISTANCE / 2, posList)) {
                // '/2' because don't delete the ones we just placed
                
                space.remove(sp);
                rootNode.detachChild(sp);
                curPieces.remove(sp);

                if (changedListener != null)
                    changedListener.pieceRemoved(endSpPos);

            } else {
                break; // this means only remove pieces in order
            }
        }
    }

	@Override
	public Transform getStart() {
		Quaternion rot = new Quaternion();
		rot.fromAngleAxis(FastMath.DEG_TO_RAD * 90, Vector3f.UNIT_Y);
		return new Transform(new Vector3f(0, 1, 0), rot);
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
        PhysicsSpace space = getState(BulletAppState.class).getPhysicsSpace();
		for (Spatial sp: curPieces) {
			space.remove(sp);
			rootNode.detachChild(sp);
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
    
    public void registerListener(IPieceChanged changedListener) {
        if (this.changedListener != null)
            throw new IllegalStateException("Already has a listener");

        this.changedListener = changedListener;
    }

    public interface IPieceChanged {
        void pieceAdded(Vector3f pos);
        void pieceRemoved(Vector3f pos);
    }

    //NOTE: this method is at the bottom of the file because it screws with code editing styles in VSCode
    private Supplier<Vector3f[]> defaultDistFunction() {
        return (() -> new Vector3f[] { getApplication().getCamera().getLocation() });
    }
}
