package service.averager;

import com.jme3.math.Vector3f;

public class AverageV3f extends AveragerBase<Vector3f> {

    public AverageV3f(int size, IAverager.Type type) {
        super(size, type);
    }

    @Override
    protected Vector3f add(Vector3f value1, Vector3f value2) {
        return value1.add(value2);
    }

    @Override
    protected Vector3f createBlank() {
        return new Vector3f();
    }

    @Override
    protected Vector3f mult(Vector3f value, float mult) {
        return value.mult(mult);
    }
}