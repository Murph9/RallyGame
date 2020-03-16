package rallygame.service.averager;

public class AverageFloat extends AveragerBase<Float> {

    public AverageFloat(int size, IAverager.Type type) {
        super(size, type);
    }

    @Override
    protected Float add(Float value1, Float value2) {
        return value1 + value2;
    }

    @Override
    protected Float createBlank() {
        return 0f;
    }

    @Override
    protected Float mult(Float value, float mult) {
        return value*mult;
    }
}
