package effects;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh.Type;
import com.jme3.effect.shapes.EmitterSphereShape;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

public class ParticleAtmosphere extends BaseAppState {

	private final static int PARTICLE_MULT = 50;
	private final static int PARTICLE_MAX = 5000;

	private Vector3f prevPos;
	private Camera cam;
	private ParticleEmitter particles;
	
	public ParticleAtmosphere() {}
	
	@Override
	public void initialize(Application app) {
		cam = app.getCamera();

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
		
		particles.setNumParticles(PARTICLE_MAX);

		((SimpleApplication)app).getRootNode().attachChild(particles);
	}
	
	@Override
	public void cleanup(Application app) {
		particles.removeFromParent();
	}
	
	@Override
	public void update(float tpf) {
		if (!this.isEnabled())
			return;

		if (prevPos == null)
			prevPos = new Vector3f();
		
		//get vel of node
		float speed = cam.getLocation().subtract(prevPos).length() / tpf;
		particles.setParticlesPerSec(PARTICLE_MULT*speed);
		
		prevPos = cam.getLocation().clone();
		particles.setLocalTranslation(prevPos);
	}

	@Override
	protected void onEnable() {
	}
	@Override
	protected void onDisable() {
	}
}
