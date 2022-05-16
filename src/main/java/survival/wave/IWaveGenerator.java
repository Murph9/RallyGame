package survival.wave;

import java.util.List;

import com.jme3.app.Application;
import com.jme3.scene.Geometry;

import rallygame.car.ray.RayCarControl;

interface IWaveGenerator {
    List<Geometry> generate(Application app, RayCarControl target);
}