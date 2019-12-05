package duel;

import com.jme3.asset.AssetManager;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import car.CarStatsUI;
import car.data.CarDataConst;

public class DuelCarStatsUI extends Container {

    public DuelCarStatsUI(AssetManager am, CarDataConst car1, CarDataConst car2) {
        addChild(new CarStatsUI(am, car1), 0);
        addChild(new Label("Vs"), 1);
        addChild(new CarStatsUI(am, car2), 2);
    }
}
