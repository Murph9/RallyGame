package rallygame.service.checkpoint;

import com.jme3.bullet.control.GhostControl;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

public class Checkpoint {
    public final int num;
    public final Vector3f position;
    public final GhostControl ghost;
    public final Spatial visualModel;

    Checkpoint(int num, Vector3f pos, GhostControl ghost, Spatial visualModel) {
        this.num = num;
        if (pos == null)
            throw new IllegalArgumentException("Position must be set");
        this.position = pos;
        this.ghost = ghost;
        if (ghost == null)
            throw new IllegalArgumentException("Ghost control must be set");
        this.visualModel = visualModel;
    }
    
    public boolean equals(Object o) {
        if (!(o instanceof Checkpoint))
            return false;
        var other = (Checkpoint)o;

        return this.num == other.num && other.position.equals(this.position);
    }

    @Override
    public int hashCode() {
        return num*position.hashCode();
    }
}