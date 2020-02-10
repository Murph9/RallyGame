package service.checkpoint;

import com.jme3.bullet.control.GhostControl;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;

class Checkpoint {
    public final int num;
    public final Vector3f position;
    public final GhostControl ghost;
    public final Spatial visualModel;

    Checkpoint(int num, Vector3f pos, GhostControl ghost, Spatial visualModel) {
        this.num = num;
        this.position = pos;
        this.ghost = ghost;
        this.visualModel = visualModel;
    }
}