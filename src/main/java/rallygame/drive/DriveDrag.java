package rallygame.drive;

import com.jme3.app.Application;
import com.jme3.math.Quaternion;
import com.jme3.math.Transform;
import com.jme3.math.Vector3f;

import rallygame.car.CarManager;
import rallygame.car.ai.DriveAlongAI;
import rallygame.car.ai.DriveAtAI;
import rallygame.car.data.Car;
import rallygame.car.ray.RayCarControl;
import rallygame.game.IDriveDone;
import rallygame.helper.H;
import rallygame.world.StaticWorld;
import rallygame.world.StaticWorldBuilder;

public class DriveDrag extends DriveBase {

    // just like the drag mode in Need For Speed Underground

    private SprintMenu sprint;
    private float started;
    private boolean ended;

    public DriveDrag(IDriveDone done) {
        super(done, Car.Rocket, new StaticWorldBuilder(StaticWorld.multidragstrip));

        this.started = -3.5f;
    }

    @Override
    public void initialize(Application app) {
        super.initialize(app);

        var cm = getState(CarManager.class);
        Car[] types = Car.values();
        for (int i = 0; i < types.length; i++) {
            RayCarControl c = cm.addCar(types[i], world.getStart(), false);
            c.attachAI(new DriveAtAI(c, cm.getPlayer().getPhysicsObject()), true);
        }

        setSpawns(cm);

        getStateManager().detach(menu);
        sprint = new SprintMenu(this);
        getStateManager().attach(sprint);
    }

    private void setSpawns(CarManager cm) {
        int count = cm.getAll().size();
        Transform start = world.getStart();
        for (int i = 0; i < count; i++) {

            final int index = i * 5;
            RayCarControl car = cm.get(i);
            if (i != 0) {
                car.attachAI(new DriveAlongAI(car, (vec) -> {
                    return new Vector3f(index, 0, vec.z + 20); // next pos math
                }), true);
            }

            car.setPhysicsProperties(new Vector3f(index, start.getTranslation().y, 0), null, (Quaternion) null, null);
        }
    }
    
    private void keepStill(CarManager cm) {
        int count = cm.getAll().size();
        Transform start = world.getStart();
        for (int i = 0; i < count; i++) {
            final int index = i*5;
            RayCarControl car = cm.get(i);
            Vector3f pos = car.location;
            car.setPhysicsProperties(new Vector3f(index, start.getTranslation().y, pos.z), new Vector3f(), (Quaternion) null, new Vector3f());
        }
    }

    private int detectWinner(CarManager cm) {
        int count = cm.getAll().size();
        boolean aWinner = false;
        for (int i = 0; i < count; i++) {
            RayCarControl car = cm.get(i);
            Vector3f pos = car.location;
            if (pos.z > 1000)
                aWinner = true;
        }

        if (aWinner) {
            float max = 0;
            int maxI = -1;

            for (int i = 0; i < count; i++) {
                RayCarControl car = cm.get(i);
                Vector3f pos = car.location;
                if (pos.z > max) {
                    max = pos.z;
                    maxI = i;
                }
            }

            return maxI;
        }

        return -1;
    }

	public void update(float tpf) {
        super.update(tpf);

        var cm = getState(CarManager.class);

        if (!ended) {
            ended = detectWinner(cm) != -1;
        }
        if (ended) {
            sprint.setText("Winner: " + cm.get(detectWinner(cm)).getCarData().carModel);
            cm.setEnabled(false);
            return;
        }

        
        if (started < 0) {
            keepStill(cm);

            started += tpf;
            sprint.setText("Starting in " + H.roundDecimal(Math.abs(started), 2));
            return;
        }
        sprint.setText("");
    }
    
    @Override
    public void cleanup(Application app) {
        getStateManager().detach(sprint);
        super.cleanup(app);
    }
}