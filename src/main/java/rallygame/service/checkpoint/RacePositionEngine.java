package rallygame.service.checkpoint;

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
import com.jme3.scene.Spatial;

import rallygame.car.ray.RayCarControl;

public class RacePositionEngine {

    protected class CheckpointPosition {
        final int lap;
        final int checkNum;
        public CheckpointPosition(int lap, int checkNum) {
            this.lap = lap;
            this.checkNum = checkNum;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this)
                return true;

            if (!(obj instanceof CheckpointPosition))
                return false;
            
            CheckpointPosition other = (CheckpointPosition)obj;
            return other.lap == this.lap && other.checkNum == this.checkNum;
        }

        @Override
        public int hashCode() {
            int hash = 17;
            hash = hash * 31 + lap;
            hash = hash * 31 + checkNum;
            return hash;
        }
    }

    private final List<Checkpoint> checkpoints;
    private final Map<RayCarControl, RacerState> racers;
    private final Map<CheckpointPosition, Instant> bestTimeAtCheckpoints;

    public RacePositionEngine(Collection<RayCarControl> cars) {
        this.checkpoints = new ArrayList<Checkpoint>();

        this.bestTimeAtCheckpoints = new HashMap<>();
        this.racers = new HashMap<>();
        for (RayCarControl car : cars) {
            this.racers.put(car, new RacerState(car));
        }
    }

    public void addCar(RayCarControl c) {
        var racer = new RacerState(c);
        this.racers.put(c, racer);

        if (this.checkpoints.isEmpty())
            return; //just ignore the error

        Checkpoint first = this.checkpoints.get(0);
        racer.lastCheckpoint = first;
        racer.nextCheckpoint = first;
    }

    public void removeCar(RayCarControl c) {
        this.racers.remove(c);
    }

    public void addCheckpoint(Checkpoint check) {
        this.checkpoints.add(check);
    }

    public void init(Application app) {
        if (this.checkpoints.isEmpty())
            throw new IllegalStateException("Checkpoints is empty?");

        // set progress values
        Checkpoint first = this.checkpoints.get(0);
        for (RacerState racer : this.racers.values()) {
            racer.lastCheckpoint = first;
            racer.nextCheckpoint = first;
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
        
        // get next checkpoint then
        int nextNum = calcNextCheckFrom(checkNum);
        if (nextNum == 0)
            racer.lap++;

        // update to given checkpoints
        racer.lastCheckpoint = this.checkpoints.get(checkNum);
        racer.nextCheckpoint = this.checkpoints.get(nextNum);

        // update last time
        CheckpointPosition pos = new CheckpointPosition(racer.lap, racer.lastCheckpoint.num);
        if (!bestTimeAtCheckpoints.containsKey(pos)) {
            bestTimeAtCheckpoints.put(pos, Instant.now());
            racer.duration = Duration.ZERO;
        } else {
            racer.duration = Duration.between(bestTimeAtCheckpoints.get(pos), Instant.now());
        }
    }
    private int calcNextCheckFrom(int checkNum) {
        return (checkNum + 1) % this.checkpoints.size();
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

    public Vector3f[] getRacerNextCheckpoints(RayCarControl car, int count) {
        Checkpoint[] checkpoints = getNextCheckpoints(car, count);
        Vector3f[] positions = new Vector3f[checkpoints.length];
        for (int i = 0; i < checkpoints.length; i++) {
            if (checkpoints[i] != null)
                positions[i] = checkpoints[i].position;
        }
        return positions;
    }
    public Spatial[] getRacerNextCheckpointsVisual(RayCarControl car, int count) {
        Checkpoint[] checkpoints = getNextCheckpoints(car, count);
        Spatial[] spatials = new Spatial[checkpoints.length];
        for (int i = 0; i < checkpoints.length; i++) {
            if (checkpoints[i] != null)
                spatials[i] = checkpoints[i].visualModel;
        }
        return spatials;
    }
    private Checkpoint[] getNextCheckpoints(RayCarControl car, int count) {
        if (count <= 1) {
            return new Checkpoint[] { this.racers.get(car).nextCheckpoint };
        }
        //reduce to the size of the checkpoints given
        count = Math.min(count, this.getCheckpointCount());

        // calc the next 'count' checkpoints
        int checkNum = this.racers.get(car).nextCheckpoint.num;
        Checkpoint[] checks = new Checkpoint[count];
        checks[0] = this.racers.get(car).nextCheckpoint;
        for (int i = 0; i < count - 1; i++) {
            int check = calcNextCheckFrom(checkNum + i);
            checks[i + 1] = this.checkpoints.get(check);
        }

        return checks;
    }

    public Vector3f getRacerLastCheckpoint(RayCarControl car) {
        return this.racers.get(car).lastCheckpoint.position;
    }

	public RacerState getRacerState(RayCarControl car) {
		return this.racers.get(car);
	}

	public List<RacerState> getAllRaceStates() {
		return new ArrayList<>(this.racers.values().stream().filter(x -> x.lastCheckpoint != null).collect(Collectors.toList()));
	}

	public int getCheckpointCount() {
		return this.checkpoints.size();
	}

	public Vector3f getLastCheckpointPos() {
        if (this.checkpoints.size() < 1)
            return null;
        return getCheckpoint(this.checkpoints.size() - 1).position;
	}

	public Collection<Checkpoint> getNextCheckpoints() {
        return racers.values().stream().map(x -> x.nextCheckpoint).filter(x -> x != null).collect(Collectors.toList());
    }

	public Collection<Checkpoint> getAllPreviousCheckpoints(int num) {
        return checkpoints.stream().filter(x -> x.num < num).collect(Collectors.toList());
	}
}
