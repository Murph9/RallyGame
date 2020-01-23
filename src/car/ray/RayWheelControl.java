package car.ray;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue.Bucket;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

import effects.LoadModelWrapper;

public class RayWheelControl {
	
	//Skid marks count
	private static final int QUAD_COUNT = 200;
	
	private static final int VERTEX_BUFFER_SIZE = 4*QUAD_COUNT; //Vector3f.size * triangle size * 2 (2 tri per quad) * count
	private static final int[] indexes = { 2,0,1, 1,3,2 }; //tyre marks vertex order
	private static final Vector2f[] texCoord = new Vector2f[] { //texture of quad with order
		new Vector2f(0, 0), new Vector2f(0, 1), new Vector2f(1, 0), new Vector2f(1, 1),
	};
    
	private static final ColorRGBA BASE_SKID_COLOUR = ColorRGBA.Black;
	
	private Geometry skidLine;
	private Vector3f[] vertices;
	private int verticesPos;
	private ColorRGBA[] colors;
	
	private ColorRGBA lastColor;
	private Vector3f lastl;
	private Vector3f lastr;
	
	private static final float skidMarkTimeout = 0.1f;
	private float sinceLastPos;
	//end skid marks
	
	private final SimpleApplication app;
	private final RayWheel wheel;
	private final Node rootNode;
	protected final Spatial spat;
	
	private final Vector3f offset;
	private Vector3f posInLocal;
	
	public RayWheelControl(SimpleApplication app, RayWheel wheel, Node carRootNode, Vector3f pos) {
		this.app = app;
		this.wheel = wheel;
		this.offset = pos;
		
		//rotate and translate the wheel rootNode
		rootNode = new Node("wheel " + wheel.num);
		spat = LoadModelWrapper.create(app.getAssetManager(), wheel.data.modelName, ColorRGBA.DarkGray);
		spat.center();
		rootNode.attachChild(spat);
		
		carRootNode.attachChild(rootNode);
		
		//skid mark stuff
		this.skidLine = new Geometry();
		
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
		
		Vector2f[] coord = new Vector2f[4*QUAD_COUNT];
		j = 0;
		for (int i = 0; i < coord.length; i += 4) {
			coord[i+0] = texCoord[0].add(new Vector2f(0, j));
			coord[i+1] = texCoord[0].add(new Vector2f(0, j));
			coord[i+2] = texCoord[0].add(new Vector2f(0, j));
			coord[i+3] = texCoord[0].add(new Vector2f(0, j));
			j++;
		}
		
		mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(coord)); 
		
		this.skidLine.setMesh(mesh);
		
		//material uses vertex colours
		Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		mat.getAdditionalRenderState().setLineWidth(3);
		mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
		mat.setBoolean("VertexColor", true);
		this.skidLine.setMaterial(mat);

		// this is the default, but i have to come looking for this again...
		this.skidLine.getMesh().setMode(Mode.Triangles);
		
		this.skidLine.setQueueBucket(Bucket.Transparent);
		
		app.getRootNode().attachChild(this.skidLine);
		
		//Smoke is can be found from source control
	}

	//hopefully called by the FakeRayCarControl in physics step
	public void viewUpdate(float tpf, Vector3f velDir, float sus_min_travel) { 
		//NOTE: sus_min_travel is just poor design, but how else will the wheel object know a car const?
		app.enqueue(() -> {
			posInLocal = new Vector3f(0, -wheel.susRayLength - sus_min_travel, 0);
			rootNode.setLocalTranslation(offset.add(posInLocal));
			
			//rotate for wheel rotation
			Quaternion q = new Quaternion();
			q.fromAngleNormalAxis(-wheel.radSec*tpf*FastMath.sign(wheel.num % 2 == 1? 1 : -1), new Vector3f(1,0,0));
			spat.setLocalRotation(spat.getLocalRotation().mult(q));
			
			//rotate for wheel steering
			q.fromAngleNormalAxis(wheel.steering, new Vector3f(0,1,0));
			rootNode.setLocalRotation(q);
			//lastly the odd side needs rotating (but every frame?)
			if (wheel.num % 2 == 1)
				rootNode.rotate(0, FastMath.PI, 0);
			
			addSkidLine(tpf, velDir);
		});
	}
	
	private void addSkidLine(float tpf, Vector3f velDir) {
		sinceLastPos -= tpf;
		if (sinceLastPos > 0) {
			//then just update the current position
			return;
		}
		sinceLastPos = skidMarkTimeout;

		// TODO change to just be thinner with a larger drift angle
		// i have tested this is real life, width is lower when sliding sideways but not that much lower

		// scaling the grip value
		float clampSkid = FastMath.clamp(this.wheel.skidFraction - 0.9f, 0, 1);
        ColorRGBA c = BASE_SKID_COLOUR.clone().mult(clampSkid);
        
        //ignore moving objects because we can't 'track' it and it looks weird
        boolean contactObjectIsMoving = wheel.collisionObject != null && wheel.collisionObject.getMass() > 0;

        // no contact or slow speed => no visible skid marks
		if (!wheel.inContact || velDir.length() < 1 || clampSkid < 0.1f || contactObjectIsMoving) {
			if (lastColor != null && lastColor.equals(new ColorRGBA(0, 0, 0, 0))) {
				//the last one was null, ignore
				return;
			}
			lastColor = c = new ColorRGBA(0, 0, 0, 0);
		}

        Vector3f cur = wheel.curBasePosWorld;
        cur.y += 0.015f; // hacky z-buffering (i.e. to stop it "fighting" with the ground texture)
        Vector3f rot = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y).mult(velDir.normalize());
        Vector3f curL = cur.add(rot.mult(wheel.data.width / 2));
        Vector3f curR = cur.add(rot.negate().mult(wheel.data.width / 2));

		if (lastColor != null && lastColor.equals(new ColorRGBA(0, 0, 0, 0))) {
			lastl = curL;
			lastr = curR;
			lastColor = c;
		}
		
		colors[verticesPos] = lastColor;
		vertices[verticesPos] = lastl;
		verticesPos++;
		
		colors[verticesPos] = c;
		vertices[verticesPos] = curL;
		verticesPos++;
		
		colors[verticesPos] = lastColor;
		vertices[verticesPos] = lastr;
		verticesPos++;

		colors[verticesPos] = c;
		vertices[verticesPos] = curR;
		verticesPos++;

		lastl = curL;
		lastr = curR;
		lastColor = c;

		verticesPos = verticesPos % VERTEX_BUFFER_SIZE; //wrap around vertex pos
		
		Mesh mesh = this.skidLine.getMesh();
		mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
		mesh.setBuffer(Type.Color, 4, BufferUtils.createFloatBuffer(colors));
		
		this.skidLine.updateModelBound();
	}
	
	public RayWheel getRayWheel() {
		return wheel;
	}
	
	public void cleanup() {
		rootNode.detachChild(spat);
		app.getRootNode().detachChild(skidLine);
	}
}
