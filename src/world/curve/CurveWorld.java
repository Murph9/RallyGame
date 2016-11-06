package world.curve;

import java.util.ArrayList;
import java.util.List;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector2f;
import com.jme3.renderer.Camera;
import com.jme3.renderer.ViewPort;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Line;
import com.jme3.terrain.geomipmap.TerrainLodControl;
import com.jme3.terrain.geomipmap.TerrainQuad;
import com.jme3.terrain.heightmap.AbstractHeightMap;
import com.jme3.terrain.heightmap.HeightMap;
import com.jme3.terrain.heightmap.HillHeightMap;
import com.jme3.terrain.heightmap.ImageBasedHeightMap;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;

import game.App;
import game.H;
import world.World;
import world.WorldType;

/*
Basic idea is:
Make road our of random beizer curves
- these beizer curves to have road width (use the code from the 3d course)
  + WIDTH of basic road, similar to TDU: 
  		{45'(edge) 2.5(shoulder) | 4.5 (lane) || 4.5 | 2.5 45'}
- eventually overlay it on terrain
 + flatten the terrain under the road so it 'fits'
- add grass and trees TDU like

Later Things:
- intesections
- other kinds of road
 */

public class CurveWorld implements World {
	private boolean isInit;
	
	private Node rootNode;
	private PhysicsSpace phys;
	
	private AbstractHeightMap map;
	
	public CurveWorld() {
		rootNode = new Node("curveWorldRoot");
	}
	
	@Override
	public WorldType getType() {
		return WorldType.CURVE;
	}

	@Override
	public boolean isInit() {
		return isInit;
	}

	@Override
	public Node init(PhysicsSpace space, ViewPort view) {
		isInit = true;
		phys = space;
		
		AssetManager am = App.rally.getAssetManager();
		
		//create the terrain (from terrain world)
		Material mat_terrain = new Material(am, "Common/MatDefs/Terrain/Terrain.j3md");
		Material mat_temp = new Material(am, "Common/MatDefs/Misc/ShowNormals.j3md");
		
		Texture heightMapImage = am.loadTexture("Textures/Terrain/splat/mountains512.png");
		map = new ImageBasedHeightMap(heightMapImage.getImage());
		try {
			map = new HillHeightMap(1025, 1000, 50, 100, FastMath.rand.nextLong());
//			heightMap = new HillHeightMap(1025, 1000, 50, 100, (byte) 3);
		} catch (Exception e) {
			e.printStackTrace();
		}
	    map.setHeightScale(0.3f);
	    map.load();
	    map.normalizeTerrain(10);
	    
	    TerrainQuad terrain = new TerrainQuad("my terrain", 65, 513, map.getHeightMap());
	    terrain.setMaterial(mat_temp);
	    terrain.setLocalTranslation(0, 0, 0);
	    terrain.setShadowMode(ShadowMode.Receive);
	    rootNode.attachChild(terrain);
	    List<Camera> cameras = new ArrayList<Camera>();
	    cameras.add(view.getCamera());
	    TerrainLodControl control = new TerrainLodControl(terrain, cameras);
	    terrain.addControl(control);
	    RigidBodyControl rbc = new RigidBodyControl(0.0f);
	    terrain.addControl(rbc);
	    phys.add(rbc);
	    //end terrain
		
		Material matfloor = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		matfloor.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		matfloor.setColor("Color", ColorRGBA.Green);
		
		Box start = new Box(10, 0.25f, 10); //Something to spawn on (or in hint hint)
		Geometry startGeometry = new Geometry("Starting Box", start);
		startGeometry.setMaterial(matfloor);
		startGeometry.setLocalTranslation(0, -0.3f, 0);
		startGeometry.addControl(new RigidBodyControl(0));
		
		this.rootNode.attachChild(startGeometry);
		this.phys.add(startGeometry);
		
		Vector3f[] points = new Vector3f[] { 
				Vector3f.ZERO, 
				new Vector3f(0,map.getInterpolatedHeight(0,50),50), 
				new Vector3f(0,map.getInterpolatedHeight(0,25),25), 
				new Vector3f(0,map.getInterpolatedHeight(0,75),75),
			};
		drawMeACurve(new BezierCurve(points), true);

		int size = map.getSize();
		
		for (int i = 0; i < 10; i++) {
			points[1] = points[3].add(points[3].subtract(points[2]));
			points[0] = points[3];
			
			int x = FastMath.rand.nextInt(100)+50;
			int z = FastMath.rand.nextInt(100)+50;
			
			int xm = x - (FastMath.rand.nextInt(20)+50);
			int zm = z - (FastMath.rand.nextInt(20)+50);
			
			if (x+points[0].x < size && zm+points[0].z < size) {
				points[3] = points[0].add(new Vector3f(x, map.getInterpolatedHeight(x+points[0].x, z+points[0].z), z));	
			} else {
				points[3] = points[0].add(new Vector3f(x, 0, z));
			}
			if (xm+points[0].x < size && zm+points[0].z < size) {
				points[2] = points[0].add(new Vector3f(xm, map.getInterpolatedHeight(xm+points[0].x, zm+points[0].z), zm));
			} else {
				points[2] = points[0].add(new Vector3f(xm, 0, zm));
			}
			drawMeACurve(new BezierCurve(points), true);
		}
		
		return rootNode;
	}

	@Override
	public Node getRootNode() {
		return rootNode;
	}

	@Override
	public Vector3f getWorldStart() {
		return new Vector3f(0,0.5f,0);
	}

	@Override
	public Matrix3f getWorldRot() {
		return new Matrix3f(Matrix3f.IDENTITY);
	}

	@Override
	public void update(float tpf, Vector3f playerPos, boolean force) {
		
	}

	@Override
	public void reset() {
		
	}

	@Override
	public void cleanup() {
		isInit = false;
	}

	@Override
	public Vector3f getNextPieceClosestTo(Vector3f pos) {
		return null;
	}

	
	private void drawMeACurve(BezierCurve bc, boolean helpPoints) {
		float width = 5;
		
		BSegment[] nodes = bc.calcPoints((Vector3f off, Vector3f tang) -> { 
			Vector3f normal = new Quaternion().fromAngleAxis(FastMath.HALF_PI, Vector3f.UNIT_Y).mult(tang.normalize());
			return new BSegment(new Vector3f[] { 
					off.add(normal.mult(width)), 
					//off, //disabled for nice textures
					off.add(normal.mult(-1*width))
				});
		});
		for (int i = 1; i < nodes.length; i++) {
			for (int j = 1; j < nodes[i].v.length; j++) {
				Vector3f[] vs = new Vector3f[] {
					nodes[i-1].v[j-1],
					nodes[i-1].v[j],
					nodes[i].v[j-1],
					nodes[i].v[j],
				};
				
//				for (Vector3f v: vs)
//					v.y = map.getInterpolatedHeight(FastMath.abs(v.x), FastMath.abs(v.z));
				//TODO good idea very bad guess
				
				drawMeAQuad(vs, ColorRGBA.Black);
			}
		}
		
		//TODO needs to take into account the height of the terrain
		
		if (helpPoints) {
			Vector3f[] nds = bc.getNodes();
			for (Vector3f v: nds)
				drawMeAVerySmallBox(v, ColorRGBA.LightGray);
			drawMeALine(nds[0], nds[1], ColorRGBA.Blue);
			drawMeALine(nds[2], nds[3], ColorRGBA.Blue);
		}
	}
		
	private void drawMeAVerySmallBox(Vector3f pos, ColorRGBA colour) {
		Box b = new Box(pos, 0.1f, 0.1f, 0.1f);
		Geometry geometry = new Geometry("box", b);
		Material mat = new Material(App.rally.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", colour);
		geometry.setMaterial(mat);
		rootNode.attachChild(geometry);
	}
	
	private void drawMeALine(Vector3f start, Vector3f end, ColorRGBA colour) {
		Line line = new Line(start, end);
		line.setLineWidth(2);
		Geometry geometry = new Geometry("line", line);
		Material mat = new Material(App.rally.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", colour);
		geometry.setMaterial(mat);
		rootNode.attachChild(geometry);
	}

	private void drawMeAQuad(Vector3f[] v, ColorRGBA colour) {
		if (v == null || v.length != 4) {
			H.e("CurveWorld: Not the correct length drawMeAQuad()");
			return;
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
		
		Geometry geo = new Geometry("MyMesh", mesh);
		
		AssetManager am = App.rally.getAssetManager();
		
		Material mat = new Material(am, "Common/MatDefs/Light/Lighting.j3md");
		mat.setTexture("DiffuseMap", am.loadTexture("assets/image/asphalt_tile.jpg"));
		mat.setBoolean("UseMaterialColors", true);
		mat.setColor("Diffuse", ColorRGBA.White);
		mat.setColor("Specular", ColorRGBA.White);
		mat.setFloat("Shininess", 0); //none
		
		geo.setShadowMode(ShadowMode.Receive);
		geo.setMaterial(mat);
		
		CollisionShape col = CollisionShapeFactory.createMeshShape(geo);
		RigidBodyControl c = new RigidBodyControl(col, 0);
		
		rootNode.attachChild(geo);
		phys.add(c);
	}
	
}
