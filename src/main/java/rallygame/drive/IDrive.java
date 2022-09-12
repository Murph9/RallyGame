package rallygame.drive;

import com.jme3.app.state.AppState;
import com.jme3.math.Transform;

import rallygame.car.ray.RayCarControl;

public interface IDrive extends AppState {

    void next();
    void resetWorld();
    Transform resetPosition(RayCarControl car);
}
