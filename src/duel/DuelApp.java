package duel;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.SimpleApplication;
import com.jme3.app.StatsAppState;
import com.jme3.audio.AudioListenerState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.BulletAppState.ThreadingType;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.GuiGlobals;

import car.CarBuilder;
import effects.FilterManager;
import effects.ParticleAtmosphere;
import game.DebugAppState;
import helper.Log;

public class DuelApp extends SimpleApplication {

    public static void main(String[] args) {
        DuelApp app = new DuelApp();
        app.setDisplayStatView(true);
        app.start();
    }

    public static final Vector3f GRAVITY = new Vector3f(0, -9.81f, 0); // yay its down

    private DuelFlow flow;

    public DuelApp() {
        super(new ParticleAtmosphere()
                , new AudioListenerState()
                , new StatsAppState()
                , new FilterManager()
                , new CarBuilder()
                , new DebugAppState()
        );
    }

    @Override
    public void simpleInitApp() {
        Logger.getLogger("com.jme3.scene.plugins.blender").setLevel(Level.WARNING); // ignore blender warnings

        inputManager.setCursorVisible(true);
        inputManager.deleteMapping(INPUT_MAPPING_EXIT); // no esc close pls

        // initialize Lemur (the GUI manager)
        GuiGlobals.initialize(this);
        // Load my duel Lemur style
        DuelUiStyle.load(assetManager);

        // Init the Physics space with better defaults
        // BulletAppState needs to wait until after the app is initialised, so can't be
        // called from the constructor
        BulletAppState bullet = new BulletAppState();
        // bullet.setSpeed(0.1f); //physics per second rate
        // bullet.setDebugEnabled(true); //show bullet wireframes
        bullet.setThreadingType(ThreadingType.PARALLEL);
        getStateManager().attach(bullet);

        //set things after attaching
        bullet.getPhysicsSpace().setAccuracy(1f / 120f); // physics rate
        bullet.getPhysicsSpace().setGravity(GRAVITY);


        flow = new DuelFlow(this);
    }
    

    @Override
    public void update() {
        super.update();

        //prevent any really dumb stuff with Vector3f
        if (Vector3f.ZERO.length() != 0) {
            Log.e("Vector3f.ZERO is not zero!!!!, considered a fatal error.");
            System.exit(342);
        }
    }

    @Override
    public void destroy() {
        super.destroy();

        //cleanup Duelflow
        flow.cleanup();
    }
}