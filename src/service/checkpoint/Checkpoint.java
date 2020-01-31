package service.checkpoint;

import com.jme3.bullet.control.GhostControl;
import com.jme3.math.Vector3f;

class Checkpoint {
    public final int num;
    public final Vector3f position;
    public final GhostControl ghost;

    Checkpoint(int num, Vector3f pos, GhostControl ghost) {
        this.num = num;
        this.position = pos;
        this.ghost = ghost;
    }
}