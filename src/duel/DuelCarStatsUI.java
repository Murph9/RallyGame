package duel;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import car.CarStatsUI;
import car.data.Car;

public class DuelCarStatsUI extends Container {

    public DuelCarStatsUI(AssetManager am, Car car1, Car car2, Vector3f gravity) {
        addChild(new CarStatsUI(am, car1, gravity), 0);
        addChild(new Label("Vs"), 1);
        addChild(new CarStatsUI(am, car2, gravity), 2);
    }
}
