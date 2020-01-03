package world;

import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;

public interface IWorldPath {
    /** Initial start position */
    Vector3f getStartPos();
    
    /** For initial start rotation */
    Matrix3f getStartRot();
    
    /** Path of the map, for AI routing */
    Vector3f[] getPath();
}