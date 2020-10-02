package rallygame.car.ray;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioNode;
import com.jme3.audio.AudioSource.Status;
import com.jme3.math.FastMath;

public class RayCarVisuals {

    private final RayWheelControl[] wheelControls;
    private final RayCarControl car;

    // sound stuff
    private AudioNode engineSound;

    public RayCarVisuals(SimpleApplication app, RayCarControl car) {
        this.car = car;

        // init visual wheels
        this.wheelControls = new RayWheelControl[4];
        for (int i = 0; i < wheelControls.length; i++) {
            wheelControls[i] = new RayWheelControl(app, car.wheels[i], car.getRootNode(), car.carData.wheelOffset[i]);
        }
    }

    public void viewUpdate(float tpf) {
        if (engineSound != null && engineSound.getStatus() == Status.Playing) {
            // if sound exists
            float pitch = FastMath.clamp(0.5f + 1.5f * ((float) car.curRPM / (float) car.carData.e_redline), 0.5f, 2);
            engineSound.setPitch(pitch);

            float volume = 0.75f + car.accelCurrent * 0.25f;
            engineSound.setVolume(volume);
        }

        for (int i = 0; i < this.wheelControls.length; i++)
            this.wheelControls[i].viewUpdate(tpf, car.rbc.getLinearVelocity(), car.carData.susByWheelNum(i).min_travel);
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
        for (RayWheelControl c : this.wheelControls)
            c.reset();
    }
    
    public void cleanup(Application app) {
        for (RayWheelControl w : this.wheelControls) {
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
}
