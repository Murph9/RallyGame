package world;

import game.Rally;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.material.Material;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.renderer.queue.RenderQueue.ShadowMode;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

public class StaticWorldBuilder extends Node {

	StaticWorld world;
	
	public StaticWorldBuilder(Rally r, StaticWorld world, boolean ifShadow) {
		this.world = world;
		AssetManager as = r.getAssetManager();
		
		Material mat = new Material(as, "Common/MatDefs/Misc/ShowNormals.j3md");
		mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
		
	    //imported model
		Spatial worldNode = as.loadModel(world.name);
		if (worldNode instanceof Node) {
			for (Spatial s: ((Node) worldNode).getChildren()) {
				if (world.ifNeedsTexture) {
					s.setMaterial(mat);
				}
				addWorldModel(r, s, ifShadow);
			}
		} else {
			Geometry worldModel = (Geometry) as.loadModel(world.name);
			
			if (world.ifNeedsTexture) {
				worldModel.setMaterial(mat);
			}
			addWorldModel(r, worldModel, ifShadow);
		}
	}
	
	private void addWorldModel(Rally r, Spatial s, boolean ifShadow) {
		System.err.println("Adding: "+ s.getName());
		
		s.move(0,-5,0);
		s.scale(world.scale);
		
		CollisionShape world = CollisionShapeFactory.createMeshShape(s);
		RigidBodyControl landscape = new RigidBodyControl(world, 0);
		s.addControl(landscape);
		if (ifShadow) {
			s.setShadowMode(ShadowMode.Receive);
		}

		r.getPhysicsSpace().add(landscape);
		
		r.getRootNode().attachChild(s);
	}
}
