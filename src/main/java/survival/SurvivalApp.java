package survival;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.app.SimpleApplication;
import com.jme3.audio.AudioListenerState;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.BulletAppState.ThreadingType;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.GuiGlobals;

import rallygame.car.CarManager;
import rallygame.car.data.CarDataLoader;
import rallygame.effects.FilterManager;
import rallygame.effects.ParticleAtmosphere;
import rallygame.game.DebugAppState;
import rallygame.service.ConstantChecker;
import rallygame.service.ObjectPlacer;
import rallygame.service.WorldGuiText;
import rallygame.service.ray.SceneRaycaster;

public class SurvivalApp extends SimpleApplication {

    public static void main(String[] args) {
        boolean isDebug = java.lang.management.ManagementFactory.getRuntimeMXBean(). getInputArguments().toString().contains("-agentlib:jdwp");

        SurvivalApp app = new SurvivalApp();
        app.setDisplayStatView(isDebug);
        app.start();
    }

    public static final Vector3f GRAVITY = new Vector3f(0, -9.81f, 0); // yay its down
    public static final String PROJECT_VERSION = "v0.2.0 (2022-05-27)";

    private Flow flow;

    public SurvivalApp() {
        super(new ParticleAtmosphere()
                , new AudioListenerState()
                , new FilterManager()
                , new CarManager(new CarDataLoader(), 0.6f)
                , new DebugAppState()
                , new ObjectPlacer(true)
                , new ConstantChecker()
                , new WorldGuiText()
                , new com.jme3.app.StatsAppState()
                , new SceneRaycaster()
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
        SurvivalUiStyle.load(assetManager);

        // Init the Physics space with better defaults
        // BulletAppState needs to wait until after the app is initialised, so can't be
        // called from the constructor
        BulletAppState bullet = new BulletAppState();
        bullet.setThreadingType(ThreadingType.PARALLEL);
        getStateManager().attach(bullet);

        //set things after attaching
        bullet.getPhysicsSpace().setAccuracy(1f / 60f); // physics rate
        bullet.getPhysicsSpace().setGravity(GRAVITY);

        flow = new Flow(this, PROJECT_VERSION);
    }
    

    @Override
    public void update() {
        super.update();
    }

    @Override
    public void destroy() {
        if (flow != null)
            flow.cleanup();

        super.destroy();
    }
}