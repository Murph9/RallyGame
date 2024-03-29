package rallygame.car.data;

import java.util.Arrays;
import java.util.List;

/** Allows you to adjust car data using CarDataAdjustment objects */
public class CarDataAdjuster {
    
    private final List<CarDataAdjustment> adjustments;

    public CarDataAdjuster(CarDataAdjustment ...adjustments) {
        this.adjustments = Arrays.asList(adjustments);
    }
    public CarDataAdjuster(List<CarDataAdjustment> adjustments) {
        this.adjustments = adjustments;
    }

    public void applyAll(CarDataConst data) {
        for (CarDataAdjustment adj: adjustments) {
            adj.apply(data);
        }
    }
}
