package world;

import game.App;

import java.util.ArrayList;
import java.util.List;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;


public class StaticWorldHelper {

	private static StaticWorld curWorld; //assuming you can't have 2
	private static List<RigidBodyControl> landscapes = new ArrayList<RigidBodyControl>(5);
	private static List<Spatial> models = new ArrayList<Spatial>(5); //at least 5
	
	public static void addStaticWorld(Node node, PhysicsSpace phys, StaticWorld world, boolean ifShadow) {
		if (curWorld != null) { System.err.println("can't make a world until you remove the last one"); return; }
		curWorld = world;
		
		AssetManager as = App.rally.getAssetManager();
		
		Material mat = new Material(as, "Common/MatDefs/Misc/ShowNormals.j3md");
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		
	    //imported model
		Spatial worldNode = as.loadModel(curWorld.name);
		if (worldNode instanceof Node) {
			for (Spatial s: ((Node) worldNode).getChildren()) {
				if (curWorld.ifNeedsTexture) {
					s.setMaterial(mat);
				}
				addWorldModel(node, phys, s, ifShadow);
			}
		} else {
			Geometry worldModel = (Geometry) as.loadModel(curWorld.name);
			
			if (curWorld.ifNeedsTexture) {
				worldModel.setMaterial(mat);
			}
			addWorldModel(node, phys, worldModel, ifShadow);
		}
		
		System.err.println("Adding: "+ curWorld.name);
	}
	
	private static void addWorldModel(Node node, PhysicsSpace phys, Spatial s, boolean ifShadow) {
		s.scale(curWorld.scale);
		
		CollisionShape col = CollisionShapeFactory.createMeshShape(s);
		RigidBodyControl landscape = new RigidBodyControl(col, 0);
		s.addControl(landscape);
		if (ifShadow) {
			s.setShadowMode(ShadowMode.Receive);
		}

		landscapes.add(landscape);
		models.add(s);
		
		phys.add(landscape);
		node.attachChild(s);
	}
	
	
	public static void removeStaticWorld(Node node, PhysicsSpace phys, StaticWorld world) {
		for (RigidBodyControl r: landscapes) {
			phys.remove(r);
		}
		landscapes.clear();
		for (Spatial s: models) {
			node.detachChild(s);
		}
		models.clear();
		
		curWorld = null;
	}
}