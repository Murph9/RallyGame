package survival.upgrade;

import java.util.function.Consumer;

import rallygame.car.data.CarDataConst;

public class CarDataConstUpgrade extends Upgrade<CarDataConst> {
    public CarDataConstUpgrade(boolean positive, String label, Consumer<CarDataConst> func) {
        super(positive, label, func);
    }
}
