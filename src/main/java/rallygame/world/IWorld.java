package rallygame.world;

import java.lang.reflect.InvocationTargetException;

import com.jme3.math.Transform;

import rallygame.service.ILoadable;

public interface IWorld extends ILoadable {
    /** Reset the world */
    void reset();

    /** Get type of the world */
    WorldType getType();

    /** Initial start */
    Transform getStart();

    /** Clone method */
    IWorld copy() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException;
}