package drive;

import java.util.Collection;

import com.jme3.app.state.AppState;
import com.jme3.math.Transform;

import car.ray.RayCarControl;

public interface IDrive extends AppState {

    void next();
    
    void resetWorld();
    
    Collection<RayCarControl> getAllCars();
    
    Transform resetPosition(RayCarControl car);
}