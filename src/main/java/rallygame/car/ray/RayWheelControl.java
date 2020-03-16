package rallygame.car.ray;

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

import rallygame.effects.LoadModelWrapper;
import rallygame.helper.H;

public class RayWheelControl {
	
	//#region Skid marks fields
	private static final int QUAD_COUNT = 200;
    
    // Vector3f.size * triangle size * 2 (2 tri per quad) * count
	private static final int VERTEX_BUFFER_SIZE = 4 * QUAD_COUNT;
	private static final int[] indexes = { 2, 0, 1, 1, 3, 2 }; // tyre marks vertex order
	private static final Vector2f[] texCoord = new Vector2f[] { // texture of quad with order
		    new Vector2f(0, 0), new Vector2f(0, 1), new Vector2f(1, 0), new Vector2f(1, 1),
    };
    private static final int[] i_s = new int[6 * QUAD_COUNT];
    private static final Vector2f[] coord = new Vector2f[4 * QUAD_COUNT];
    static {
        //init some more complex static fields
        int j = 0;
        for (int i = 0; i < i_s.length; i += 6) {
            i_s[i + 0] = j * 4 + indexes[0];
            i_s[i + 1] = j * 4 + indexes[1];
            i_s[i + 2] = j * 4 + indexes[2];
            i_s[i + 3] = j * 4 + indexes[3];
            i_s[i + 4] = j * 4 + indexes[4];
            i_s[i + 5] = j * 4 + indexes[5];
            j++;
        }
        j = 0;
        for (int i = 0; i < coord.length; i += 4) {
            coord[i + 0] = texCoord[0].add(new Vector2f(0, j));
            coord[i + 1] = texCoord[0].add(new Vector2f(0, j));
            coord[i + 2] = texCoord[0].add(new Vector2f(0, j));
            coord[i + 3] = texCoord[0].add(new Vector2f(0, j));
            j++;
        }
    }
    
	private static final ColorRGBA BASE_SKID_COLOUR = ColorRGBA.Black;
	
	private final Geometry skidLine;
	private final Vector3f[] vertices;
    private final ColorRGBA[] colors;
    private int verticesPos;
	
	private ColorRGBA lastColor;
	private Vector3f lastl;
	private Vector3f lastr;
	
	private static final float SKID_MARK_TIMEOUT = 0.1f;
	private float sinceLastPos;
	//#endregion
	
	private final SimpleApplication app;
	private final RayWheel wheel;
	private final Node rootNode;
	private final Spatial spat;
	
	private final Vector3f offset;
	
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
        this.lastColor = ColorRGBA.BlackNoAlpha;
        colors = new ColorRGBA[VERTEX_BUFFER_SIZE];
        vertices = new Vector3f[VERTEX_BUFFER_SIZE];

        this.skidLine = new Geometry();
        // create skid mesh
        Mesh mesh = new Mesh();
        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        mesh.setBuffer(Type.Color, 4, BufferUtils.createFloatBuffer(colors));
        mesh.setBuffer(Type.Index, 3, BufferUtils.createIntBuffer(i_s));
        mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(coord));
        // this is the default, but i have to come looking for this again...
        mesh.setMode(Mode.Triangles);
		this.skidLine.setMesh(mesh);
		
		//material uses vertex colours
		Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
		mat.setBoolean("VertexColor", true);
		this.skidLine.setMaterial(mat);
        this.skidLine.setQueueBucket(Bucket.Transparent);
		
		app.getRootNode().attachChild(this.skidLine);
        
        //trigger one view update so the cars wheels are in 'position' on load incase the car isn't loaded enabled
        viewUpdate(1/60f, new Vector3f(), 0);

		//Smoke can be found from source control
	}

	//hopefully called by the RayCarControl in physics step
	protected void viewUpdate(float tpf, Vector3f velDir, float sus_min_travel) { 
		//NOTE: sus_min_travel is just poor design, but how else will the wheel object know a car const?
		app.enqueue(() -> {
			Vector3f posInLocal = new Vector3f(0, -wheel.susRayLength - sus_min_travel, 0);
			rootNode.setLocalTranslation(offset.add(posInLocal));
			
			//rotate for wheel rotation
			Quaternion q = new Quaternion();
			q.fromAngleNormalAxis(-wheel.radSec * tpf * FastMath.sign(wheel.num % 2 == 1 ? 1 : -1),
                    new Vector3f(1, 0, 0));
			spat.setLocalRotation(spat.getLocalRotation().mult(q));
			
			//rotate for wheel steering
			q.fromAngleNormalAxis(wheel.steering, new Vector3f(0,1,0));
			rootNode.setLocalRotation(q);
			//lastly the odd side needs rotating (but every frame?)
			if (wheel.num % 2 == 1)
				rootNode.rotate(0, FastMath.PI, 0);
			
			updateSkidLine(tpf, velDir);
		});
    }
	
	private void updateSkidLine(float tpf, Vector3f velDir) {
        boolean lastingSkid = false;
        // continually update the current skid mark position and 'lock it in' to persist
        sinceLastPos -= tpf;
		if (sinceLastPos < 0) {
            lastingSkid = true;
            sinceLastPos = SKID_MARK_TIMEOUT;
		}

		// Possible: change this be thinner with a larger drift angle (which we don't have here)
		// i have tested this is real life, width is lower when sliding sideways but not that much lower

		// scaling the grip value
		float clampSkid = calcSkidSkew(this.wheel.skidFraction);
        ColorRGBA curColor = BASE_SKID_COLOUR.clone().mult(clampSkid);
        
        //ignore moving objects because we can't 'track' it and it looks weird
        boolean contactObjectIsMoving = wheel.collisionObject != null && wheel.collisionObject.getMass() > 0;

        // no contact or slow speed => no visible skid marks
		if (!wheel.inContact || velDir.length() < 0.5f || clampSkid < 0.05f || contactObjectIsMoving) {
			if (lastColor.equals(ColorRGBA.BlackNoAlpha)) {
				//the last one was null, ignore
				return;
            }
            //we reset the current quad to nothing
			this.lastColor = curColor = ColorRGBA.BlackNoAlpha;
		}

        Vector3f pos = wheel.curBasePosWorld;
        pos.y += 0.015f; // hacky z-buffering (i.e. to stop it "fighting" with the ground texture)
        Vector3f rot = new Quaternion().fromAngleAxis(FastMath.HALF_PI, wheel.hitNormalInWorld).mult(velDir.normalize());
        Vector3f posL = pos.add(rot.mult(wheel.data.width / 2));
        Vector3f posR = pos.add(rot.negate().mult(wheel.data.width / 2));

		if (lastColor.equals(ColorRGBA.BlackNoAlpha)) {
            // update with nothing
            updateSegment(curColor, curColor, posL, posR, posL, posR);
		} else {
            updateSegment(lastColor, curColor, lastl, lastr, posL, posR);
        }

        if (lastingSkid) {
            this.verticesPos += 4;
            // wrap around vertex pos
            this.verticesPos = this.verticesPos % VERTEX_BUFFER_SIZE;

            this.lastl = posL;
            this.lastr = posR;
            this.lastColor = curColor;
        }
    }
    
    private void updateSegment(ColorRGBA lastColor, ColorRGBA curColor, Vector3f lastL, Vector3f lastR, Vector3f curL, Vector3f curR) {
        this.colors[verticesPos] = lastColor;
        this.vertices[verticesPos] = lastL;

        this.colors[verticesPos + 1] = curColor;
        this.vertices[verticesPos + 1] = curL;

        this.colors[verticesPos + 2] = lastColor;
        this.vertices[verticesPos + 2] = lastR;

        this.colors[verticesPos + 3] = curColor;
        this.vertices[verticesPos + 3] = curR;

        Mesh mesh = this.skidLine.getMesh();
        mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        mesh.setBuffer(Type.Color, 4, BufferUtils.createFloatBuffer(colors));

        this.skidLine.updateModelBound();
    }

    private static float calcSkidSkew(float skidFraction) {
        return H.skew(skidFraction, 0.75f, 1.3f, 0, 1);
    }
	
	public RayWheel getRayWheel() {
		return wheel;
	}
	
	public void cleanup() {
		rootNode.detachChild(spat);
		app.getRootNode().detachChild(skidLine);
    }
}
