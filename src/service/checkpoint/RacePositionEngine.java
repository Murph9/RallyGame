package service.checkpoint;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.jme3.app.Application;
import com.jme3.bullet.control.GhostControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.math.Vector3f;

import car.ray.RayCarControl;

public class RacePositionEngine {

    private final List<Checkpoint> checkpoints;
    private final Map<RayCarControl, RacerState> racers;
    private final Map<Integer, Instant> timeAtCheckpoints;

    public RacePositionEngine(Collection<RayCarControl> cars) {
        this.checkpoints = new ArrayList<Checkpoint>();

        this.timeAtCheckpoints = new HashMap<>();
        this.racers = new HashMap<>();
        for (RayCarControl car : cars) {
            this.racers.put(car, new RacerState(car));
        }
    }

    public void addCheckpoint(Checkpoint check) {
        this.checkpoints.add(check);
    }

    public void init(Application app) {
        if (this.checkpoints.isEmpty())
            throw new IllegalStateException("Checkpoints is empty?");

        // set progress values
        for (RacerState racer : this.racers.values()) {
            racer.lastCheckpoint = this.checkpoints.get(0);
            racer.nextCheckpoint = this.checkpoints.get(0);
        }
    }

    public RacerState getIfRayCar(RigidBodyControl body) {
        for (Entry<RayCarControl, RacerState> racer : racers.entrySet())
            if (body == racer.getKey().getPhysicsObject())
                return racer.getValue();
        return null;
    }

    public Checkpoint getIfCheckpoint(GhostControl ghost) {
        for (Checkpoint checkpoint : this.checkpoints)
            if (checkpoint.ghost == ghost)
                return checkpoint;
        return null;
    }

    public void racerHitCheckpoint(RacerState racer, Checkpoint check) {
        int checkNum = check.num;
        
        // calc next checkpoint then
        int nextNum = (checkNum + 1 % this.checkpoints.size());
        if (nextNum == 0)
            racer.lap++;

        // update to given checkpoints
        racer.lastCheckpoint = this.checkpoints.get(checkNum);
        racer.nextCheckpoint = this.checkpoints.get(nextNum);

        // update last time
        int fakeCheckpointHash = racer.lap * 10000 + racer.lastCheckpoint.num;
        if (!timeAtCheckpoints.containsKey(fakeCheckpointHash)) {
            timeAtCheckpoints.put(fakeCheckpointHash, Instant.now());
            racer.duration = Duration.ZERO;
        } else {
            racer.duration = Duration.between(timeAtCheckpoints.get(fakeCheckpointHash), Instant.now());
        }
    }

    public Checkpoint getCheckpointFromPos(Vector3f pos) {
        for (Checkpoint c : this.checkpoints)
            if (c.position.equals(pos))
                return c;
        return null;
    }

    public Checkpoint getCheckpoint(int i) {
        if (i < 0 || i >= this.checkpoints.size())
            return null;

        return this.checkpoints.get(i);
    }

	public RacerState getRacerState(RayCarControl car) {
		return this.racers.get(car);
	}

	public List<RacerState> getAllRaceStates() {
		return new ArrayList<>(this.racers.values());
	}

	public int getCheckpointCount() {
		return this.checkpoints.size();
	}

	public Vector3f getLastCheckpointPos() {
        if (this.checkpoints.size() < 1)
            return null;
        return posOfCheckpoint(getCheckpoint(this.checkpoints.size() - 1));
	}

	public List<Checkpoint> getNextCheckpoints() {
        return racers.values().stream().map(x -> x.nextCheckpoint).collect(Collectors.toList());
    }
    

    private Vector3f posOfCheckpoint(Checkpoint check) {
        return check == null ? null : check.position;
    }
}