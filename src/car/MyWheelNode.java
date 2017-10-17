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
import helper.H;

//TODO smoke from tyres

public class MyWheelNode extends Node {

	private static final int QUAD_COUNT = 200;
	
	private static final int VERTEX_BUFFER_SIZE = 4*QUAD_COUNT; //Vector3f.size * triangle size * 2 (2 tri per quad) * count
	private static final int[] indexes = { 2,0,1, 1,3,2 }; //tyre marks vertex order
	private static final Vector2f[] texCoord = new Vector2f[] { //texture of quad with order
		new Vector2f(0, 0), new Vector2f(0, 1), new Vector2f(1, 0), new Vector2f(1, 1),
	};
	private Geometry SkidLine;
	private Vector3f[] vertices;
	private int verticesPos;
	private ColorRGBA[] colors;

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
		
		this.SkidLine = new Geometry();
		Mesh mesh = new Mesh(); //making a quad positions
		vertices = new Vector3f[VERTEX_BUFFER_SIZE];
		verticesPos = 0;
		mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
		colors = new ColorRGBA[VERTEX_BUFFER_SIZE];
		mesh.setBuffer(Type.Color, 4, BufferUtils.createFloatBuffer(colors));
		
		int[] i_s = new int[6*QUAD_COUNT];
		int j = 0;
		for (int i = 0; i < i_s.length; i += 6) {
			i_s[i+0] = j*4+indexes[0];
			i_s[i+1] = j*4+indexes[1];
			i_s[i+2] = j*4+indexes[2];
			i_s[i+3] = j*4+indexes[3];
			i_s[i+4] = j*4+indexes[4];
			i_s[i+5] = j*4+indexes[5];
			j++;
		}
		
		mesh.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(i_s)); 
		
		Vector2f[] coord = new Vector2f[4*QUAD_COUNT]; //TODO
		mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(coord)); 
		
		mesh.setBuffer(Type.Color, 4, BufferUtils.createFloatBuffer(ColorRGBA.BlackNoAlpha, ColorRGBA.BlackNoAlpha, ColorRGBA.BlackNoAlpha, ColorRGBA.BlackNoAlpha));
		this.SkidLine.setMesh(mesh);
		
		Material mat = new Material(App.rally.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		mat.setBoolean("VertexColor", true);
		mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);

		this.SkidLine.setMaterial(mat);
		
		this.SkidLine.setShadowMode(ShadowMode.Off);
		this.SkidLine.setQueueBucket(Bucket.Transparent);
		
		App.rally.getRootNode().attachChild(this.SkidLine);
	}
	
	public void update(float tpf, int reverse) {
		reverse = FastMath.sign(reverse);

		Quaternion q = new Quaternion();
		q = q.fromAngleNormalAxis(-radSec*tpf*reverse, new Vector3f(1,0,0));
		spat.setLocalRotation(spat.getLocalRotation().mult(q));
		
		if (App.rally.frameCount % 3 == 0) {
			addSkidLine();
			//TODO set the current quads next pos to be where the wheels are
			//this is to prevent the weird flickering
		}
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
		if (lastl.equals(Vector3f.ZERO) || lastr.equals(Vector3f.ZERO) || cur.equals(Vector3f.ZERO)) {
			lastl = cur; 
			lastr = cur;
			lastColor = null;
			return; //don't make a line because they aren't valid positions
		}

		//TODO better scaling on grip value
		float clampSkid = FastMath.clamp((skid - 0.85f)/2, 0, 1);
		if (clampSkid < 0.1f) {
			lastl = cur;
			lastr = cur;
			lastColor = null;
			return; //don't make a line because they aren't valid positions
		} //exit early if there is no mesh to create
		
		ColorRGBA c = new ColorRGBA(0,0,0,clampSkid);
		cur.y += 0.005f; //z-buffering (i.e. to stop it "fighting" with the ground texture)
		
		//TODO change to just be smaller on a large drift angle (look at most wanted's skid marks)
		
		Vector3f rot = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y).mult(mvc.vel.normalize());
		colors[verticesPos] = lastColor;
		vertices[verticesPos++] = lastl;
		lastl = cur.add(rot.mult(mvc.car.w_width));
		colors[verticesPos] = c;
		vertices[verticesPos++] = lastl;
		
		colors[verticesPos] = lastColor;
		vertices[verticesPos++] = lastr;
		lastr = cur.add(rot.negate().mult(mvc.car.w_width));
		colors[verticesPos] = c;
		vertices[verticesPos++] = lastr;
		
		verticesPos = verticesPos % VERTEX_BUFFER_SIZE; //wrap around vertex pos
		
		Mesh mesh = this.SkidLine.getMesh();
		mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
		mesh.setBuffer(Type.Color, 4, BufferUtils.createFloatBuffer(colors));
		lastColor = c;
		
		this.SkidLine.updateModelBound();
	}
	
	public void reset() {
		lastl = new Vector3f(0,0,0);
		lastr = new Vector3f(0,0,0);
		
		//TODO reset skid buffers?
	}

	public void cleanup() {
		App.rally.getRootNode().detachChild(this.SkidLine);
	}
}