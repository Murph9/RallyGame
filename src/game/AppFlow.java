package game;

import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;

import car.data.Car;
import drive.*;
import world.StaticWorld;
import world.StaticWorldBuilder;
import world.World;

public class AppFlow implements IFlow, IDriveDone, IChooseStuff {

    private final Application app;
	private Car car;
	private World world;

    public enum StartType {
        Fast,
		Start,
		AI,
		Crash,
		Getaway,
		Race,
		Dev;
    }

    public AppFlow(Application app) {
        this.app = app;
        init();
    }
    private void init() {
		Start start = new Start((IFlow)this);
		app.getStateManager().attach(start);
    }

    public void startCallback(StartType type) {
		AppStateManager sm = app.getStateManager();
        sm.detach(sm.getState(Start.class));

        switch(type) {
            case Start:
                sm.attach(new ChooseCar((IChooseStuff)this));
                break;
            case AI:
                sm.attach(new DriveAI((IDriveDone)this));
                break;
            case Fast:
                sm.attach(new DriveBase((IDriveDone)this, Car.Runner, new StaticWorldBuilder(StaticWorld.track2)));
                break;
			case Getaway:
				sm.attach(new DriveMainRoadGetaway((IDriveDone)this));
				break;
			case Dev:
				sm.attach(new DriveDev((IDriveDone)this));
				break;
			case Crash:
				sm.attach(new DriveCrash((IDriveDone)this));
				break;
			case Race:
				sm.attach(new DriveRace());
                break;
        }
    }

    public void done(AppState state) {
        app.getStateManager().detach(state);
        init();
	}
	

	//TODO overhaul how this works with passing through values
	@Override
	public void chooseCar(Car car) {
		this.car = car;

		AppStateManager sm = app.getStateManager();
        sm.detach(sm.getState(ChooseCar.class));
		
		sm.attach(new ChooseMap((IChooseStuff)this));
	}
	@Override
	public void chooseMap(World world) {
		this.world = world;
		
		AppStateManager sm = app.getStateManager();
        sm.detach(sm.getState(ChooseMap.class));

		sm.attach(new DriveBase((IDriveDone)this, this.car, this.world));
	}
}