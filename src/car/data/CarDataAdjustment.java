package car.data;

import java.util.function.Consumer;

public class CarDataAdjustment {

    public static CarDataAdjustment asFunc(Consumer<CarDataConst> func) {
        return new CarDataAdjustment(func);
    }

    private final Consumer<CarDataConst> func;

    private CarDataAdjustment(Consumer<CarDataConst> func) {
        this.func = func;
    }

    public void apply(CarDataConst c) {
        func.accept(c);
    }
}
