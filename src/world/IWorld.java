package world;

import java.lang.reflect.InvocationTargetException;

import com.jme3.app.state.AppState;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;

public interface IWorld extends AppState {
    /** Reset the world */
    void reset();

    /** Get type of the world */
    WorldType getType();

    /** Initial start position */
    Vector3f getStartPos();

    /** For initial start rotation */
    Quaternion getStartRot();

    /** Clone method */
    IWorld copy() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException;
    
    /** Simple AI directional call, please don't use much */
    Vector3f getNextPieceClosestTo(Vector3f pos);
}