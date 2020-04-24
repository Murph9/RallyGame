package rallygame.world.track;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.jme3.app.Application;
import com.jme3.asset.AssetManager;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
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

import rallygame.car.ray.RayCarControl;
import rallygame.drive.IDrive;
import rallygame.effects.LoadModelWrapper;
import rallygame.game.DebugAppState;
import rallygame.helper.Geo;
import rallygame.helper.H;
import rallygame.helper.Log;
import rallygame.helper.Trig;
import rallygame.service.ObjectPlacer;
import rallygame.service.ObjectPlacer.NodeId;
import rallygame.world.ICheckpointWorld;
import rallygame.world.World;
import rallygame.world.WorldType;

public class TrackWorld extends World implements ICheckpointWorld {

	//NOTE: this class is outdated by PathWorld, but it contains things that it doesn't do

	private static final boolean DEBUG = true;

	private static final int POINT_COUNT = 12;

	private final long seed;
	private final int worldSize;
	private final Vector3f worldScale;

	private TerrainQuad terrain;
	private List<PhysicsControl> physicsPieces;

	private TerrainTrackHelper terrainHelper;
	private List<TrackSegment> trackSegments;
	private List<Vector3f> controlPoints;
	private NodeId treeNode;

	private Vector3f normalizeHeightIn(Vector3f pos) {
		Vector3f p = pos.clone();
		p.divideLocal(worldScale);
		return p;
	}

	private Vector3f unnormalizeHeightIn(Vector3f pos) {
		Vector3f p = pos.clone();
		p.multLocal(worldScale);
		return p;
	}

	public TrackWorld() {
		super("trackworld");

		seed = FastMath.rand.nextLong();
		worldSize = (1 << 9); // 9
		worldScale = new Vector3f(2, 300, 2); // pls only 1 on non-y axis

		Log.p("Track World size:" + worldSize + " seed:" + seed + " worldscale:" + worldScale);
	}

	@Override
	public void initialize(Application app) {
		super.initialize(app);

		float[] map = createHeightMap();

		this.terrainHelper = new TerrainTrackHelper(map, this.worldSize, 0.003f);

		this.trackSegments = createTrack(terrainHelper, app.getAssetManager());

		this.physicsPieces = new LinkedList<>();

		// add them as quads
		for (TrackSegment seg : trackSegments) {
			TrackSlice[] slices = seg.getSlices(8); // TODO hardcoded number
			for (int i = 1; i < slices.length; i++) {
				for (int j = 2; j < slices[i].points.length; j++) { // avoid the first one
					Vector3f[] quadP = new Vector3f[] { slices[i - 1].points[j - 1].clone(),
							slices[i - 1].points[j].clone(), slices[i].points[j - 1].clone(),
							slices[i].points[j].clone(), };
					// set the relevent terrain heights
					TrackWorld.setTerrainHeights(terrainHelper, Arrays.asList(new Vector3f[][] { quadP }), (quad) -> {
						return new Vector3f[] { quad[0], quad[1], quad[3], quad[2] };
					});

					for (int k = 0; k < quadP.length; k++) {
						quadP[k] = unnormalizeHeightIn(quadP[k]);
					}

					Geometry geo = createQuad(app.getAssetManager(), quadP, ColorRGBA.Brown);
					if (geo == null)
						continue;

					// CollisionShape col = CollisionShapeFactory.createMeshShape(geo);
					// RigidBodyControl c = new RigidBodyControl(col, 0);

					// physicsPieces.add(c);
					// App.rally.getPhysicsSpace().add(c);

					geo.setLocalTranslation(geo.getLocalTranslation().add(0, 0.001f, 0)); // to stop flickering
					rootNode.attachChild(geo);
				}
			}
		}

		terrain = new TerrainQuad("trackworld", (this.worldSize / 4) + 1, this.worldSize + 1, map);

		Material terrainMaterial = createTerrainMaterial(app.getAssetManager());
		if (DEBUG) {
			terrainMaterial = new Material(app.getAssetManager(), "Common/MatDefs/Misc/ShowNormals.j3md");
			terrainMaterial.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		}

		terrain.setMaterial(terrainMaterial);
		terrain.setShadowMode(ShadowMode.CastAndReceive);

		terrain.setLocalScale(worldScale);
		rootNode.attachChild(terrain);

		// Add a LOD which depends on were the camera is
		List<Camera> cameras = new LinkedList<Camera>();
		cameras.add(app.getViewPort().getCamera());
		TerrainLodControl control = new TerrainLodControl(terrain, cameras);
		terrain.addControl(control);

		// finally add the terrain to physics engine
		RigidBodyControl rbc = new RigidBodyControl(0.0f);
		terrain.addControl(rbc);
		getState(BulletAppState.class).getPhysicsSpace().add(rbc);

		// tree world doesn't need to know the world before the scale
		treeNode = getTreeNode(2000);
	}


	private static final String[] Tree_Strings = new String[] { "objects/tree_0.blend.glb", "objects/tree_1.blend.glb",
			"objects/tree_2.blend.glb", "objects/tree_3.blend.glb", "objects/tree_4.blend.glb",
			"objects/tree_5.blend.glb", "objects/tree_6.blend.glb", };
	private NodeId getTreeNode(int treeCount) {
		Spatial treeGeoms[] = new Spatial[Tree_Strings.length];
		for (int i = 0; i < Tree_Strings.length; i++) {
			treeGeoms[i] = LoadModelWrapper.create(getApplication().getAssetManager(), Tree_Strings[i]);
		}

		Spatial[] spats = new Spatial[treeCount];
		Vector3f[] positions = new Vector3f[treeCount];
		for (int i = 0; i < treeCount; i++) {
			Vector3f pos = H.randV3f(worldSize*terrain.getWorldScale().x/2, true);
			float height = terrain.getHeight(new Vector2f(pos.x, pos.z));
			if (Float.isNaN(height) || height == 0) {
				Log.p("pos, ", pos, "isn't on the terrain :(");
				height = 0;
			}
			pos.y = height;
			positions[i] = pos;

			spats[i] = treeGeoms[FastMath.nextRandomInt(0, treeGeoms.length - 1)].clone();
		}
		ObjectPlacer op = getState(ObjectPlacer.class);
		return op.addBulk(Arrays.asList(spats), Arrays.asList(positions));
	}

	private float[] createHeightMap() {
		// TODO seed?

		// Create a noise based height variance filter
		FractalSum base = new FractalSum();
		base.setRoughness(0.7f);
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
		iterate.setIterations(3); // higher numbers make it really smooth

		ground.addPreFilter(iterate);

		return ground.getBuffer((this.worldSize), (this.worldSize), 0, this.worldSize + 1).array();
	}

	private List<TrackSegment> createTrack(TerrainTrackHelper terrainHelper, AssetManager am) {
		controlPoints = new LinkedList<>();

		// generate some random points
		for (int i = 0; i < POINT_COUNT; i++) {
			Vector3f pos = H.randV3f(this.worldSize / 2, true);
			pos.y = terrainHelper.getHeight(pos);

			controlPoints.add(pos);

			if (DEBUG) {
				getState(DebugAppState.class).drawBox("ControlPoint" + i, ColorRGBA.Cyan, unnormalizeHeightIn(pos),
						0.5f);
			}
		}

		// connect them with lines as a minimal circuit
		// https://en.wikipedia.org/wiki/Hamiltonian_path
		// oops its NP-complete
		// "epiphany" -> just join in polar co-ord order around the origin [good enough and O(n)]

		// sort by angle in polar co-ords
		controlPoints.sort(new Comparator<Vector3f>() {
			public int compare(Vector3f v1, Vector3f v2) {
				Vector3f p1 = FastMath.cartesianToSpherical(v1, null);
				Vector3f p2 = FastMath.cartesianToSpherical(v2, null);
				float diff = p1.y - p2.y;
				return (int) (FastMath.sign(diff) * FastMath.ceil(FastMath.abs(diff))); // prevent any precision loss on
																						// cast
			};
		});
/* debug 'circle'
		controlPoints = Arrays.asList(
	            new Vector3f(-worldSize/4, terrainHelper.getHeight(new Vector3f(-worldSize/4,0,worldSize/4)), worldSize/4),
	            new Vector3f(-worldSize/4, terrainHelper.getHeight(new Vector3f(-worldSize/4,0,-worldSize/4)), -worldSize/4),
	            new Vector3f(worldSize/4, terrainHelper.getHeight(new Vector3f(worldSize/4,0,-worldSize/4)), -worldSize/4),
	            new Vector3f(worldSize/4, terrainHelper.getHeight(new Vector3f(worldSize/4,0,worldSize/4)),worldSize/4)
	        );
*/
		List<TrackSegment> trackSegments = BezierPolygonInterpolation.GetBezierCurvesN(controlPoints, 1, TrackWorld.CurveFunction());

		for (int i = 1; i < trackSegments.size(); i++) {
			Vector3f cur = trackSegments.get(i).getControlPoints()[0];
			Vector3f pos = trackSegments.get(i).getControlPoints()[3];
			if (DEBUG) {
				Log.p(i);
				Log.p(trackSegments.get(i));
				getState(DebugAppState.class).drawArrow("RoadSegment" + i, ColorRGBA.Blue, unnormalizeHeightIn(pos), 
						unnormalizeHeightIn(cur).subtract(unnormalizeHeightIn(pos)));
			}
		}

		return trackSegments;
	}

	private Material createTerrainMaterial(AssetManager am) {
		Material m = new Material(am, "MatDefs/terrainheight/TerrainColorByHeight.j3md");
		m.setColor("LowColor", ColorRGBA.Green);
		m.setColor("HighColor", ColorRGBA.Green);
		return m;
	}

	@Override
	public void update(float tpf) {
		IDrive drive = getStateManager().getState(IDrive.class);
		if (drive != null) {
			boolean noCars = drive.getAllCars().isEmpty();
			if (this.trackSegments != null && !noCars && DEBUG) {
				// hack to see if the bezier curve stuff works
				RayCarControl car = drive.getAllCars().stream().findFirst().get();
				Vector3f pos = getClosestPointTo(this.trackSegments, normalizeHeightIn(car.location));

				getState(DebugAppState.class).drawBox("closestpointtocurve", ColorRGBA.LightGray, unnormalizeHeightIn(pos), 1);
			}
		}
	}

	@Override
	public void reset() {
	}

	@Override
	public Transform getStart() {
		if (trackSegments == null || trackSegments.isEmpty())
			return new Transform();
		TrackSegment segment = trackSegments.get(0);
		if (segment.getControlPoints().length < 1)
			return new Transform();

		Vector3f pos = unnormalizeHeightIn(segment.getControlPoints()[0]).add(0, 2, 0);
		return new Transform(pos);
	}

	@Override
	public void cleanup(Application app) {
		this.rootNode.detachChild(terrain);
		PhysicsSpace space = getState(BulletAppState.class).getPhysicsSpace();
		space.remove(terrain);
		for (PhysicsControl c : physicsPieces)
			space.remove(c);

		if (treeNode != null)
			getState(ObjectPlacer.class).removeBulk(treeNode);

		super.cleanup(app);
	}

	@Override
	public WorldType getType() {
		return WorldType.TRACK;
	}

	private static Vector3f getClosestPointTo(List<TrackSegment> segments, Vector3f pos) {
		Vector3f point = new Vector3f();
		float dist = Float.MAX_VALUE;

		for (TrackSegment seg : segments) {
			Vector3f curPoint = seg.getProjectedPointFrom(pos);
			float d = curPoint.distance(pos);
			if (d < dist) {
				point = curPoint;
				dist = d;
			}
		}

		return point;
	}

	private static Quaternion rot90 = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y);

	private static BiFunction<Vector3f, Vector3f, TrackSlice> CurveFunction() {
		return (Vector3f off, Vector3f tang) -> {
			// Vector3f angleOff = new Vector3f(0, -1, 0);
			Vector3f normal = rot90.mult(tang.normalize());
			return new TrackSlice(new Vector3f[] { off, // center of the road, not used for collision just position
					// off.add(normal.mult(6f)).add(angleOff), //these would be edges that go down
					off.add(normal.mult(5.5f)), off.add(normal.mult(-5.5f)),
					// off.add(normal.mult(-6f)).add(angleOff),
			});
		};
	}

	private static Geometry createQuad(AssetManager am, Vector3f[] v, ColorRGBA colour) {
		Mesh mesh = Geo.createQuad(v);
		Geometry geo = new Geometry("Quad", mesh);

		Material mat = null;
		if (colour != null) {
			mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
			mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
			mat.setColor("Color", colour);
		} else {
			mat = new Material(am, "Common/MatDefs/Light/Lighting.j3md");
			mat.setTexture("DiffuseMap", am.loadTexture("image/asphalt_tile.jpg"));
			mat.setBoolean("UseMaterialColors", true);
			mat.setColor("Diffuse", ColorRGBA.White);
			mat.setColor("Specular", ColorRGBA.White);
			mat.setFloat("Shininess", 0); // none
		}
		geo.setShadowMode(ShadowMode.Receive);
		geo.setMaterial(mat);

		return geo;
	}

	private static void setTerrainHeights(TerrainTrackHelper helper, List<Vector3f[]> quads,
			Function<Vector3f[], Vector3f[]> order) {
		List<Vector3f> posList = new LinkedList<>();

		for (Vector3f[] quad : quads) {

			quad = order.apply(quad);

			float[] box = Trig.boundingBoxXZ(quad);
			box[0] -= 1; // extend the extends so they cover it completely
			box[1] -= 1;
			box[2] += 1;
			box[3] += 1;
			for (int i = (int) box[0]; i < box[2]; i++) {
				for (int j = (int) box[1]; j < box[3]; j++) {
					Vector3f pos = new Vector3f(i, 0, j);

					// use the jme3 library method for point in triangle
					if (FastMath.pointInsideTriangle(H.v3tov2fXZ(quad[0]), H.v3tov2fXZ(quad[2]), H.v3tov2fXZ(quad[3]),
							H.v3tov2fXZ(pos)) != 0) {
						pos.y = Trig.heightInTri(quad[0], quad[2], quad[3], pos);
						posList.add(pos);

					} else if (FastMath.pointInsideTriangle(H.v3tov2fXZ(quad[0]), H.v3tov2fXZ(quad[2]),
							H.v3tov2fXZ(quad[1]), H.v3tov2fXZ(pos)) != 0) {
						pos.y = Trig.heightInTri(quad[0], quad[2], quad[1], pos);
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

	@Override
	public Transform start(int i) {
		return this.getStart();
	}

	@Override
	public Vector3f[] checkpoints() {
		return this.controlPoints.toArray(new Vector3f[0]);
	}
}
