package duel;

import java.util.InputMismatchException;

import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.ColorRGBA;

import car.data.Car;
import car.data.CarDataAdjuster;
import car.data.CarDataAdjustment;

interface IDuelFlow {
    void nextState(AppState state, DuelResultData result);
    DuelData getData();
}

public class DuelFlow implements IDuelFlow {
    
    private final Application app;
    private final String version;
    private DuelData data;
    private AppState curState;

    public DuelFlow(Application app, String version) {
        this.app = app;
        this.version = version;
        this.data = getStartDataState();
        
        nextState(null, null);
    }

    public DuelData getData() {
        return data;
    }

    public void nextState(AppState state, DuelResultData result) {
        if (this.data == null)
            throw new IllegalArgumentException("Recived no data from the previous state: " + state.getClass());

        AppStateManager sm = app.getStateManager();

        if (state == null) {
            curState = new DuelMainMenu(this, null, version);
            sm.attach(curState);
            return;
        }
        
        if (state != curState) {
            throw new InputMismatchException("Recieved state '" + curState.getClass() + "' but was expecting  '" + state.getClass() + "'");
        }

        sm.detach(state);
        
        if (result.quitGame) {
            app.stop(); //then just quit the game
            return;
        }

        if (state instanceof DuelMainMenu) {
            curState = new DuelRace(this);
        } else if (state instanceof DuelRace) {
            if (result.raceResult != null && result.raceResult.playerWon) {
                this.data.wins++;
                this.data.yourCar = this.data.theirCar; //basically just stolen
                this.data.yourAdjuster = this.data.theirAdjuster;

                Racer rival = generateNextRival(this.data.wins);
                this.data.theirCar = rival.car;
                this.data.theirAdjuster = rival.adj;
                curState = new DuelRace(this);
            } else {
                curState = new DuelMainMenu(this, this.data, version);
                this.data = getStartDataState();
            }
        } else {
            throw new IllegalArgumentException();
        }
        
        sm.attach(curState);
    }

	public void cleanup() {
        app.getStateManager().detach(curState);
    }
    
    private DuelData getStartDataState() {
        DuelData data = new DuelData();
        data.yourCar = Car.Runner;
        Racer rival = generateNextRival(data.wins);
        data.theirCar = rival.car;
        data.theirAdjuster = rival.adj;
        return data;
    }
    
    private Racer generateNextRival(int wins) {
        // random calls need to stay the same if this is called again
        ColorRGBA col = ColorRGBA.randomColor();
        // Car c = helper.H.randFromArray(Car.values());
        Car c = Car.Runner;
        Racer r = new Racer(c,
            new CarDataAdjuster(CarDataAdjustment.asFunc((data) -> {
                data.baseColor = col;
                for (int i = 0; i < data.e_torque.length; i++) {
                    data.e_torque[i] *= Math.pow(1.1, wins);
                }
            }))
        );
        return r;
    }

    class Racer {
        final Car car;
        final CarDataAdjuster adj;
        public Racer(Car car, CarDataAdjuster adj) {
            this.car = car;
            this.adj = adj;
        }
    }
}
