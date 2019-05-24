package game;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Triangle;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.Mesh.Mode;
import com.jme3.scene.VertexBuffer.Type;
import com.jme3.util.BufferUtils;

import helper.H;

public class WireframeHighlighter {

	public static final int LINE_WIDTH = 3;
	private static final float DIFF = 0.91f;
	//TODO cache the wireframes some how
	//TODO whatever the race drive mode is on doesn't have lines
	
	public static Spatial create(AssetManager am, String model, ColorRGBA base, ColorRGBA highlight) {
		return create(am, am.loadModel(model), base, highlight);
	}
	public static Spatial create(AssetManager am, Spatial s, ColorRGBA base, ColorRGBA highlight) {
		if (s == null)
			return s;
		
		if (s instanceof Geometry) {
			Node n = new Node();
			n.attachChild(s);
			n.attachChild(_do(am, (Geometry)s, base, highlight));
			return s;
		}
		Node n = (Node)s;
		List<Geometry> gList = H.getGeomList(n);
		for (Geometry g: gList) {
			Mesh m = g.getMesh();
			if (m.getMode() != Mode.Triangles) {
				addWireframeMat(am, g, highlight);
				continue; //just blindly add?
			}
			
			g.getParent().attachChild(_do(am, g, base, highlight));
		}
		return n;
	}
	
	public static void addWireframeMat(AssetManager am, Geometry g, ColorRGBA highlight) {
		Material mat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		mat.setColor("Color", highlight);
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		mat.getAdditionalRenderState().setWireframe(true);
		mat.getAdditionalRenderState().setLineWidth(LINE_WIDTH);
		g.setMaterial(mat);
	}
	
	private static Geometry _do(AssetManager am, Geometry g, ColorRGBA base, ColorRGBA highlight) {
		// 1 load
		Material baseMat = new Material(am, "Common/MatDefs/Misc/Unshaded.j3md");
		baseMat.setColor("Color", base);
		baseMat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		g.setMaterial(baseMat);

		// 2 find all the edges which are only connected to one polygon
		Mesh highlightMesh = findAllHighlightEdges(g.getMesh());
		
		// 3 make a new Geometry out of them
		Geometry highlightGeom = new Geometry("highlight", highlightMesh);
		highlightGeom.scale(1.0001f);
		addWireframeMat(am, highlightGeom, highlight);
		
		App.rally.getRootNode().attachChild(highlightGeom);
		
		return highlightGeom;
	}
	
	private static Mesh findAllHighlightEdges(Mesh m) {
		// Steps for this method:
		// read edges->triangles into a map: edge,list<tri>
		// iterate through the edges
		// find any edges with only one triangle and add to list
		// find any edges with a large difference between the triangle normals and add it as well
		Map<Edge, List<Triangle>> edgeMap = new HashMap<Edge, List<Triangle>>();

		int count = m.getTriangleCount();
		for (int i = 0; i < count; i++) {
			Triangle tri = new Triangle();
			m.getTriangle(i, tri);

			//add edges
			addToMap(edgeMap, new Edge(tri.get1(), tri.get2()), tri);
			addToMap(edgeMap, new Edge(tri.get3(), tri.get2()), tri);
			addToMap(edgeMap, new Edge(tri.get1(), tri.get3()), tri);
		}

		List<Edge> edges = new LinkedList<>();
		for (Entry<Edge, List<Triangle>> a : edgeMap.entrySet()) {
			//detect empty edges (only have a triangle on one side)
			if (a.getValue().size() == 1) {
				edges.add(a.getKey());
			}
			
			//detect large normal differences
			if (a.getValue().size() == 2) {
				//has 2 triangles
				Triangle tri1 = a.getValue().get(0);
				tri1.calculateNormal();
				Triangle tri2 = a.getValue().get(1);
				tri2.calculateNormal();
				
				Vector3f normal1 = tri1.getNormal();
				Vector3f normal2 = tri2.getNormal();
				
				if (normal1.dot(normal2) < DIFF) {
					edges.add(a.getKey());
				}
			}
		}

		Vector3f[] vertices = new Vector3f[edges.size()*2];
		int i = 0;
		for (Edge e: edges) {
			vertices[i] = e.a;
			vertices[i+1] = e.b;
			i+=2;
		}

		Mesh newMesh = new Mesh();
		newMesh.setMode(Mode.Lines);
		newMesh.setBuffer(Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
		newMesh.updateBound();
		return newMesh;
	}
	
	private static void addToMap(Map<Edge, List<Triangle>> map, Edge edge, Triangle tri) {
		if (map.containsKey(edge)) {
			map.get(edge).add(tri);
		} else {
			map.put(edge, new LinkedList<>(Arrays.asList(tri)));
		}
	}
}

//private
class Edge {
    final Vector3f a;
    final Vector3f b;
    
    public Edge(Vector3f a, Vector3f b) {
        //force the smaller number first
        if (a.x + a.y + a.z <= b.x + b.y + b.z) {
            this.a = a;
            this.b = b;
        } else {
            this.a = b;
            this.b = a;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Edge))
            return false;
        Edge e = (Edge)obj;
        if (e == obj) //object ref is the same
            return true;
        
        //normal equals
        if (a.equals(e.a) && b.equals(e.b)) {
            return true;
        }

        return false;
    }

    @Override
    public int hashCode() {
    	return this.b.toString().hashCode() + this.a.toString().hashCode();
        //i am kind of annoyed at this as using a 'correct' number based hash was causing too many collisions
    }

    @Override
    public String toString() {
        return "Edge: a" + this.a + " b" + this.b + " hash"+this.hashCode();
    }
}