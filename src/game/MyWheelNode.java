package game;

import com.jme3.effect.ParticleEmitter;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;

public class MyWheelNode extends Node {

	MyVehicleControl mvc;
	int num;
	
	Material mat;
	int[] indexes = { 2,0,1, 1,3,2 };
	
	boolean contact;
	Vector3f last;
	
	float skid;
	ParticleEmitter smoke;
	
	public MyWheelNode(String name, MyVehicleControl mvc, int num) {
		super(name);
		this.mvc = mvc;
		this.num = num;
		this.last = new Vector3f(new Vector3f(0,0,0));
		
		this.setShadowMode(ShadowMode.CastAndReceive);
	}

	public Vector3f getForceLocation(float wheelRadius, float rollInfluence) {
		return getLocalTranslation().add(0,-wheelRadius*rollInfluence,0);
	}
	
	public void addSkidLine() {
		if (contact) {
			addSkidLine(last, mvc.getWheel(num).getCollisionLocation(), skid);
			last = mvc.getWheel(num).getCollisionLocation();
		} else {
			last = new Vector3f(0,0,0);
		}
	}
	
	private void addSkidLine(Vector3f a, Vector3f b, float grip) {
		if (a.equals(new Vector3f(0,0,0)) || b.equals(new Vector3f(0,0,0))) {
			return; //don't make a line because they aren't valid positions
		}
		a.y += 0.05;
		b.y += 0.05; //z-buffering (i.e. to stop it "fighting" with the ground)
		
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
		
		Material mat = new Material(mvc.assetManager, "Common/MatDefs/Light/Lighting.j3md");
		
		Texture tex = mvc.assetManager.loadTexture("assets/stripes.png");
		mat.setTexture("DiffuseMap", tex);
		mat.setTexture("NormalMap", tex);
		mat.setBoolean("UseMaterialColors", true);
		
		mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		
		mat.setColor("Diffuse", new ColorRGBA(0,0,0,grip));
		
		mat.setBoolean("UseMaterialColors", true);
		geo.setMaterial(mat);
		geo.setShadowMode(ShadowMode.Off);
		geo.setQueueBucket(Bucket.Transparent);
		mvc.skidNode.attachChild(geo);
		mvc.skidList.add(geo);
	}
}