package effects;

import com.jme3.app.Application;
import com.jme3.app.state.AbstractAppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh.Type;
import com.jme3.effect.shapes.EmitterSphereShape;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

public class ParticleAtmosphere extends AbstractAppState {

	private Vector3f prevPos;
	private Node root;
	private ParticleEmitter particles;
	
	public ParticleAtmosphere(Node root) {
		this.root = root;
	}
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
		particles = new ParticleEmitter("particles", Type.Triangle, 0);
		
		particles.setGravity(0, 0, 0);
		particles.setLowLife(7);
		particles.setHighLife(2);
		
		particles.setStartSize(0.01f);
		particles.setEndSize(0.01f);
		
		//spread out the initial spawn positions
		particles.setShape(new EmitterSphereShape(Vector3f.ZERO, 150f));
		particles.setInWorldSpace(true); //don't move them after
		
		particles.setStartColor(ColorRGBA.White);
		particles.setEndColor(ColorRGBA.White);
		
		Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Particle.j3md");
		mat.setTexture("Texture", app.getAssetManager().loadTexture("assets/image/solid-white.png"));
		particles.setMaterial(mat);
		
		particles.setNumParticles(5000);

		root.attachChild(particles);
	}
	
	@Override
	public void cleanup() {
		particles.removeFromParent();
	}
	
	@Override
	public void update(float tpf) {
		if (prevPos == null)
			prevPos = new Vector3f();
		
		//get vel of node
		float speed = root.getWorldTranslation().subtract(prevPos).length() / tpf;
		particles.setParticlesPerSec(25*speed);
		
		prevPos = root.getWorldTranslation().clone(); //doesn't work without clone, suspect world translation uses the same object
	}
}
