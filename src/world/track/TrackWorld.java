package world.track;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
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
import com.jme3.scene.Node;
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
	private final float worldVerticalScale;
	
	private TerrainQuad terrain;
	private List<PhysicsControl> physicsPieces;
	private List<Vector3f> controlPoints;
	
	private List<TrackSegment> trackSegments;
		
	public TrackWorld() {
		super("trackworld");
		
		seed = FastMath.rand.nextLong();
		worldSize = (2 << 7); //8
		worldVerticalScale = 300;
		Log.p("Track World size:" + worldSize + " seed:" + seed +" worldscale:"+worldVerticalScale);
	}

	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		super.initialize(stateManager, app);
		createTerrain();
		createTrack(app.getAssetManager());
		
		//add them as quads
		physicsPieces = new LinkedList<>();
		List<Vector3f[]> quads = new LinkedList<>();
		for (TrackSegment seg: trackSegments) {
			TrackSlice[] slices = seg.getSlices();
			for (int i = 1; i < slices.length; i++) {
				for (int j = 2; j < slices[i].points.length; j++) { //avoid the first one
					Vector3f[] vs = new Vector3f[] {
							slices[i-1].points[j-1],
							slices[i-1].points[j],
							slices[i].points[j-1],
							slices[i].points[j],
						};
				
					Geometry geo = createQuad(rootNode, vs, ColorRGBA.Brown);
					if (geo == null) {
						continue;
					}
//					CollisionShape col = CollisionShapeFactory.createMeshShape(geo);
//					RigidBodyControl c = new RigidBodyControl(col, 0);
					
					rootNode.attachChild(geo);
//					physicsPieces.add(c);
//					App.rally.getPhysicsSpace().add(c);
					
					quads.add(vs);
					
//					TrackWorld.setHeightsFor(terrain, this.worldVerticalScale, Arrays.asList(new Vector3f[][] {vs}), (quad) -> {
//						return new Vector3f[] { quad[0], quad[1], quad[3], quad[2] };
//					});
				}
			}
		}
		
		TrackWorld.setHeightsSlow(terrain, this.worldVerticalScale, quads, (quad) -> {
			return new Vector3f[] { quad[0], quad[1], quad[3], quad[2] };
		});
		
		//finally add the terrain to physics engine
	    RigidBodyControl rbc = new RigidBodyControl(0.0f);
	    terrain.addControl(rbc);
	    App.rally.getPhysicsSpace().add(rbc);
	}
	
	private void createTerrain() {
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
		
		float[] map = ground.getBuffer((this.worldSize), (this.worldSize), 0, this.worldSize+1).array();
		
		
	    terrain = new TerrainQuad("trackworld", (this.worldSize+1)/4, this.worldSize + 1, map);

	    Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/ShowNormals.j3md");
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
	    
	    terrain.setMaterial(mat);
	    terrain.setLocalTranslation(0, -worldVerticalScale/2, 0);
	    terrain.setLocalScale(2f, worldVerticalScale, 2f);
	    terrain.setShadowMode(ShadowMode.CastAndReceive);
	    rootNode.attachChild(terrain);

	    // Add a LOD which depends on were the camera is
	    List<Camera> cameras = new LinkedList<Camera>();
	    cameras.add(app.getViewPort().getCamera());
	    TerrainLodControl control = new TerrainLodControl(terrain, cameras);
	    terrain.addControl(control);
    }
	
	private void createTrack(AssetManager am) {
		controlPoints = new LinkedList<>();
		
		//generate some random points
		for (int i = 0; i < POINT_COUNT; i++) {
			Vector3f pos = H.randV3f(this.worldSize, true);
			pos.y = 0;
			
			controlPoints.add(pos);
			
			if (DEBUG) {
				HelperObj.use(this.rootNode, "ControlPoint"+i, H.makeShapeBox(am, ColorRGBA.Cyan, pos, 0.5f));
			}
		}
	
		//connect them with lines as a minimal circuit
		//https://en.wikipedia.org/wiki/Hamiltonian_path
		//oops its NP-complete
		//"epiphany" -> just join in polar co-ord order around the origin [good enough and O(n)]
		
		//sort by angle in polar co-ords
		this.controlPoints.sort(new Comparator<Vector3f>() {
			public int compare(Vector3f v1, Vector3f v2) {
				Vector3f p1 = FastMath.cartesianToSpherical(v1, null);
				Vector3f p2 = FastMath.cartesianToSpherical(v2, null);
				float diff = p1.y - p2.y;
				return (int)(FastMath.sign(diff)*FastMath.ceil(FastMath.abs(diff))); //prevent any precision loss on cast
			};
		});
		
		trackSegments = new LinkedList<>();
		//link all the segments in order
		Vector3f pos = controlPoints.get(0);
		for (int i = 1; i < controlPoints.size(); i++) {
			Vector3f cur = controlPoints.get(i);
			trackSegments.add(new TrackSegmentStraight(new Vector3f[] {pos, cur}, TrackWorld.CurveFunction(0.05f)));
		
			if (DEBUG) {
				HelperObj.use(this.rootNode, "RoadSegment"+i, H.makeShapeArrow(am, ColorRGBA.Blue, cur.subtract(pos), pos));
			}
			
			pos = cur;
		}

		//then add last -> first
		Vector3f first = controlPoints.get(0);
		Vector3f last = controlPoints.get(controlPoints.size()-1);
		trackSegments.add(new TrackSegmentStraight(new Vector3f[] {first, last}, TrackWorld.CurveFunction(0.05f)));
		if (DEBUG) {
			HelperObj.use(this.rootNode, "RoadSegment"+-1, H.makeShapeArrow(am, ColorRGBA.Blue, last.subtract(first), first));
		}
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
		
		return segment.getControlPoints()[0].add(0, 2, 0);
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
	private static BiFunction<Vector3f, Vector3f, TrackSlice> CurveFunction(float verticalOffset) { 
		return (Vector3f off, Vector3f tang) -> {
			off.y += verticalOffset;
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
	
	private static Geometry createQuad(Node rootNode, Vector3f[] v, ColorRGBA colour) {
		if (v == null || v.length != 4) {
			Log.e("Roads-drawMeAQuad: Not the correct length drawMeAQuad()");
			return null;
		}
		if (Arrays.asList(v).stream().anyMatch(x -> !Vector3f.isValidVector(x))) {
			Log.e("Roads-drawMeAQuad: Invalid vector given");
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
		mesh.setBuffer(Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
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

	private static void setHeightsFor(TerrainQuad terrain, float heightScale, List<Vector3f[]> quads, Function<Vector3f[], Vector3f[]> order) {
		List<Vector3f> heightList = new LinkedList<Vector3f>();
		
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
						heightList.add(pos);
						
					} else if (FastMath.pointInsideTriangle(H.v3tov2fXZ(quad[0]), H.v3tov2fXZ(quad[2]), H.v3tov2fXZ(quad[1]), H.v3tov2fXZ(pos)) != 0) {
						pos.y = H.heightInTri(quad[0], quad[2], quad[1], pos);
						heightList.add(pos);
					}
				}
			}
		}
		
		for (Vector3f vec: heightList)
			terrain.setHeight(H.v3tov2fXZ(vec), (vec.y + heightScale/2)/heightScale + 0.0002f); //TODO so i can see it
	}

	private static void setHeightsSlow(TerrainQuad terrain, float heightScale, List<Vector3f[]> quads, Function<Vector3f[], Vector3f[]> order) {
		int size = terrain.getTotalSize();
		float scaleX = terrain.getWorldScale().x;
		float scaleZ = terrain.getWorldScale().z;
		
		HashMap<Vector2f, Vector3f> heightList = new HashMap<>();
		
		for (int i = (int)(-size*scaleX); i < size*scaleX; i++) {
			for (int j = (int)(-size*scaleZ); j < size*scaleZ; j++) {
				Vector3f pos = new Vector3f(i, 0, j);
				
				for (Vector3f[] quad: quads) {
					quad = order.apply(quad);
					
					//use the jme3 library method for point in triangle
					if (FastMath.pointInsideTriangle(H.v3tov2fXZ(quad[0]), H.v3tov2fXZ(quad[2]), H.v3tov2fXZ(quad[3]), H.v3tov2fXZ(pos)) != 0) {
						pos.y = H.heightInTri(quad[0], quad[2], quad[3], pos);
						
						heightList.put(H.v3tov2fXZ(pos), pos);
						continue;
						
					} else if (FastMath.pointInsideTriangle(H.v3tov2fXZ(quad[0]), H.v3tov2fXZ(quad[2]), H.v3tov2fXZ(quad[1]), H.v3tov2fXZ(pos)) != 0) {
						pos.y = H.heightInTri(quad[0], quad[2], quad[1], pos);
						
						heightList.put(H.v3tov2fXZ(pos), pos);
						continue;
					} else {
						
						//scale it so it smoothes the ground around it
						float distFromQuad = H.distFromLineXZ(new Vector3f().interpolateLocal(quad[0], quad[1], 1/2), 
								new Vector3f().interpolateLocal(quad[2], quad[3], 1/2), pos);
						float curHeight = terrain.getHeight(H.v3tov2fXZ(pos));
						
						if (distFromQuad < 10) {
//							pos.y = new Vector3f().interpolateLocal(quad[0], quad[1], 1/2).y;
						}
						
						//TODO
						//set the height of points outside the road to smooth the edge transisions
					}
				}
			}
		}
		
		for (Vector3f vec: heightList.values())
			terrain.setHeight(H.v3tov2fXZ(vec), (vec.y + heightScale/2)/heightScale + 0.0002f); //TODO so i can see it

		//smooth using the filter		
		float[] map = smoothHeightMap(size, size, 1, terrain.getHeightMap(), size+1);
		
		//TODO how to set this back on the terrain 
	}
	
	//https://github.com/jMonkeyEngine/jmonkeyengine/blob/master/jme3-terrain/src/main/java/com/jme3/terrain/noise/filter/SmoothFilter.java
	private static float[] smoothHeightMap(float sx, float sy, float base, float[] data, int size) {
        float[] retval = new float[data.length];
        int radius = 1;
        float effect = 0.7f;

        for (int y = radius; y < size - radius; y++) {
            for (int x = radius; x < size - radius; x++) {
                int idx = y * size + x;
                float n = 0;
                for (int i = -radius; i < radius + 1; i++) {
                    for (int j = -radius; j < radius + 1; j++) {
                        n += data[(y + i) * size + x + j];
                    }
                }
                retval[idx] = effect * n / (4 * radius * (radius + 1) + 1) + (1 - effect) * data[idx];
            }
        }

        return retval;
    }
}
