package world;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.math.Vector3f;
import com.jme3.renderer.ViewPort;
import com.jme3.scene.Node;

import helper.H;

public class StaticWorldBuilder extends World {

	private StaticWorld world;
	
	public StaticWorldBuilder(StaticWorld world) {
		this.world = world;
	}
	
	public WorldType getType() {
		return WorldType.STATIC;
	}

	@Override
	public Node init(PhysicsSpace phys, ViewPort view) {
		if (isInit) {
			H.e("WAS INITED TWICE");
		}
		
		this.isInit = true;
		this.phys = phys;
		this.rootNode = new Node();
		
		StaticWorldHelper.addStaticWorld(rootNode, phys, world, true);
		return rootNode;
	}

	@Override
	public Vector3f getWorldStart() {
		return world.start;
	}

	@Override
	public void update(float tpf, Vector3f playerPos, boolean force) {
		//doesn't ever need to update yet
	}

	@Override
	public void reset() {
		//reset is a play action, so its usually reset dynamic stuff which this doesn't have
	}

	@Override
	public void cleanup() {
		StaticWorldHelper.removeStaticWorld(rootNode, phys, world);
		rootNode.detachAllChildren();
	}
}



