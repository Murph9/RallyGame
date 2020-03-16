package rallygame.drive;

import com.jme3.app.Application;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

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

    private int themCount;

    private SprintMenu sprint;
    private float started;
    private boolean ended;

    public DriveDrag(IDriveDone done) {
        super(done, Car.Runner, new StaticWorldBuilder(StaticWorld.multidragstrip));

        this.themCount = Car.values().length;
        this.started = -4;
    }

    @Override
    public void initialize(Application app) {
        super.initialize(app);

        Car[] types = Car.values();
        for (int i = 0; i < this.themCount; i++) {
            RayCarControl c = this.cb.addCar(types[i], world.getStartPos(), world.getStartRot(), false);
            c.attachAI(new DriveAtAI(c, this.cb.get(0).getPhysicsObject()), true);
        }

        setSpawns();

        getStateManager().detach(menu);
        sprint = new SprintMenu(this);
        getStateManager().attach(sprint);
    }

    private void setSpawns() {
        int count = this.cb.getAll().size();
        for (int i = 0; i < count; i++) {

            final int index = i * 5;
            RayCarControl car = this.cb.get(i);
            if (i != 0) {
                car.attachAI(new DriveAlongAI(car, (vec) -> {
                    return new Vector3f(index, 0, vec.z + 20); // next pos math
                }), true);
            }

            car.setPhysicsProperties(new Vector3f(index, world.getStartPos().y, 0), null, (Quaternion) null, null);
        }
    }
    
    private void keepStill() {
        int count = this.cb.getAll().size();
        for (int i = 0; i < count; i++) {
            final int index = i*5;
            RayCarControl car = this.cb.get(i);
            Vector3f pos = car.location;
            car.setPhysicsProperties(new Vector3f(index, world.getStartPos().y, pos.z), new Vector3f(), (Quaternion) null, new Vector3f());
        }
    }

    private int detectWinner() {
        int count = this.cb.getAll().size();
        boolean aWinner = false;
        for (int i = 0; i < count; i++) {
            RayCarControl car = this.cb.get(i);
            Vector3f pos = car.location;
            if (pos.z > 1000)
                aWinner = true;
        }

        if (aWinner) {
            float max = 0;
            int maxI = -1;

            for (int i = 0; i < count; i++) {
                RayCarControl car = this.cb.get(i);
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

        if (!ended) {
            ended = detectWinner() != -1;
        }
        if (ended) {
            sprint.setText("Winner: " + this.cb.get(detectWinner()).getCarData().carModel);
            keepStill();
            return;
        }

        
        if (started < 0) {
            keepStill();

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