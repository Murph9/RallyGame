package world;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public interface IWorld {
    /** Reset the world */
    void reset();

    /** Get type of the world */
    WorldType getType();

    /** Initial start position */
    Vector3f getStartPos();

    /** For initial start rotation */
    Quaternion getStartRot();
}