package rallygame.duel;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioListenerState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.BulletAppState.ThreadingType;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.GuiGlobals;

import rallygame.car.CarBuilder;
import rallygame.car.data.CarDataLoader;
import rallygame.effects.FilterManager;
import rallygame.effects.ParticleAtmosphere;
import rallygame.game.DebugAppState;
import rallygame.service.ConstantChecker;
import rallygame.service.ObjectPlacer;
import rallygame.service.WorldGuiText;

public class DuelApp extends SimpleApplication {

    public static void main(String[] args) {
        DuelApp app = new DuelApp();
        app.setDisplayStatView(true);
        app.start();
    }

    public static final Vector3f GRAVITY = new Vector3f(0, -9.81f, 0); // yay its down
    public static final String PROJECT_VERSION = "v0.1.3 (2020-04-07)";

    private DuelFlow flow;

    public DuelApp() {
        super(new ParticleAtmosphere()
                , new AudioListenerState()
                , new FilterManager()
                , new CarBuilder(0.4f, new CarDataLoader())
                , new DebugAppState()
                , new ObjectPlacer(true)
                , new ConstantChecker()
                , new WorldGuiText()
        );
    }

    @Override
    public void simpleInitApp() {
        Logger.getLogger("com.jme3.scene.plugins.OBJLoader").setLevel(Level.SEVERE);

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


        flow = new DuelFlow(this, PROJECT_VERSION);
    }
    

    @Override
    public void update() {
        super.update();
    }

    @Override
    public void destroy() {
        super.destroy();

        //cleanup Duelflow
        flow.cleanup();
    }
}