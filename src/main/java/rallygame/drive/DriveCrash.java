package rallygame.drive;

import rallygame.world.StaticWorld;
import rallygame.world.StaticWorldBuilder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.DefaultRangedValueModel;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.ProgressBar;
import com.simsilica.lemur.component.QuadBackgroundComponent;

import rallygame.car.CarManager;
import rallygame.car.ai.DriveAtAI;
import rallygame.car.data.Car;
import rallygame.car.data.CarDataConst;
import rallygame.car.data.CarModelData.CarPart;
import rallygame.car.ray.RayCarControl;
import rallygame.effects.LoadModelWrapper;
import rallygame.game.DebugAppState;
import rallygame.game.IDriveDone;
import rallygame.helper.Colours;
import rallygame.helper.Geo;
import rallygame.helper.Rand;
import rallygame.service.IRayCarCollisionListener;
import rallygame.service.RayCarCollisionService;
import rallygame.service.Screen;

public class DriveCrash extends DriveBase implements IRayCarCollisionListener {

    private static String POLICE_TEXT = "You are in trouble from the 'physics' police.\n"
            + "They are trying to nab you for breaking standard physics laws,\n specifcally for being rediculously bouncy.\n"
            + "Your existance is brief, so try and survive\n[reset the death timer touching them]\n";

    private final Car them;
    private final int themCount;

    private final Map<RayCarControl, Instant> hitList;
    private static final Duration HIT_TIMEOUT = Duration.ofSeconds(5);
    private RayCarCollisionService carCollisionState;

    private int totalFlipped;
    private int frameCount = 0; // global frame timer

    private float loseTimer;
    private static float TIMEOUT = 10;

    private Container progressContainer;
    private Label label;
    private ProgressBar progressBar;

    public DriveCrash(IDriveDone done) {
        super(done, Car.Runner, new StaticWorldBuilder(StaticWorld.duct2));
        this.them = Car.Rally;
        this.themCount = 10;
        this.totalFlipped = 0;

        this.hitList = new HashMap<>();
    }

    @Override
    public void initialize(Application app) {
        super.initialize(app);
        var cm = getState(CarManager.class);

        this.carCollisionState = new RayCarCollisionService(this, cm);
        getState(BulletAppState.class).getPhysicsSpace().addCollisionListener(carCollisionState);

        progressContainer = new Container();
        label = progressContainer.addChild(new Label(""));
        progressBar = createBar(0);
        progressContainer.addChild(progressBar);

        ((SimpleApplication) app).getGuiNode().attachChild(progressContainer);
    }

    @Override
    public void cleanup(Application app) {
        ((SimpleApplication) app).getGuiNode().detachChild(progressContainer);
        getState(BulletAppState.class).getPhysicsSpace().removeCollisionListener(this.carCollisionState);

        super.cleanup(app);
    }

    public void update(float tpf) {
        AppSettings settings = getApplication().getContext().getSettings();
        new Screen(settings).topRightMe(progressContainer);

        if (!this.isEnabled()) return;
        
        frameCount++;

        var cm = getState(CarManager.class);
        Transform start = world.getStart();
        if (cm.getCount() < (themCount + 1) && frameCount % 60 == 0) {
            Vector3f spawn = Rand.randV3f(10, true);
            spawn.x = Math.round(spawn.x) * 2;
            spawn.y = 0; // maybe ray cast from very high to find the ground height?
            spawn.z = Math.round(spawn.z) * 2;

            CarDataConst data = cm.loadData(them, true);
            RayCarControl c = cm.addCar(data, spawn, start.getRotation(), false);
            c.attachAI(new DriveAtAI(c, cm.getPlayer().getPhysicsObject()), true);
        }

        // check if any hit ones are upside down, if so kill them
        List<RayCarControl> toKill = new ArrayList<RayCarControl>();
        for (RayCarControl c : this.hitList.keySet())
            if (c.up != null && c.up.y < 0 && c != cm.getPlayer()) // not the player
                toKill.add(c);
        for (RayCarControl c : cm.getAll())
            if (c.location.y < -100)
                toKill.add(c);
        for (RayCarControl c : toKill) {
            cm.removeCar(c);
            if (hitList.containsKey(c)) {
                totalFlipped++;
                hitList.remove(c);
            }
        }

        label.setText(POLICE_TEXT + "Total Flipped: " + totalFlipped);

        // update timeout mode only if we have an opponent
        if (cm.getCount() > 1) {
            loseTimer += tpf;
            if (loseTimer > TIMEOUT) // you lose
                cm.setEnabled(false);
        }

        this.progressBar.setModel(new DefaultRangedValueModel(0, 1, loseTimer / TIMEOUT));
        this.progressBar.getValueIndicator().setBackground(new QuadBackgroundComponent(Colours.getOnGreenToRedScale(loseTimer / TIMEOUT)));

        //update the visual color of them when they come back from blue
        Instant now = Instant.now();
        for (Entry<RayCarControl, Instant> entry: this.hitList.entrySet()) {
            if (entry.getValue().isBefore(now))
                updateToColour(entry.getKey(), ColorRGBA.White);
        }
    }
    
    private void updateToColour(RayCarControl car, ColorRGBA colour) {
        Spatial s = Geo.getNamedSpatial(car.getRootNode(), CarPart.Chassis.getPartName());
        LoadModelWrapper.setPrimaryColour(s, colour);
    }

    @Override
    public void playerCollision(RayCarControl player, RayCarControl them, Vector3f normalInWorld, Vector3f themLocalPos, float appliedImpulse) {
        Instant now = Instant.now();
        updateToColour(them, ColorRGBA.Blue);

        if (!this.hitList.containsKey(them))
            this.hitList.put(them, now.plus(HIT_TIMEOUT));
        else {
            //check if its been 5 seconds since the last collision
            if (this.hitList.get(them).isBefore(now)) {
                this.hitList.put(them, now.plus(HIT_TIMEOUT));
                return; //prevent any more collisions
            }
        }

        loseTimer = 0;

        PhysicsRigidBody prb = them.getPhysicsObject();
        Vector3f newDir = Vector3f.UNIT_Y.mult(FastMath.clamp(appliedImpulse, 3000, 5000) * 2f);
        // Vector3f newDir = normalInWorld.mult(FastMath.clamp(appliedImpulse, 3000, 10000) * 5);
        prb.applyImpulse(newDir, themLocalPos);

        Vector3f posInWorld = prb.getPhysicsRotation().mult(themLocalPos).add(prb.getPhysicsLocation());
        getState(DebugAppState.class).drawArrow("playerCollision", ColorRGBA.Orange, posInWorld, newDir.normalize());
    }

    private ProgressBar createBar(float value) {
        ProgressBar pb = new ProgressBar();
        pb.setProgressPercent(value);
        pb.setModel(new DefaultRangedValueModel(0, 1, value));
        pb.getValueIndicator().setBackground(new QuadBackgroundComponent(Colours.getOnGreenToRedScale(value)));
        return pb;
    }
}
