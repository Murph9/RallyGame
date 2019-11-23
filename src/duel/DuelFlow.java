package duel;

import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;

import helper.Log;

interface IDuelFlow {
    void nextState(AppState state, boolean something);
    DuelData getData();
}

public class DuelFlow implements IDuelFlow {
    
    private final Application app;
    private DuelData data; //so i can keep track of the cars and stuff
    public DuelFlow(Application app) {
        this.app = app;
        this.data = new DuelData();
        
        //init
        nextState(null, false);
    }

    public DuelData getData() {
        return data;
    }

    public void nextState(AppState state, boolean something) {
        AppStateManager sm = app.getStateManager();

        if (state == null) {
            DuelMainMenu dmm = new DuelMainMenu(this);
            sm.attach(dmm);
            return;
        }
        sm.detach(state);

        Log.p("State", state.getClass(), "closed");
        
        if (state instanceof DuelMainMenu) {
            sm.attach(new DuelRaceStart(this));
        } else if (state instanceof DuelRaceStart) {
            sm.attach(new DuelRace(this));
        } else if (state instanceof DuelRace) {
            if (something) {
                sm.attach(new DuelRaceEnd(this));
            } else {
                sm.attach(new DuelMainMenu(this));
                //TODO but with data from the previous run
            }
        } else if (state instanceof DuelRaceEnd) {
            sm.attach(new DuelRaceStart(this));
        } else {
            throw new IllegalArgumentException();
        }
    }
}