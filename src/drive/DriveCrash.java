package drive;

import world.StaticWorld;
import world.StaticWorldBuilder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.DefaultRangedValueModel;
import com.simsilica.lemur.ProgressBar;
import com.simsilica.lemur.component.QuadBackgroundComponent;

import car.ai.DriveAtAI;
import car.data.Car;
import car.data.CarDataConst;
import car.ray.RayCarControl;
import game.DebugAppState;
import game.IDriveDone;
import helper.Colours;
import helper.Geo;
import helper.H;
import service.Screen;

public class DriveCrash extends DriveBase implements PhysicsCollisionListener {

    private static String POLICE_TEXT = "You are in trouble from the 'physics' police.\n"
        +"They are trying to nab you for breaking normal physics laws\n"
        +"specifcally for being rediculously bouncy.\n"
        +"Try and survive [reset it by touching them]\n";

    private final Car them;
    private final int themCount;

    private final List<RayCarControl> hitList;

    private int totalFlipped;
    private int frameCount = 0; // global frame timer
    
    private float loseTimer;
    private static float TIMEOUT = 10;

    private Container progressContainer;
    private ProgressBar progressBar;

    public DriveCrash(IDriveDone done) {
        super(done, Car.Runner, new StaticWorldBuilder(StaticWorld.duct2));
        this.them = Car.Rally;
        this.themCount = 10;
        this.totalFlipped = 0;

        this.hitList = new LinkedList<>();
    }

    @Override
    public void initialize(Application app) {
        super.initialize(app);
        getState(BulletAppState.class).getPhysicsSpace().addCollisionListener(this);

        progressContainer = new Container();
        progressBar = createBar(0);
        progressContainer.addChild(progressBar);

        AppSettings settings = app.getContext().getSettings();
        progressContainer.setPreferredSize(new Vector3f(settings.getWidth() / 3, settings.getHeight() / 15f, 0));
        
        new Screen(settings).bottomCenterMe(progressContainer);

        ((SimpleApplication) app).getGuiNode().attachChild(progressContainer);
    }

    @Override
    public void cleanup(Application app) {
        ((SimpleApplication) app).getGuiNode().detachChild(progressContainer);
        getState(BulletAppState.class).getPhysicsSpace().removeCollisionListener(this);
        super.cleanup(app);
    }

    public void update(float tpf) {
        super.update(tpf);
        frameCount++;

        PhysicsRigidBody playerBody = this.cb.get(0).getPhysicsObject();

        if (this.cb.getCount() < (themCount+1) && frameCount % 60 == 0) {
            Vector3f spawn = H.randV3f(10, true);
            spawn.x = Math.round(spawn.x)*2;
            spawn.y = 0; //maybe ray cast from very high to find the ground height?
            spawn.z = Math.round(spawn.z)*2;

            CarDataConst data = this.cb.loadData(them);
            RayCarControl c = this.cb.addCar(data, spawn, world.getStartRot(), false);
            c.attachAI(new DriveAtAI(c, playerBody), true);
        }

        // check if any hit ones are upside down, if so kill them
        List<RayCarControl> toKill = new ArrayList<RayCarControl>();
        for (RayCarControl c : this.hitList)
            if (c.up != null && c.up.y < 0 && c != this.cb.get(0))
                toKill.add(c);
        for (RayCarControl c : this.cb.getAll())
            if (c.location.y < -100)
                toKill.add(c);
        for (RayCarControl c : toKill) {
            cb.removeCar(c);
            if (hitList.contains(c)) {
                totalFlipped++;
                hitList.remove(c);
            }
        }

        if (this.menu.randomthing != null) {
            this.menu.randomthing.setText(POLICE_TEXT + "Total Flipped: " + totalFlipped);
        }

        //update timeout mode
        loseTimer += tpf;
        if (loseTimer > TIMEOUT) {
            // you lose
            this.cb.setEnabled(false);
        }
        
        this.progressBar.setModel(new DefaultRangedValueModel(0, 1, loseTimer/TIMEOUT));
        this.progressBar.getValueIndicator().setBackground(new QuadBackgroundComponent(Colours.getOnGreenToRedScale(loseTimer / TIMEOUT)));
    }

    private RayCarControl getCarFrom(Spatial node) {
        for (RayCarControl car : this.cb.getAll()) {
            if (Geo.hasParentNode(node, car.getRootNode())) {
                return car;
            }
        }
        return null;
    }

    // detect if collisions are from the player
    @Override
    public void collision(PhysicsCollisionEvent event) {
        RayCarControl carA = getCarFrom(event.getNodeA());
        RayCarControl carB = getCarFrom(event.getNodeB());

        if (carA == null || carB == null)
            return; //not 2 car collisions
        if (carA.getAI() != null && carB.getAI() != null)
            return; //both are ai
        if (carA.getAI() == null && carB.getAI() == null)
            return; // both are a player !!!

        if (carA.getAI() == null)
            playerCollision(carA, carB, event.getNormalWorldOnB().negate(), event.getLocalPointB(), event.getAppliedImpulse());
        else
            playerCollision(carB, carA, event.getNormalWorldOnB(), event.getLocalPointA(), event.getAppliedImpulse());
    }

    private void playerCollision(RayCarControl player, RayCarControl them, Vector3f normalInWorld, Vector3f themLocalPos, float appliedImpulse) {
        if (!this.hitList.contains(them))
            this.hitList.add(them);

        PhysicsRigidBody prb = them.getPhysicsObject();
        prb.applyImpulse(normalInWorld.mult(FastMath.clamp(appliedImpulse, 3000, 10000) * 5), themLocalPos);

        Vector3f posInWorld = prb.getPhysicsRotation().mult(themLocalPos).add(prb.getPhysicsLocation());
        getState(DebugAppState.class).drawArrow("playerCollision", ColorRGBA.Orange, posInWorld, normalInWorld);

        loseTimer = 0;
    }

    private ProgressBar createBar(float value) {
        ProgressBar pb = new ProgressBar();
        pb.setProgressPercent(value);
        pb.setModel(new DefaultRangedValueModel(0, 1, value));
        pb.getValueIndicator().setBackground(new QuadBackgroundComponent(Colours.getOnGreenToRedScale(value)));
        return pb;
    }
}
