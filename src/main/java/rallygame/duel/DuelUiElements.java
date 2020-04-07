package rallygame.duel;

import com.jme3.asset.AssetManager;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import rallygame.car.data.CarDataConst;
import rallygame.car.ui.CarStatsUI;

public class DuelUiElements {

    public static Container DuelCarStats(AssetManager am, CarDataConst car1, CarDataConst car2) {
        Container c = new Container();
        c.addChild(new CarStatsUI(am, car1), 0);
        c.addChild(new Label("Vs"), 1);
        c.addChild(new CarStatsUI(am, car2), 2);
        return c;
    }
}
