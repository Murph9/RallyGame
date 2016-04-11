package world;

import game.App;
import game.Rally;

import java.util.LinkedList;
import java.util.List;

import com.jme3.asset.AssetManager;
import com.jme3.asset.TextureKey;
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
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Quad;
import com.jme3.util.SkyFactory;
import com.jme3.water.SimpleWaterProcessor;

public class WorldBuilder extends Node {

	//designed to generate the world infront of the player dynamically.
	
	public static final Quaternion 
		STRIAGHT = Quaternion.IDENTITY,
		LEFT_90 = new Quaternion(0, 0.7071f, 0, 0.7071f),
		LEFT_45 = new Quaternion(0, 0.3827f, 0, 0.9239f),
		
		RIGHT_90 = new Quaternion(0, -0.7071f, 0, 0.7071f),
		RIGHT_45 = new Quaternion(0, -0.3827f, 0, 0.9239f)
		;
	
	//TODO's
	//hard - try and just make a curver to drive on instead of the loaded segments
		//bezier curve stuffs..
	
	public List<Spatial> curPieces = new LinkedList<Spatial>();

	private WP[] wbs;
	private Spatial[] spats; //yay magic array indexes
	private CollisionShape[] colls;
	Material mat;

	boolean wantWater = true;
	Geometry water;
	
	public Vector3f start = new Vector3f(0,0,0);
	public Vector3f nextPos = new Vector3f(0,0,0);
	public Quaternion nextRot = new Quaternion();
	int count = 0;

	int totalPlaced = 0;
	float distance = 150;
	final float max_wp = 50;
	
	public WorldBuilder (WP[] type, ViewPort view, boolean mat) {
		this.wbs = type; //kind of keeps this class generic
		if (mat) {
			this.mat = new Material(App.rally.getAssetManager(), "Common/MatDefs/Misc/ShowNormals.j3md");		
			this.mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		}
		
		this.spats = new Spatial[wbs.length];
		this.colls = new CollisionShape[wbs.length];
		for (int i = 0; i < wbs.length; i++) {
			Spatial piece = App.rally.getAssetManager().loadModel(wbs[i].getName());
			Spatial s = ((Node)piece).getChild(0); //there is only one object in there (hopefully)
			if (this.mat != null) {
				s.setMaterial(this.mat); //TODO double sided objects
			}
			
			spats[i] = s; //pre load spatials
			
			colls[i] = CollisionShapeFactory.createMeshShape(s);
			
		}
		
		this.setShadowMode(ShadowMode.CastAndReceive);
		
        Material matfloor = new Material(App.rally.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        matfloor.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        matfloor.setColor("Color", ColorRGBA.Green);
        
        Box start = new Box(10, 0.25f, 10); //Something to spawn on (or in hint hint)
        Geometry startGeometry = new Geometry("Starting Box", start);
        startGeometry.setMaterial(matfloor);
        startGeometry.setLocalTranslation(0, -0.1f, 0);
        startGeometry.addControl(new RigidBodyControl(0));
        this.attachChild(startGeometry);
        App.rally.getPhysicsSpace().add(startGeometry);

        
        App.rally.getRootNode().attachChild(SkyFactory.createSky(App.rally.getAssetManager(), "Textures/Sky/Bright/BrightSky.dds", false));
        boolean mine = false; //TODO my try (note, the images need to rotate around one, so that the sky and floor match
        if (mine) {
	        TextureKey keye = new TextureKey("assets/east.png", true);
	        TextureKey keyw = new TextureKey("assets/west.png", true);
	        TextureKey keyn = new TextureKey("assets/north.png", true);
	        TextureKey keys = new TextureKey("assets/south.png", true);
	        TextureKey keyt = new TextureKey("assets/top.png", true);
	        TextureKey keyb = new TextureKey("assets/bottom.png", true);
	        App.rally.getRootNode().attachChild(SkyFactory.createSky(App.rally.getAssetManager(), App.rally.getAssetManager().loadTexture(keyw), 
	        		App.rally.getAssetManager().loadTexture(keye), App.rally.getAssetManager().loadTexture(keyn), 
	        		App.rally.getAssetManager().loadTexture(keys), App.rally.getAssetManager().loadTexture(keyt), 
	        		App.rally.getAssetManager().loadTexture(keyb)));
        }
        
        if (type[0] instanceof Floating && wantWater) { //this is the nice floating looking one
        	// we create a water processor
        	SimpleWaterProcessor waterProcessor = new SimpleWaterProcessor(App.rally.getAssetManager());
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
        	///* TODO
        	Plane under = new Plane(Vector3f.UNIT_Y, waterLocation.add(0,1.75f,0).dot(Vector3f.UNIT_Y));
        	PlaneCollisionShape p = new PlaneCollisionShape(under);
        	
        	RigidBodyControl underp = new RigidBodyControl(p, 0);
//        	p.addControl(underp);
    		underp.setKinematic(false);
    		App.rally.getPhysicsSpace().add(underp);
//    		this.attachChild(p);
    		//*/
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
				if (sp.getWorldTranslation().subtract(playerPos).length() > distance/2) {
						//2 because don't delete the ones we just placed
					App.rally.getPhysicsSpace().remove(sp.getControl(0));
					this.detachChild(sp);
					curPieces.remove(sp);
					System.err.println("Removing: "+sp.getName() + ", num left: "+curPieces.size());
				
				} else {
					break; //this means only remove in order
				}
			}
		}
	}
	
	
	private void addNewPiece() {
		int i = (int)(Math.random()*wbs.length);
		Quaternion inv = nextRot.mult(wbs[i].getNewAngle()).inverse();
		Quaternion result = Quaternion.IDENTITY.mult(inv);
		float angle = FastMath.acos(result.getW())*2; //believe me that this gets the angle between them
		
		if (!(FastMath.abs(angle) < FastMath.PI)) { //try *3/2 if its not interesting enough
			return;
		}
		WP world = wbs[i];
		Spatial s = spats[i].clone();
		CollisionShape coll = colls[i];
		
		float scale = world.getScale();
		//translate, rotate, scale
		s.setLocalTranslation(nextPos);
		s.rotate(nextRot);
		s.scale(scale);
		
		RigidBodyControl landscape = new RigidBodyControl(coll, 0);
		landscape.setKinematic(false);
		s.addControl(landscape);
		
		App.rally.getPhysicsSpace().add(landscape);
		this.attachChild(s);

		System.err.println("Adding: "+world.getName() + ", at: " + nextPos);
		if (App.rally.ifDebug) {
			System.err.println("at: "+nextPos+", Rot: "+nextRot+", Obj.angle: "+world.getNewAngle()+", Obj.nextPos: "+world.getNewPos());
		}
		curPieces.add(s);
		
		totalPlaced++;

		
		//setup the position of the next object
		Vector3f cur = world.getNewPos().mult(scale);
		nextPos.addLocal(nextRot.mult(cur));
		
		nextRot.multLocal(world.getNewAngle());
	}
	
	public int getTotalPlaced() {
		return totalPlaced;
	}
}
