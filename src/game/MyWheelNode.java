package game;

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

public class MyWheelNode extends Node {

	Spatial spat;
	
	static Texture tex;
	
	MyPhysicsVehicle mvc;
	int num;
	float radSec;
	
	Material mat;
	int[] indexes = { 2,0,1, 1,3,2 };
	
	boolean contact;
	Vector3f last;
	
	float skid;
	ParticleEmitter smoke;
	boolean ifSmoke = false;
	
	public MyWheelNode(String name, MyPhysicsVehicle mvc, int num) {
		super(name);
		tex = App.rally.getAssetManager().loadTexture("assets/stripes.png");
		
		this.skid = 1;
		this.mvc = mvc;
		this.num = num;
		this.last = new Vector3f(0,0,0);
		
		if (this.ifSmoke) {
			this.smoke = initSmoke();
			attachChild(this.smoke); //TODO fix rotate with tyre
		}
		this.setShadowMode(ShadowMode.Off);
	}
	
	public void update(float tpf, int rpm) {
		if (ifSmoke) {
			if (skid <= 0.1 && contact) {
				smoke.setEnabled(true);
			} else {
				smoke.setEnabled(false);
			}
		}
		
		if (spat == null) {
			try {
				throw new Exception("No wheel spatial, in wheelnode update");
			} catch (Exception e) {
			}
		}
		
		Quaternion q = new Quaternion();
		q = q.fromAngleNormalAxis(radSec*-tpf, new Vector3f(1,0,0));
		spat.setLocalRotation(spat.getLocalRotation().mult(q));
		
	}
	
	private ParticleEmitter initSmoke() {
		ParticleEmitter smoke = new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 40);
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
		return getLocalTranslation().add(0,-wheelRadius*rollInfluence,0);
	}
	
	public void addSkidLine() {
		if (contact) {
			addSkidLine(last, mvc.getWheel(num).getCollisionLocation(), (1-skid));
			last = mvc.getWheel(num).getCollisionLocation();
		} else {
			last = new Vector3f(0,0,0);
		}
	}
	
	private void addSkidLine(Vector3f a, Vector3f b, float grip) {
		if (a.equals(new Vector3f(0,0,0)) || b.equals(new Vector3f(0,0,0))) {
			return; //don't make a line because they aren't valid positions
		}
		a.y += 0.1;
		b.y += 0.1; //z-buffering (i.e. to stop it "fighting" with the ground)
		
		Mesh mesh = new Mesh(); //making a quad positions
		Vector3f [] vertices = new Vector3f[4];
		vertices[0] = a.add(mvc.right.mult(mvc.car.wheelWidth));
		vertices[1] = b.add(mvc.right.mult(mvc.car.wheelWidth));
		vertices[2] = a.add(mvc.left.mult(mvc.car.wheelWidth));
		vertices[3] = b.add(mvc.left.mult(mvc.car.wheelWidth));
		
		Vector2f[] texCoord = new Vector2f[4]; //texture of quad
		texCoord[0] = new Vector2f(0, 0);
		texCoord[1] = new Vector2f(0, 1);
		texCoord[2] = new Vector2f(1, 0);
		texCoord[3] = new Vector2f(1, 1);
		
		mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
		mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoord));
		mesh.setBuffer(Type.Index,    3, BufferUtils.createIntBuffer(indexes));

		mesh.updateBound();
		
		Geometry geo = new Geometry("MyMesh", mesh);
		
		Material mat = new Material(App.rally.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
		mat.setTexture("DiffuseMap", tex);
		mat.setBoolean("UseMaterialColors", true);
		
		mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		
		mat.setColor("Diffuse", new ColorRGBA(0,0,0,grip));
		
		geo.setMaterial(mat);
		geo.setShadowMode(ShadowMode.Off);
		geo.setQueueBucket(Bucket.Transparent);
		mvc.skidNode.attachChild(geo);
		mvc.skidList.add(geo);
	}
}