package car.ray;

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
import game.WireframeHighlighter;

public class RayWheelControl {
	
	//Skid marks:
	private static final int QUAD_COUNT = 200;
	
	private static final int VERTEX_BUFFER_SIZE = 4*QUAD_COUNT; //Vector3f.size * triangle size * 2 (2 tri per quad) * count
	private static final int[] indexes = { 2,0,1, 1,3,2 }; //tyre marks vertex order
	private static final Vector2f[] texCoord = new Vector2f[] { //texture of quad with order
		new Vector2f(0, 0), new Vector2f(0, 1), new Vector2f(1, 0), new Vector2f(1, 1),
	};
	
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
	
	private final RayWheel wheel;
	private final Node rootNode;
	protected final Spatial spat;
	
	private final Vector3f offset;
	private Vector3f posInLocal;
	
	//TODO change to just be a wireframe and not triangles to fit the wireframe theme
	
	public RayWheelControl(RayWheel wheel, Node carRootNode, Vector3f pos) {
		this.wheel = wheel;
		this.offset = pos;
		
		//rotate and translate the wheel rootNode
		rootNode = new Node("wheel " + wheel.num);
		spat = WireframeHighlighter.create(App.rally.getAssetManager(), wheel.data.modelName, ColorRGBA.Black, ColorRGBA.Blue);
		spat.center();
		rootNode.attachChild(spat);
		
		carRootNode.attachChild(rootNode);
		
		//skid mark stuff
		this.skidLine = new Geometry();
		
		this.lastl = new Vector3f(0,0,0);
		this.lastr = new Vector3f(0,0,0);
		
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
		
		mesh.setBuffer(Type.Color, 4, BufferUtils.createFloatBuffer(ColorRGBA.BlackNoAlpha, ColorRGBA.BlackNoAlpha, ColorRGBA.BlackNoAlpha, ColorRGBA.BlackNoAlpha));
		this.skidLine.setMesh(mesh);
		
		Material mat = new Material(App.rally.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		mat.setBoolean("VertexColor", true);
		mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
		mat.getAdditionalRenderState().setWireframe(true);
		mat.getAdditionalRenderState().setLineWidth(WireframeHighlighter.LINE_WIDTH);
		
		//skid line is a special wireframe
		this.skidLine.setMaterial(mat);
		
		this.skidLine.setQueueBucket(Bucket.Transparent);
				
		App.rally.getRootNode().attachChild(this.skidLine);
		
		//TODO smoke from source control
	}

	//hopefully called by the FakeRayCarControl in physics step
	public void physicsUpdate(float tpf, Vector3f velDir, float sus_min_travel) { //TODO sus_min_travel is just poor design
		App.rally.enqueue(() -> {
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
			
			sinceLastPos -= tpf;
			if (sinceLastPos < 0) {
				sinceLastPos = skidMarkTimeout;
				addSkidLine(velDir);
			}
		});
	}
	
	private void addSkidLine(Vector3f velDir) {
		if (!wheel.inContact) {
			lastl = new Vector3f(0,0,0);
			lastr = new Vector3f(0,0,0);
			lastColor = null;
			return;
		} //exit early

		Vector3f cur = wheel.curBasePosWorld;
		if (lastl.equals(Vector3f.ZERO) || lastr.equals(Vector3f.ZERO) || cur.equals(Vector3f.ZERO)) {
			lastl = cur; 
			lastr = cur;
			lastColor = null;
			return; //don't make a line because they aren't valid positions
		}
		
		//TODO better scaling for grip value
		float clampSkid = FastMath.clamp((this.wheel.skidFraction - 0.85f)/2, 0, 1);
		if (clampSkid < 0.1f) {
			lastl = cur;
			lastr = cur;
			lastColor = null;
			return; //don't make a line because they aren't valid positions
		} //exit early if there is no mesh to create
		
		ColorRGBA c = new ColorRGBA(0,0,0,clampSkid);
		cur.y += 0.005f; //z-buffering (i.e. to stop it "fighting" with the ground texture)
		
		//TODO change to just be smaller on a large drift angle (look at most wanted's skid marks)
		
		Vector3f rot = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y).mult(velDir.normalize());
		colors[verticesPos] = lastColor;
		vertices[verticesPos++] = lastl;
		lastl = cur.add(rot.mult(wheel.data.width/2));
		colors[verticesPos] = c;
		vertices[verticesPos++] = lastl;
		
		colors[verticesPos] = lastColor;
		vertices[verticesPos++] = lastr;
		lastr = cur.add(rot.negate().mult(wheel.data.width/2));
		colors[verticesPos] = c;
		vertices[verticesPos++] = lastr;
		
		verticesPos = verticesPos % VERTEX_BUFFER_SIZE; //wrap around vertex pos
		
		Mesh mesh = this.skidLine.getMesh();
		mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
		mesh.setBuffer(Type.Color, 4, BufferUtils.createFloatBuffer(colors));
		lastColor = c;
		
		this.skidLine.updateModelBound();
	}
	
	public RayWheel getRayWheel() {
		return wheel;
	}
	
	public void cleanup() {
		rootNode.detachChild(spat);
		App.rally.getRootNode().detachChild(skidLine);
	}
}
