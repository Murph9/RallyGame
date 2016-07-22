package world;

import game.App;
import game.H;
import world.wp.Cliff;
import world.wp.Floating;
import world.wp.WP;
import world.wp.WP.NodeType;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.PlaneCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Plane;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.FogFilter;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.water.SimpleWaterProcessor;

public class WorldBuilder extends Node {

	//designed to generate the world infront of the player dynamically.

	//TODO's
	//hard - try and just make a curver to drive on instead of the loaded segments
	//bezier curve stuffs..

	public List<Spatial> curPieces = new LinkedList<Spatial>();
	private PhysicsSpace space;

	private List<WPObject> wpos;
	Material mat;

	boolean wantFog = false;
	Geometry fogGeom;
	
	boolean wantWater = true;
	Geometry water;

	public Vector3f start = new Vector3f(0,0,0);
	public Vector3f nextPos = new Vector3f(0,0,0);
	public Quaternion nextRot = new Quaternion();
	public NodeType nextNode = null;
	int count = 0;

	int totalPlaced = 0;
	float distance = 500; //TODO find a good number
	
	public WorldBuilder (WP[] type, PhysicsSpace space, ViewPort view) {
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

		
		this.setShadowMode(ShadowMode.CastAndReceive);

		Material matfloor = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		matfloor.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		matfloor.setColor("Color", ColorRGBA.Green);

		Box start = new Box(10, 0.25f, 10); //Something to spawn on (or in hint hint)
		Geometry startGeometry = new Geometry("Starting Box", start);
		startGeometry.setMaterial(matfloor);
		startGeometry.setLocalTranslation(0, -0.1f, 0);
		startGeometry.addControl(new RigidBodyControl(0));
		this.attachChild(startGeometry);
		this.space.add(startGeometry);


		if (type[0] instanceof Floating && wantWater) { //this is the nice floating looking one
			// we create a water processor
			SimpleWaterProcessor waterProcessor = new SimpleWaterProcessor(am);
			// we set wave properties
			waterProcessor.setRenderSize(256,256);    // performance boost because small (don't know the defaults)
			waterProcessor.setWaterDepth(40);         // transparency of water
			waterProcessor.setDistortionScale(0.5f);  // strength of waves (default = 0.2)
			waterProcessor.setWaveSpeed(0.05f);       // speed of waves
			waterProcessor.setWaterColor(ColorRGBA.Green); //TODO actually make it green

			waterProcessor.setReflectionScene(App.rally.getRootNode());

			// we set the water plane
			Vector3f waterLocation = new Vector3f(0,-6,0);
			waterProcessor.setPlane(new Plane(Vector3f.UNIT_Y, waterLocation.dot(Vector3f.UNIT_Y)));
			view.addProcessor(waterProcessor);

			// we define the wave size by setting the size of the texture coordinates
			Quad quad = new Quad(4000,4000);
			quad.scaleTextureCoordinates(new Vector2f(50f,50f));

			// we create the water geometry from the quad
			water = new Geometry("water", quad);
			water.setLocalRotation(new Quaternion().fromAngleAxis(-FastMath.HALF_PI, Vector3f.UNIT_X));
			water.setLocalTranslation(-2000, -4, 2000);
			water.setShadowMode(ShadowMode.Receive);
			water.setMaterial(waterProcessor.getMaterial());
			App.rally.getRootNode().attachChild(water);

			//add lastly a plane just under it so you don't fall forever
			//TODO suffers from collision precision issues
			Plane under = new Plane(Vector3f.UNIT_Y, waterLocation.add(0,1.75f,0).dot(Vector3f.UNIT_Y));
			PlaneCollisionShape p = new PlaneCollisionShape(under);

			RigidBodyControl underp = new RigidBodyControl(p, 0);
			underp.setKinematic(false);
			this.space.add(underp);
		}

		if (type[0] instanceof Cliff) {
			if (wantFog) {
				//make fog, TODO makes everything transparent...
				FilterPostProcessor fogPPS = new FilterPostProcessor(am);
				FogFilter fog = new FogFilter();
				fog.setFogDistance(50);
				fog.setFogDensity(2.0f);
				fogPPS.addFilter(fog);
				App.rally.getViewPort().addProcessor(fogPPS);
			}

			//TODO think about the material
//			Material fogmat = new Material(App.rally.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
//			Box fogBox = new Box(1f,0.1f,1f);
//			fogGeom = new Geometry("fog Box", fogBox);
//			fogGeom.scale(50);
//			fogGeom.setMaterial(fogmat);
//			fogGeom.setLocalTranslation(0, -10f, 0);
//			fogGeom.setShadowMode(ShadowMode.Off);
//			this.attachChild(fogGeom);
		}
	}

	public void update(Vector3f playerPos) {
		count++;
		if (count % 10 == 0) {
			while (nextPos.subtract(playerPos).length() < distance) {
				addNewPiece();
			}

			List<Spatial> temp = new LinkedList<Spatial>(curPieces);
			for (Spatial sp: temp) {
				Vector3f endSpPos = sp.getWorldTranslation();
				if (endSpPos.subtract(playerPos).length() > distance/2) {
					//2 because don't delete the ones we just placed
					this.space.remove(sp.getControl(0));
					this.detachChild(sp);
					curPieces.remove(sp);
				} else {
					break; //this means only remove pieces in order
				}
			}
		}
		
	}


	private void addNewPiece() {
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

		WP world = wpo.wp;
		Spatial s = wpo.sp.clone();
		CollisionShape coll = wpo.col;

		Quaternion inv = nextRot.mult(world.getNewAngle()).inverse();
		Quaternion result = Quaternion.IDENTITY.mult(inv);
		float angle = FastMath.acos(result.getW())*2; //believe me that this gets the angle between them

		if (!(FastMath.abs(angle) < FastMath.PI)) { //try *3/2 if its not interesting enough
			return;
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
		this.attachChild(s);

		curPieces.add(s);
		totalPlaced++;

		//setup the start for the next piece
		Vector3f cur = world.getNewPos().mult(scale);
		nextPos.addLocal(nextRot.mult(cur));
		
		Quaternion rot = world.getNewAngle();
		rot.set(rot);
		nextRot.multLocal(rot);
		
		nextNode = world.endNode();
	}

	public int getTotalPlaced() {
		return totalPlaced;
	}


	private class WPObject {
		WP wp;
		Spatial sp;
		CollisionShape col;
	}
	
	
	public static Spatial curPiece;
	public static void placeOnePiece(WP wp) {
		if (curPiece != null) {
			App.rally.getRootNode().detachChild(curPiece); 
		}
		
		if (wp == null) {
			H.e("Why was 'wp' null?");
			return;
		}
		
		AssetManager am = App.rally.getAssetManager();
				
		curPiece = am.loadModel(wp.getName());
		curPiece.setCullHint(CullHint.Never);
		Spatial sp = ((Node)curPiece).getChild(0);
		
		boolean needsMat = wp.needsMaterial();
		Material mat;
		if (needsMat) {
			mat = new Material(am, "Common/MatDefs/Misc/ShowNormals.j3md");
			mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
			sp.setMaterial(mat);
		}
		
		App.rally.getRootNode().attachChild(curPiece);
	}
}