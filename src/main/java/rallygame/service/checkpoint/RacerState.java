package rallygame.service.checkpoint;

import java.time.Duration;

import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;

import rallygame.car.ray.RayCarControl;

class RacerState implements Comparable<RacerState> {
    public final RayCarControl car;

    public int lap;
    public Checkpoint lastCheckpoint;
    public Checkpoint nextCheckpoint;
    public Duration duration;

    public Geometry arrow;

    public RacerState(RayCarControl car) {
        this.car = car;
    }

    @Override
    public int compareTo(RacerState o) {
        if (this.lap != o.lap)
            return o.lap - this.lap;
        if (this.nextCheckpoint == null || o.nextCheckpoint == null)
            return 0;
        if (this.nextCheckpoint.num != o.nextCheckpoint.num)
            return o.nextCheckpoint.num - this.nextCheckpoint.num;

        // note this is backwards because closer is better
        return (int) ((this.distanceToNextCheckpoint() - o.distanceToNextCheckpoint()) * 1000);
    }

    private float distanceToNextCheckpoint() {
        if (car == null)
            return Float.MAX_VALUE;

        Vector3f pos = car.location;
        return pos.subtract(nextCheckpoint.position).length();
    }

    public String getName() {
        return car.getCarData().name;
    }

    public float calcLapProgress(int checkpointCount) {
        return nextCheckpoint.num / ((float) checkpointCount);
    }
}
