package world.wp;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.jme3.asset.AssetManager;
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
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Spatial.CullHint;
import com.jme3.scene.shape.Box;

import game.App;
import world.wp.WP.DynamicBuilder;
import world.wp.WP.NodeType;

public abstract class DefaultBuilder implements DynamicBuilder {

	//designed to generate the world infront of the player dynamically.

	//TODO's
	//hard - try and just make a curver to drive on instead of the loaded segments
	//bezier curve stuffs..
	
	protected WP[] type;
	protected PhysicsSpace space;
	
	protected Node rootNode;
	
	protected List<Spatial> curPieces = new ArrayList<Spatial>();
	protected List<WPObject> wpos;
	protected Material mat;
	
	protected Vector3f start = new Vector3f();
	protected Vector3f nextPos = new Vector3f();
	protected Quaternion nextRot = new Quaternion();
	protected NodeType nextNode = null;
	
	protected int count = 0;
	protected float distance = 500; //TODO find a good number
	
	DefaultBuilder(WP[] type) {
		this.type = type;
		this.rootNode = new Node("builder root node");
		rootNode.setShadowMode(ShadowMode.CastAndReceive); //performance concern 1
	}
	
	@Override
	public void init(PhysicsSpace space, ViewPort view) {
		this.space = space;
		
		AssetManager am = App.rally.getAssetManager();
		boolean mat = type[0].needsMaterial();
		if (mat) {
			this.mat = new Material(am, "Common/MatDefs/Misc/ShowNormals.j3md");		
			this.mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		}
		
		this.wpos = new ArrayList<WPObject>();
		for (int i = 0; i < type.length; i++) {
			WPObject wpo = new WPObject();
			wpo.wp = type[i];
			
			Spatial piece = am.loadModel(type[i].getName());
			piece.setCullHint(CullHint.Never);
			wpo.sp = ((Node)piece).getChild(0); //there is only one object in there (hopefully)
			if (this.mat != null) {
				wpo.sp.setMaterial(this.mat); //TODO double sided objects
			}

			//scale and unscale spatials so that the collision shape size is correct
			wpo.sp.scale(type[i].getScale());
			wpo.col = CollisionShapeFactory.createMeshShape(wpo.sp);
			wpo.sp.scale(1/type[i].getScale());

			this.wpos.add(wpo);
		}
		
		Material matfloor = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		matfloor.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		matfloor.setColor("Color", ColorRGBA.Green);
		
		Box start = new Box(10, 0.25f, 10); //Something to spawn on (or in hint hint)
		Geometry startGeometry = new Geometry("Starting Box", start);
		startGeometry.setMaterial(matfloor);
		startGeometry.setLocalTranslation(0, -0.1f, 0);
		startGeometry.addControl(new RigidBodyControl(0));
		
		this.rootNode.attachChild(startGeometry);
		this.space.add(startGeometry);
	}

	@Override
	public void update(Vector3f playerPos, boolean force) {
		count++;
		if (!force && count % 10 != 0) return; //only on every 10th frame to save lag
		
		while (nextPos.subtract(playerPos).length() < distance) {
			selectNewPiece();
		}
		
		List<Spatial> temp = new LinkedList<Spatial>(curPieces);
		for (Spatial sp: temp) {
			Vector3f endSpPos = sp.getWorldTranslation();
			if (endSpPos.subtract(playerPos).length() > distance/2) {
				//2 because don't delete the ones we just placed
				this.space.remove(sp.getControl(0));
				rootNode.detachChild(sp);
				curPieces.remove(sp);
			} else {
				break; //this means only remove pieces in order
			}
		}
	}

	protected void selectNewPiece() {
		//get list of valid pieces (i.e. pieces with the correct node type)
		List<WPObject> wpoList = new ArrayList<>();
		for (WPObject w: wpos) {
			if (nextNode == null || nextNode == w.wp.startNode()) {
				wpoList.add(w);
			}
		}

		if (wpoList.isEmpty()) { 
			try {
				throw new Exception("No pieces with the node start " + nextNode.name() + " found.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		int i = (int)(Math.random()*wpoList.size());
		WPObject wpo = wpoList.get(i);
		int count = 0;
		while (!PlacePiece(wpo)) {
			i = (int)(Math.random()*wpoList.size()); //so select a new one
			count++;
			if (count > 100) {
				break; //please no loops huh?
			}
		}
	}
	
	protected boolean PlacePiece(WPObject wpo) {
		WP world = wpo.wp;
		Spatial s = wpo.sp.clone();
		CollisionShape coll = wpo.col;

		Quaternion inv = nextRot.mult(world.getNewAngle()).inverse();
		Quaternion result = Quaternion.IDENTITY.mult(inv);
		float angle = FastMath.acos(result.getW())*2; //believe me that this gets the angle between them

		if (!(FastMath.abs(angle) < FastMath.PI)) { //try *3/2 if its not interesting enough
			return false;
		}

		float scale = world.getScale();
		//translate, rotate, scale
		s.setLocalTranslation(nextPos);
		s.rotate(nextRot);
		s.scale(scale);

		RigidBodyControl landscape = new RigidBodyControl(coll, 0);
		landscape.setKinematic(false);
		s.addControl(landscape);

		this.space.add(landscape);
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
			space.remove(s.getControl(0));
			rootNode.detachChild(s);
			curPieces.remove(s);
		}

		start = new Vector3f(0,0,0);
		nextPos = new Vector3f(0,0,0);
		nextRot = new Quaternion();
	}

	@Override
	public Spatial getRootNode() {
		return rootNode;
	}

	@Override
	public Vector3f getWorldStart() {
		return new Vector3f(0,1,0); //TODO better
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
	
	protected class WPObject {
		WP wp;
		Spatial sp;
		CollisionShape col;
	}
}