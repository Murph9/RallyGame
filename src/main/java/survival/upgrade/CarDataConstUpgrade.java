package survival.upgrade;

import java.util.List;
import java.util.function.Consumer;

import rallygame.car.data.CarDataConst;

public class CarDataConstUpgrade extends Upgrade<CarDataConst> {
    public CarDataConstUpgrade(boolean positive, String label, Consumer<CarDataConst> func) {
        super(positive, label, func);
    }

    @Override
    public boolean applies(List<Upgrade<?>> existing) {
        return true; // no restrictions that i know of
    }

    // public static Upgrade<CarDataConst> MuchPOWER = new CarDataConstUpgrade(true, "MUCH POWER (5% nitro increase) [ctrl]", x -> x.nitro_force *= 1.05f);
    /*public static Upgrade<CarDataConst> ImproveGrip = new CarDataConstUpgrade(true, "Improve Grip (3%)", (data) -> {
        for (int i = 0; i < 4; i++) {
            data.wheelData[i].traction.pjk_lat.D *= 1.03f;
            data.wheelData[i].traction.pjk_long.D *= 1.03f;
        }
    });*/
    public static Upgrade<CarDataConst> ImproveEngine = new CarDataConstUpgrade(true, "Improve Engine (4%)", (data) -> {
        for (int i = 0; i < data.e_torque.length; i++) {
            data.e_torque[i] *= 1.04f;
        }
    });
    // public static Upgrade<CarDataConst> ReduceFuelUse = new CarDataConstUpgrade(true, "Reduce Full use by (10%)", x -> x.fuelRpmRate *= 0.9f);
}
