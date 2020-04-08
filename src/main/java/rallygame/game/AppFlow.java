package rallygame.game;

import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.state.AppState;
import com.jme3.app.state.AppStateManager;
import com.jme3.math.FastMath;

import rallygame.car.data.Car;
import rallygame.drive.*;
import rallygame.helper.Log;
import rallygame.service.ILoadable;
import rallygame.service.LoadingState;
import rallygame.world.*;
import rallygame.world.wp.DefaultBuilder;
import rallygame.world.wp.WP.DynamicType;
import rallygame.world.path.PathWorld;

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
		
		List<ILoadable> loadingStates = new LinkedList<>();
		List<AppState> loadme = new LinkedList<>();
        switch(type) {
            case Start:
                loadme.add(new ChooseCar((IChooseStuff)this));
                break;
			case Fast:
				World w = new PathWorld(FastMath.nextRandomInt() % 10);
				loadingStates.add(w);
                loadme.add(new DriveBase((IDriveDone)this, Car.Runner, w));
                break;
			case Getaway:
				loadme.add(new DriveMainRoadGetaway((IDriveDone)this));
				break;
			case Dev:
				loadme.add(new DriveDev((IDriveDone)this));
				break;
			case Crash:
				loadme.add(new DriveCrash((IDriveDone)this));
				break;
            case Race:
                StaticWorldBuilder world = new StaticWorldBuilder(StaticWorld.duct2);
                loadingStates.add(world);
				loadme.add(new DriveRace(world, (IDriveDone) this));
				break;
			case RaceDyn:
				DefaultBuilder dynWorld = DynamicType.Valley.getBuilder();
				loadingStates.add(dynWorld);
				loadme.add(new DriveDynamicRace(dynWorld, (IDriveDone) this));
				break;
			case Drag:
				loadme.add(new DriveDrag((IDriveDone)this));
				break;
			default:
				Log.p("No idea which world type that was: " + type.name());
		}
		
		if (!loadingStates.isEmpty()) {
			//load them first
			LoadingState loading = new LoadingState(loadingStates.toArray(new ILoadable[loadingStates.size()]), null);
			loading.setCallback((states) -> {
				loadIt(sm, loadme);
			});
			sm.attach(loading);
		} else {
			loadIt(sm, loadme);
		}
	}
	private void loadIt(AppStateManager sm, List<AppState> states) {
		for (AppState s: states) {
			sm.attach(s);
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
