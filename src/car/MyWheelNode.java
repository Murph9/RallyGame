package car;

import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
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
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;

import game.App;

@SuppressWarnings("unused")
public class MyWheelNode extends Node {

	public static final boolean ifSmoke = false;
	
	private static final int[] indexes = { 2,0,1, 1,3,2 }; //tyre marks vertex order
	private static final Vector2f[] texCoord = new Vector2f[] { //texture of quad with order
		new Vector2f(0, 0), new Vector2f(0, 1), new Vector2f(1, 0), new Vector2f(1, 1),
	};
	private static Texture skidTex = App.rally.getAssetManager().loadTexture("assets/image/stripes.png"); //texture for the tyre marks
	
	Spatial spat; //the actual wheel model

	MyPhysicsVehicle mvc;
	int num;
	
	boolean contact;
	private Vector3f last;
	float radSec;

	public float susForce;
	public Vector3f gripDir;
	public float skid;
	ParticleEmitter smokEmit;
	
	public MyWheelNode(String name, MyPhysicsVehicle mvc, int num) {
		super(name);
		
		this.skid = 1;
		this.mvc = mvc;
		this.num = num;
		this.last = new Vector3f(0,0,0);
		
		if (ifSmoke) {
			this.smokEmit = initSmoke();
			smokEmit.setEnabled(true);
			attachChild(this.smokEmit); //TODO fix rotate with tyre
		}
		this.setShadowMode(ShadowMode.Cast);
	}
	
	public void update(float tpf, int reverse) {
		if (ifSmoke) {
			if (skid > 1.5 && contact) {
				smokEmit.setParticlesPerSec(10);
			} else {
				smokEmit.setParticlesPerSec(0);
			}
		}
		reverse = FastMath.sign(reverse);

		Quaternion q = new Quaternion();
		q = q.fromAngleNormalAxis(-radSec*tpf*reverse, new Vector3f(1,0,0));
		spat.setLocalRotation(spat.getLocalRotation().mult(q));
	}
	
	private ParticleEmitter initSmoke() {
		ParticleEmitter smoke = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 40);
		smoke.setNumParticles(90);
		smoke.setParticlesPerSec(10);
		smoke.setInWorldSpace(true);
		smoke.setRotateSpeed(FastMath.QUARTER_PI);
		
		smoke.setImagesX(15); //the smoke image is 15x * 1y (y is already the default of 1)
		smoke.setEndColor(new ColorRGBA(0.7f, 0.7f, 0.7f, 0.2f));
		smoke.setStartColor(new ColorRGBA(0.7f, 0.7f, 0.7f, 0.7f));

		smoke.setStartSize(0.4f);
		smoke.setEndSize(10f);
		
		//TODO to use these move the emitter to something that doesn't rotate
//		smoke.getParticleInfluencer().setInitialVelocity(new Vector3f(0,9,0));
//		smoke.getParticleInfluencer().setVelocityVariation(0.05f);

	    Material emit = new Material(App.rally.getAssetManager(), "Common/MatDefs/Misc/Particle.j3md");
	    emit.setTexture("Texture", App.rally.getAssetManager().loadTexture("Effects/Smoke/Smoke.png"));
	    smoke.setMaterial(emit);
    	return smoke;
	}

	public Vector3f getContactPoint(float wheelRadius, float rollInfluence) {
		// a faked collision location based on how much the car should roll
		// see getWheel(<int>).getCollisionLocation() for the real one
		return getLocalTranslation().add(0,-wheelRadius*rollInfluence,0);
	}
	
	public void addSkidLine() {
		if (!contact) {
			last = new Vector3f(0,0,0);
			return;
		} //exit early

		Vector3f cur = mvc.getWheel(num).getCollisionLocation();
		if (last.equals(new Vector3f(0,0,0)) || cur.equals(new Vector3f(0,0,0))) {
			last = cur;
			return; //don't make a line because they aren't valid positions
		}
		cur.y += 0.02f; //z-buffering (i.e. to stop it "fighting" with the ground)
		
		float clampSkid = FastMath.clamp(skid - 0.85f, 0, 1);
		if (clampSkid < 0.01f) {
			last = cur;
			return; //don't make a line because they aren't valid positions
		} //exit early if there is no mesh to create
		
		Mesh mesh = new Mesh(); //making a quad positions
		Vector3f [] vertices = new Vector3f[4];
		vertices[0] = last.add(mvc.right.mult(mvc.car.w_width));
		vertices[1] = cur.add(mvc.right.mult(mvc.car.w_width));
		vertices[2] = last.add(mvc.left.mult(mvc.car.w_width));
		vertices[3] = cur.add(mvc.left.mult(mvc.car.w_width));
		
		mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
		mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoord));
		mesh.setBuffer(Type.Index,    3, BufferUtils.createIntBuffer(indexes));

		mesh.updateBound();
		Geometry geo = new Geometry("MyMesh", mesh);
		
		//TODO better scaling on grip value
		Material mat = new Material(App.rally.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		mat.setColor("Color", new ColorRGBA(0,0,0,clampSkid));
		mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
		geo.setShadowMode(ShadowMode.Off);
		geo.setQueueBucket(Bucket.Transparent);

		geo.setMaterial(mat);
		
		mvc.skidNode.attachChild(geo);
		mvc.skidList.add(geo);
		
		last = cur;
	}
	
	public void reset() {
		last = new Vector3f(0,0,0);
	}
}