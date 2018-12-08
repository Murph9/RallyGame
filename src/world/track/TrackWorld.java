package world.track;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.noise.ShaderUtils;
import com.jme3.terrain.noise.basis.FilteredBasis;
import com.jme3.terrain.noise.filter.IterativeFilter;
import com.jme3.terrain.noise.filter.OptimizedErode;
import com.jme3.terrain.noise.filter.PerturbFilter;
import com.jme3.terrain.noise.filter.SmoothFilter;
import com.jme3.terrain.noise.fractal.FractalSum;
import com.jme3.terrain.noise.modulator.NoiseModulator;
import com.jme3.util.BufferUtils;

import game.App;
import helper.H;
import helper.HelperObj;
import helper.Log;
import world.World;
import world.WorldType;

public class TrackWorld extends World {

	private static final boolean DEBUG = true;
	
	private static final int POINT_COUNT = 8;
	
	private final long seed;
	private final int worldSize;
	private final Vector3f worldScale;
	
	private TerrainQuad terrain;
	private List<PhysicsControl> physicsPieces;
	
	private TerrainTrackHelper terrainHelper;
	private List<TrackSegment> trackSegments;
	
	private Vector3f unnormalizeHeightIn(Vector3f pos) {
		Vector3f p = pos.clone();
		p.y *= worldScale.y;
		return p;
	}
	
	public TrackWorld() {
		super("trackworld");

		seed = FastMath.rand.nextLong();
		worldSize = (1 << 9); //9
		worldScale = new Vector3f(1, 300, 1); //pls only 1 on non-y axis
		
		Log.p("Track World size:" + worldSize + " seed:" + seed +" worldscale:"+worldScale);
	}

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		
		float[] map = createHeightMap();
		
		this.terrainHelper = new TerrainTrackHelper(map, this.worldSize, 0.003f);
		
		this.trackSegments = createTrack(terrainHelper, app.getAssetManager());
		
		this.physicsPieces = new LinkedList<>();
		
		//add them as quads
		for (TrackSegment seg: trackSegments) {
			TrackSlice[] slices = seg.getSlices();
			for (int i = 1; i < slices.length; i++) {
				for (int j = 2; j < slices[i].points.length; j++) { //avoid the first one
					Vector3f[] quadP = new Vector3f[] {
							slices[i-1].points[j-1].clone(),
							slices[i-1].points[j].clone(),
							slices[i].points[j-1].clone(),
							slices[i].points[j].clone(),
						};
					//set the relevent terrain heights
					TrackWorld.setTerrainHeights(terrainHelper, Arrays.asList(new Vector3f[][] {quadP}), (quad) -> {
						return new Vector3f[] { quad[0], quad[1], quad[3], quad[2] };
					});
					
					for (int k = 0; k < quadP.length; k++) {
						quadP[k] = unnormalizeHeightIn(quadP[k]);
					}
					
					Geometry geo = createQuad(quadP, ColorRGBA.Brown);
					if (geo == null)
						continue;
					
//					CollisionShape col = CollisionShapeFactory.createMeshShape(geo);
//					RigidBodyControl c = new RigidBodyControl(col, 0);
					
//					physicsPieces.add(c);
//					App.rally.getPhysicsSpace().add(c);
					
					rootNode.attachChild(geo);
				}
			}
		}		
		
	    terrain = new TerrainQuad("trackworld", (this.worldSize/4)+1, this.worldSize + 1, map);

	    Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/ShowNormals.j3md");
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
	    
	    terrain.setMaterial(mat);
	    terrain.setShadowMode(ShadowMode.CastAndReceive);

	    terrain.setLocalScale(worldScale);
	    rootNode.attachChild(terrain);
	    
	    // Add a LOD which depends on were the camera is
	    List<Camera> cameras = new LinkedList<Camera>();
	    cameras.add(app.getViewPort().getCamera());
	    TerrainLodControl control = new TerrainLodControl(terrain, cameras);
	    terrain.addControl(control);
		
		//finally add the terrain to physics engine
	    RigidBodyControl rbc = new RigidBodyControl(0.0f);
	    terrain.addControl(rbc);
	    App.rally.getPhysicsSpace().add(rbc);
	}
	
	private float[] createHeightMap() {
		//TODO seed?
		
		//Create a noise based height variance filter
		FractalSum base = new FractalSum();
		base.setRoughness(0.7f);
        base.setFrequency(1.0f);
        base.setAmplitude(1.0f);
        base.setLacunarity(3.12f);
        base.setOctaves(8);
        base.setScale(0.02125f);
		base.addModulator(new NoiseModulator() {
			@Override
			public float value(float... in) {
				return ShaderUtils.clamp(in[0] * 0.5f + 0.5f, 0, 1);
			}
		});
		FilteredBasis ground = new FilteredBasis(base);
		PerturbFilter perturb = new PerturbFilter();
		perturb.setMagnitude(0.119f);

		OptimizedErode therm = new OptimizedErode();
		therm.setRadius(5);
		therm.setTalus(0.011f);

		SmoothFilter smooth = new SmoothFilter();
		smooth.setRadius(1);
		smooth.setEffect(0.7f);

		IterativeFilter iterate = new IterativeFilter();
		iterate.addPreFilter(perturb);
		iterate.addPostFilter(smooth);
		iterate.setFilter(therm);
		iterate.setIterations(1); //higher numbers make it really smooth

		ground.addPreFilter(iterate);
		
		return ground.getBuffer((this.worldSize), (this.worldSize), 0, this.worldSize+1).array();
    }
	
	private List<TrackSegment> createTrack(TerrainTrackHelper terrainHelper, AssetManager am) {
		List<Vector3f> controlPoints = new LinkedList<>();
		
		//generate some random points
		for (int i = 0; i < POINT_COUNT; i++) {
			Vector3f pos = H.randV3f(this.worldSize/2, true);
			pos.y = terrainHelper.getHeight(pos);
			
			controlPoints.add(pos);
			
			if (DEBUG) {
				HelperObj.use(this.rootNode, "ControlPoint"+i, H.makeShapeBox(am, ColorRGBA.Cyan, unnormalizeHeightIn(pos), 0.5f));
			}
		}
	
		//connect them with lines as a minimal circuit
		//https://en.wikipedia.org/wiki/Hamiltonian_path
		//oops its NP-complete
		//"epiphany" -> just join in polar co-ord order around the origin [good enough and O(n)]
		
		//sort by angle in polar co-ords
		controlPoints.sort(new Comparator<Vector3f>() {
			public int compare(Vector3f v1, Vector3f v2) {
				Vector3f p1 = FastMath.cartesianToSpherical(v1, null);
				Vector3f p2 = FastMath.cartesianToSpherical(v2, null);
				float diff = p1.y - p2.y;
				return (int)(FastMath.sign(diff)*FastMath.ceil(FastMath.abs(diff))); //prevent any precision loss on cast
			};
		});
		
		List<TrackSegment> trackSegments = new LinkedList<>();
		//link all the segments in order
		Vector3f pos = controlPoints.get(0);
		for (int i = 1; i < controlPoints.size(); i++) {
			Vector3f cur = controlPoints.get(i);
			trackSegments.add(new TrackSegmentStraight(new Vector3f[] {pos, cur}, TrackWorld.CurveFunction()));
			
			if (DEBUG) {
				HelperObj.use(this.rootNode, "RoadSegment"+i, H.makeShapeArrow(am, ColorRGBA.Blue, unnormalizeHeightIn(cur).subtract(unnormalizeHeightIn(pos)), unnormalizeHeightIn(pos)));
			}
			
			pos = cur;
		}

		//then add last -> first
		Vector3f first = controlPoints.get(0);
		Vector3f last = controlPoints.get(controlPoints.size()-1);
		trackSegments.add(new TrackSegmentStraight(new Vector3f[] {first, last}, TrackWorld.CurveFunction()));
		if (DEBUG) {
			HelperObj.use(this.rootNode, "RoadSegment"+-1, H.makeShapeArrow(am, ColorRGBA.Blue, unnormalizeHeightIn(last).subtract(unnormalizeHeightIn(first)), unnormalizeHeightIn(first)));
		}
		
		//TODO use
		new BezierPolygonInterpolation();
		
		return trackSegments;
	}
	
	@Override
	public void update(float tpf) {}
	@Override
	public void reset() {}
	
	@Override //player start pos
	public Vector3f getStartPos() { 
		if (trackSegments == null || trackSegments.isEmpty())
			return new Vector3f();
		TrackSegment segment = trackSegments.get(0);
		if (segment.getControlPoints().length < 1)
			return new Vector3f();
		
		return unnormalizeHeightIn(segment.getControlPoints()[0]).add(0, 2, 0);
	}
	@Override //player rotation
	public Matrix3f getStartRot() { return new Matrix3f(Matrix3f.IDENTITY); }
	
	public void cleanup() {
		this.rootNode.detachChild(terrain);
		App.rally.getPhysicsSpace().remove(terrain);
		for (PhysicsControl c: physicsPieces)
			App.rally.getPhysicsSpace().remove(c);
	}
	
	@Override
	public WorldType getType() {
		return WorldType.TRACK;
	}
	
	
	private static Quaternion rot90 = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);
	private static BiFunction<Vector3f, Vector3f, TrackSlice> CurveFunction() { 
		return (Vector3f off, Vector3f tang) -> {
//			Vector3f angleOff = new Vector3f(0, -1, 0);
			Vector3f normal = rot90.mult(tang.normalize());
			return new TrackSlice(new Vector3f[] { 
					off, //center of the road, not used for collision just position
//					off.add(normal.mult(6f)).add(angleOff), //TODO edges that go down
					off.add(normal.mult(5.5f)),
					off.add(normal.mult(-5.5f)),
//					off.add(normal.mult(-6f)).add(angleOff),
			});
		};
	}
	
	private static Geometry createQuad(Vector3f[] v, ColorRGBA colour) {
		if (v == null || v.length != 4) {
			Log.e("TrackWorld-createQuad: Not the correct length drawMeAQuad():");
			helper.Log.e(v, ",");
			return null;
		}
		if (Arrays.asList(v).stream().anyMatch(x -> !Vector3f.isValidVector(x))) {
			Log.e("TrackWorld-createQuad: Invalid vector given in:");
			helper.Log.e(v, ",");
			return null;
		}
		
		Mesh mesh = new Mesh(); //making a quad positions
		
		Vector2f[] texCoord = new Vector2f[4]; //texture of quad
		texCoord[0] = new Vector2f(0, 0);
		texCoord[1] = new Vector2f(0, 1);
		texCoord[2] = new Vector2f(1, 0);
		texCoord[3] = new Vector2f(1, 1);
		
		int[] indexes = { 2,0,1, 1,3,2 };
		float[] normals = new float[12];
		normals = new float[]{0,1,0, 0,1,0, 0,1,0, 0,1,0};
		
		mesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(v));
		mesh.setBuffer(Type.Normal,   3, BufferUtils.createFloatBuffer(normals));
		mesh.setBuffer(Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoord));
		mesh.setBuffer(Type.Index,    3, BufferUtils.createIntBuffer(indexes));

		mesh.updateBound();
		
		Geometry geo = new Geometry("Quad", mesh);
		
		AssetManager am = App.rally.getAssetManager();
		Material mat = null;
		if (colour != null) {
			mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
			mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
			mat.setColor("Color", colour);
		} else {
			mat = new Material(am, "Common/MatDefs/Light/Lighting.j3md");
			mat.setTexture("DiffuseMap", am.loadTexture("assets/image/asphalt_tile.jpg"));
			mat.setBoolean("UseMaterialColors", true);
			mat.setColor("Diffuse", ColorRGBA.White);
			mat.setColor("Specular", ColorRGBA.White);
			mat.setFloat("Shininess", 0); //none
		}
		geo.setShadowMode(ShadowMode.Receive);
		geo.setMaterial(mat);
		
		return geo;
	}

	private static void setTerrainHeights(TerrainTrackHelper helper, List<Vector3f[]> quads, Function<Vector3f[], Vector3f[]> order) {
		List<Vector3f> posList = new LinkedList<>();
		
		for (Vector3f[] quad: quads) {
			
			quad = order.apply(quad);
			
			float[] box = H.boundingBoxXZ(quad);
			box[0] -= 1; //extend the extends so they cover it completely
			box[1] -= 1;
			box[2] += 1;
			box[3] += 1;
			for (int i = (int)box[0]; i < box[2]; i++) {
				for (int j = (int)box[1]; j < box[3]; j++) {
					Vector3f pos = new Vector3f(i, 0, j);
					
					//use the jme3 library method for point in triangle
					if (FastMath.pointInsideTriangle(H.v3tov2fXZ(quad[0]), H.v3tov2fXZ(quad[2]), H.v3tov2fXZ(quad[3]), H.v3tov2fXZ(pos)) != 0) {
						pos.y = H.heightInTri(quad[0], quad[2], quad[3], pos);
						posList.add(pos);
						
					} else if (FastMath.pointInsideTriangle(H.v3tov2fXZ(quad[0]), H.v3tov2fXZ(quad[2]), H.v3tov2fXZ(quad[1]), H.v3tov2fXZ(pos)) != 0) {
						pos.y = H.heightInTri(quad[0], quad[2], quad[1], pos);
						posList.add(pos);
					}
				}
			}
		}
		
		
		try {			
			helper.setHeights(posList);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}