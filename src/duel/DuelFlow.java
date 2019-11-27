package duel;

import java.util.InputMismatchException;

import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;

import car.data.Car;
import helper.H;
import helper.Log;

interface IDuelFlow {
    void nextState(AppState state, DuelResultData result);
    DuelData getData();
}

public class DuelFlow implements IDuelFlow {
    
    private final Application app;
    private DuelData data; //so i can keep track of the cars and stuff
    private AppState curState;

    public DuelFlow(Application app) {
        this.app = app;
        this.data = new DuelData();
        this.data.yourCar = Car.Runner;
        this.data.theirCar = Car.Rally;
        
        nextState(null, null);
    }

    public DuelData getData() {
        return data;
    }

    public void nextState(AppState state, DuelResultData result) {
        if (this.data == null)
            throw new IllegalStateException();

        AppStateManager sm = app.getStateManager();

        if (state == null) {
            curState = new DuelMainMenu(this, null);
            sm.attach(curState);
            return;
        }
        if (state != curState) {
            throw new InputMismatchException(
                    "Recieved state '" + curState.getClass() + "' but was expecting  '" + state.getClass() + "'");
        }
        if (data == null) {
            throw new IllegalArgumentException("Recived no data from the previous state: "+state.getClass());
        }

        sm.detach(state);
        Log.p("State", state.getClass(), "returned");
        
        if (result.quitGame) {
            app.stop(); //then just quit the game
            return;
        }

        if (state instanceof DuelMainMenu) {
            curState = new DuelRaceStart(this);
        } else if (state instanceof DuelRaceStart) {
            curState = new DuelRace(this);
        } else if (state instanceof DuelRace) {
            if (result.raceResult != null && result.raceResult.playerWon) {
                this.data.wins++;
                this.data.yourCar = this.data.theirCar; //basically just stolen

                // TODO actually do something with this to make progression
                this.data.theirCar = H.randFromArray(Car.values());
                curState = new DuelRaceStart(this);
            } else {
                curState = new DuelMainMenu(this, this.data);
                
                this.data = new DuelData();
                this.data.yourCar = Car.Runner;
                this.data.theirCar = Car.Rally;
            }
        } else {
            throw new IllegalArgumentException();
        }
        
        sm.attach(curState);
    }

	public void cleanup() {
        app.getStateManager().detach(curState);
	}
}