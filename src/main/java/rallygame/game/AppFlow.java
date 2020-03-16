package rallygame.game;

import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;

import rallygame.car.data.Car;
import rallygame.drive.*;
import rallygame.helper.Log;
import rallygame.world.StaticWorld;
import rallygame.world.StaticWorldBuilder;
import rallygame.world.wp.DefaultBuilder;
import rallygame.world.wp.WP.DynamicType;
import rallygame.world.IWorld;

public class AppFlow implements IFlow, IDriveDone, IChooseStuff {

    private final Application app;
	private Car car;
	private IWorld world;

    public enum StartType {
        Fast,
		Start,
		Crash,
		Getaway,
		Race,
		RaceDyn,
		Drag,
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
            case Fast:
                sm.attach(new DriveBase((IDriveDone)this, Car.Runner, new StaticWorldBuilder(StaticWorld.duct)));
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
                StaticWorldBuilder world = new StaticWorldBuilder(StaticWorld.duct2);
                sm.attach(world);
				sm.attach(new DriveRace(world, (IDriveDone) this));
				break;
			case RaceDyn:
				DefaultBuilder dynWorld = DynamicType.Valley.getBuilder();
				sm.attach(dynWorld);
				sm.attach(new DriveDynamicRace(dynWorld, (IDriveDone) this));
				break;
			case Drag:
				sm.attach(new DriveDrag((IDriveDone)this));
				break;
			default:
				Log.p("No idea which world type that was: " + type.name());
        }
    }

    public void done(AppState state) {
        app.getStateManager().detach(state);
        init();
	}
	

    // To overhaul how this works use a AppFlowData class which is the current state
    // each app gets a view onto it, allowing certain things like selecting a map
	@Override
	public void chooseCar(Car car) {
		this.car = car;

		AppStateManager sm = app.getStateManager();
        sm.detach(sm.getState(ChooseCar.class));
		
		sm.attach(new ChooseMap((IChooseStuff)this));
	}
	@Override
	public void chooseMap(IWorld world) {
		this.world = world;
		
		AppStateManager sm = app.getStateManager();
        sm.detach(sm.getState(ChooseMap.class));

		sm.attach(new DriveBase((IDriveDone)this, this.car, this.world));
	}

	public void cleanup() {
		
	}
}