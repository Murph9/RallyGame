package rallygame.car.ray;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource.Status;
import com.jme3.math.FastMath;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import rallygame.effects.LoadModelWrapper;

public class RayCarVisuals {

    private final RayCarDebug debug;
    private final Node rootNode;
    private final RayWheelVisuals[] wheelControls;
    private final RayCarControl car;

    // sound stuff
    private AudioNode engineSound;

    public RayCarVisuals(SimpleApplication app, Spatial initialCarModel, RayCarControl car) {
        this.car = car;

        this.debug = new RayCarDebug(car.rayCar, false, app);

        var carData = car.getCarData();
        this.rootNode = new Node("Car:" + carData);
        this.rootNode.addControl(car.rayCar.rbc);

        // init visual wheels
        this.wheelControls = new RayWheelVisuals[4];
        for (int i = 0; i < wheelControls.length; i++) {
            wheelControls[i] = new RayWheelVisuals(app, car.rayCar.wheels[i],
                    carData.susByWheelNum(i), rootNode, carData.wheelOffset[i]);
        }

        Node carModel = LoadModelWrapper.create(app.getAssetManager(), initialCarModel, carData.baseColor, null);
        this.rootNode.attachChild(carModel);
    }

    public void viewUpdate(float tpf) {
        var carData = car.getCarData();

        if (engineSound != null && engineSound.getStatus() == Status.Playing) {
            var powered = car.getPoweredState();

            // if sound exists
            float pitch = FastMath.clamp(0.5f + 1.5f * ((float) powered.curRPM() / (float) carData.e_redline), 0.5f, 2);
            engineSound.setPitch(pitch);

            float volume = 0.75f + powered.accelCurrent() * 0.25f;
            engineSound.setVolume(volume);
        }

        for (int i = 0; i < this.wheelControls.length; i++)
            this.wheelControls[i].viewUpdate(tpf, car.vel);

        debug.update();
    }

    public void enableSound(boolean enabled) {
        if (engineSound == null)
            return;
        if (enabled)
            engineSound.play();
        else
            engineSound.pause();
    }

    public void reset() {
        // reset all skidmarks
        for (RayWheelVisuals c : this.wheelControls)
            c.reset();
    }
    
    public void cleanup(Application app) {
        for (RayWheelVisuals w : this.wheelControls) {
            w.cleanup();
        }

        if (this.engineSound != null) {
            car.getRootNode().detachChild(this.engineSound);
            app.getAudioRenderer().stopSource(engineSound);
            engineSound = null;
        }
    }

	public void giveSound(AudioNode audio) {
        if (engineSound != null)
            car.getRootNode().detachChild(engineSound); // in case we have 2 sounds running

        engineSound = audio;
        engineSound.setVelocityFromTranslation(true); // prevent camera based doppler?
        engineSound.setLooping(true);
        engineSound.play();
        car.getRootNode().attachChild(engineSound);
	}

	public Node getRootNode() {
		return this.rootNode;
	}
}
