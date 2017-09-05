package car;

import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

import game.App;

//TODO smoke from tyres

public class MyWheelNode extends Node {

	private static final int[] indexes = { 2,0,1, 1,3,2 }; //tyre marks vertex order
	private static final Vector2f[] texCoord = new Vector2f[] { //texture of quad with order
		new Vector2f(0, 0), new Vector2f(0, 1), new Vector2f(1, 0), new Vector2f(1, 1),
	};

	public Spatial spat; //the actual wheel model

	private MyPhysicsVehicle mvc;
	public int num;
	
	public boolean contact;
	public float radSec;

	private ColorRGBA lastColor;
	private Vector3f lastl;
	private Vector3f lastr;
	
	public float susForce;
	public Vector3f gripDir;
	public float skid;
	
	public MyWheelNode(String name, MyPhysicsVehicle mvc, int num) {
		super(name);
		
		this.skid = 1;
		this.mvc = mvc;
		this.num = num;
		this.lastl = new Vector3f(0,0,0);
		this.lastr = new Vector3f(0,0,0);
		
		this.setShadowMode(ShadowMode.Cast);
	}
	
	public void update(float tpf, int reverse) {
		reverse = FastMath.sign(reverse);

		Quaternion q = new Quaternion();
		q = q.fromAngleNormalAxis(-radSec*tpf*reverse, new Vector3f(1,0,0));
		spat.setLocalRotation(spat.getLocalRotation().mult(q));
	}
	
	public Vector3f getContactPoint(float wheelRadius, float rollInfluence) {
		// a faked collision location based on how much the car should roll
		// see getWheel(<int>).getCollisionLocation() for the real one
		return getLocalTranslation().add(0,-wheelRadius*rollInfluence,0);
	}
	
	public void addSkidLine() {
		if (!contact) {
			lastl = new Vector3f(0,0,0);
			lastr = new Vector3f(0,0,0);
			lastColor = null;
			return;
		} //exit early

		Vector3f cur = mvc.getWheel(num).getCollisionLocation();
		if (lastl.equals(new Vector3f(0,0,0)) || lastr.equals(new Vector3f(0,0,0)) || cur.equals(new Vector3f(0,0,0))) {
			lastl = cur; 
			lastr = cur;
			lastColor = null;
			return; //don't make a line because they aren't valid positions
		}

		//TODO better scaling on grip value
		float clampSkid = FastMath.clamp(skid - 0.85f, 0, 1);
		if (clampSkid < 0.1f) {
			lastl = cur;
			lastr = cur;
			lastColor = null;
			return; //don't make a line because they aren't valid positions
		} //exit early if there is no mesh to create
		
		ColorRGBA c = new ColorRGBA(0,0,0,clampSkid);
		
		cur.y += 0.02f; //z-buffering (i.e. to stop it "fighting" with the ground texture)
		
		Mesh mesh = new Mesh(); //making a quad positions
		Vector3f [] vertices = new Vector3f[4];
		Vector3f rot = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y).mult(mvc.vel.normalize());
		vertices[0] = lastl;
		lastl = vertices[1] = cur.add(rot.mult(mvc.car.w_width));
		vertices[2] = lastr;
		lastr = vertices[3] = cur.add(rot.negate().mult(mvc.car.w_width));
		
		mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
		mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoord));
		mesh.setBuffer(Type.Index,    3, BufferUtils.createIntBuffer(indexes));

		mesh.setBuffer(Type.Color, 4, BufferUtils.createFloatBuffer(lastColor, c, lastColor, c));
		lastColor = c;
		
		mesh.updateBound();
		Geometry geo = new Geometry("MyMesh", mesh);
		
		
		
		Material mat = new Material(App.rally.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		mat.setBoolean("VertexColor", true);
//		mat.setColor("Color", ); //TODO vertex color so its gradient from this to the previous value
		mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
		geo.setShadowMode(ShadowMode.Off);
		geo.setQueueBucket(Bucket.Transparent);
		

		geo.setMaterial(mat);
		
		mvc.skidNode.attachChild(geo);
		mvc.skidList.add(geo);
	}
	
	public void reset() {
		lastl = new Vector3f(0,0,0);
		lastr = new Vector3f(0,0,0);
	}
}