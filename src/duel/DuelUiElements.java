package duel;

import com.jme3.asset.AssetManager;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Command;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import car.CarStatsUI;
import car.data.CarDataConst;

public class DuelUiElements {

    public static Container DuelCarStats(AssetManager am, CarDataConst car1, CarDataConst car2) {
        Container c = new Container();
        c.addChild(new CarStatsUI(am, car1), 0);
        c.addChild(new Label("Vs"), 1);
        c.addChild(new CarStatsUI(am, car2), 2);
        return c;
    }

    @SuppressWarnings("unchecked") //button vargs
    public static Container pauseMenu(Runnable unPause, Runnable quit) {
        if (unPause == null || quit == null)
            return null;

        Container c = new Container();
        Button button = c.addChild(new Button("Resume"));
        button.addClickCommands(new Command<Button>() {
            @Override
            public void execute(Button source) {
                unPause.run();
            }
        });

        Button button2 = c.addChild(new Button("Quit"));
        button2.addClickCommands(new Command<Button>() {
            @Override
            public void execute(Button source) {
                quit.run();
            }
        });

        return c;
    }
}
