package drive;

import world.StaticWorld;
import world.StaticWorldBuilder;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.PhysicsCollisionEvent;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

import car.ai.DriveAtAI;
import car.data.Car;
import car.ray.RayCarControl;
import game.DebugAppState;
import game.IDriveDone;
import helper.Geo;
import helper.H;

public class DriveCrash extends DriveBase implements PhysicsCollisionListener {

    private static String POLICE_TEXT = "You are in trouble from the physics police\nthey are trying to nab you for breaking physics laws\n specifcally for being rediculously bouncy.\n";
    private static Vector3f[] spawns = new Vector3f[] { new Vector3f(0, 0, 0), new Vector3f(0, 10, 0),
            new Vector3f(5, 5, 0) };

    private final Car them;
    private final int themCount;

    private int totalKilled;
    private int frameCount = 0; // global frame timer

    private final List<RayCarControl> hitList;

    public DriveCrash(IDriveDone done) {
        super(done, Car.Runner, new StaticWorldBuilder(StaticWorld.duct2));
        this.them = Car.Rally;
        this.themCount = 40;
        this.totalKilled = 0;

        this.hitList = new LinkedList<>();
    }

    @Override
    public void initialize(Application app) {
        super.initialize(app);
        getState(BulletAppState.class).getPhysicsSpace().addCollisionListener(this);
    }

    @Override
    public void cleanup(Application app) {
        getState(BulletAppState.class).getPhysicsSpace().removeCollisionListener(this);
        super.cleanup(app);
    }

    public void update(float tpf) {
        super.update(tpf);
        frameCount++;

        if (this.cb.getCount() < (themCount+1) && frameCount % 60 == 0) {
            Vector3f spawn = H.randFromArray(spawns);
            RayCarControl c = this.cb.addCar(them, spawn, world.getStartRot(), false);
            c.attachAI(new DriveAtAI(c, this.cb.get(0).getPhysicsObject()), true);
        }

        // check if any hit ones are upside down, if so kill them
        List<RayCarControl> toKill = new ArrayList<RayCarControl>();
        for (RayCarControl c : this.hitList)
            if (c.up != null && c.up.y < 0 && c != this.cb.get(0))
                toKill.add(c);
        for (RayCarControl c : this.hitList)
            if (c.location.y < -100)
                toKill.add(c);
        for (RayCarControl c : toKill) {
            totalKilled++;
            cb.removeCar(c);
            hitList.remove(c);
        }

        if (this.menu.randomthing != null) {
            this.menu.randomthing.setText(POLICE_TEXT + "Total Killed: " + totalKilled);
        }
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
        prb.applyImpulse(normalInWorld.mult(Math.max(10000, appliedImpulse*5)), themLocalPos);

        Vector3f posInWorld = prb.getPhysicsRotation().mult(themLocalPos).add(prb.getPhysicsLocation());
        getState(DebugAppState.class).drawArrow("playerCollision", ColorRGBA.Orange, posInWorld, normalInWorld);
    }
}
