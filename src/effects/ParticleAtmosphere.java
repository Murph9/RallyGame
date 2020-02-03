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

    private final static float FORWARD_FACTOR = 0.5f;
	private final static int PARTICLE_MULT = 50;
	private final static int PARTICLE_MAX = 5000;

    private final float velocityFactor;
    private final int particleMaximum;
    private final float particleMult;

	private Vector3f prevPos;
	private Camera cam;
	private ParticleEmitter particles;
	
    public ParticleAtmosphere() {
        this(FORWARD_FACTOR, PARTICLE_MAX, PARTICLE_MULT);
    }
    
    public ParticleAtmosphere(float velocityFactor, int particleMaximum, float particleMult) {
        this.velocityFactor = velocityFactor;
        this.particleMaximum = particleMaximum;
        this.particleMult = particleMult;
    }
	
	@Override
	public void initialize(Application app) {
		cam = app.getCamera();

		particles = new ParticleEmitter("particles", Type.Triangle, 0);
		
        particles.setGravity(0, 0, 0);
		particles.setLowLife(7);
		particles.setHighLife(2);
		
		particles.setStartSize(0.02f);
		particles.setEndSize(0.01f);
		
		//spread out the initial spawn positions
		particles.setShape(new EmitterSphereShape(Vector3f.ZERO, 150f));
		particles.setInWorldSpace(true); //don't move them after
		
		particles.setStartColor(ColorRGBA.White);
		particles.setEndColor(ColorRGBA.White);
		
		Material mat = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Particle.j3md");
		mat.setTexture("Texture", app.getAssetManager().loadTexture("assets/image/solid-white.png"));
		particles.setMaterial(mat);
		
		particles.setNumParticles(particleMaximum);

		((SimpleApplication)app).getRootNode().attachChild(particles);
	}
	
	@Override
	public void cleanup(Application app) {
        particles.removeFromParent();
        particles = null;
	}
	
	@Override
	public void update(float tpf) {
		if (prevPos == null)
			prevPos = new Vector3f();
		
		Vector3f location = cam.getLocation().clone();
        
        Vector3f direction = location.subtract(prevPos);
        // get vel of node
		float speed = direction.length() / tpf;
		particles.setParticlesPerSec(particleMult * speed);
        
		//move the particle emitter based on the velocity of the camera
        particles.setLocalTranslation(location.add(direction.normalize().mult(speed * velocityFactor)));
        prevPos = location;
	}

	@Override
	protected void onEnable() {
        particles.setEnabled(true);
	}
	@Override
	protected void onDisable() {
        particles.setEnabled(false);
	}
}
