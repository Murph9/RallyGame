package car.data;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/** Allows you to adjust car data using CarDataAdjustment objects */
public class CarDataAdjuster {
    
    private final List<CarDataAdjustment> adjustments;

    public CarDataAdjuster() {
        this(new LinkedList<>());
    }
    public CarDataAdjuster(CarDataAdjustment ...adjustments) {
        this.adjustments = Arrays.asList(adjustments);
    }
    public CarDataAdjuster(List<CarDataAdjustment> adjustments) {
        this.adjustments = adjustments;
    }

    public void addAdjustment(CarDataAdjustment adjustment) {
        adjustments.add(adjustment);
    }

    public void applyAll(CarDataConst data) {
        for (CarDataAdjustment adj: adjustments) {
            adj.apply(data);
        }
    }
}
