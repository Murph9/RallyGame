package drive;

import world.wp.DefaultBuilder;
import world.wp.WP.DynamicType;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;

import car.*;
import car.ai.FollowWorldAI;
import car.data.Car;
import car.ray.RayCarControl;
import game.App;

public class DriveDemo extends DriveBase {

	public DriveDemo (Car car) {
    	super(car, DynamicType.Valley.getBuilder());
    }
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
    	super.initialize(stateManager, app);
    	
    	//remove all stuff we want and player from everything
    	
    	this.cb.removeCar(0);
    	this.cb.addCar(0, car, world.getStartPos(), world.getStartRot(), true, null); //even though they aren't a player

    	RayCarControl car = this.cb.get(0);
    	car.attachAI(new FollowWorldAI(car, (DefaultBuilder)world));
    	
    	app.getStateManager().detach(uiNode);
    	uiNode = new CarUI(cb.get(0));
		app.getStateManager().attach(uiNode);
		
		App.CUR.getStateManager().detach(camera);
		app.getInputManager().removeRawInputListener(camera);
		camera = new CarCamera("Camera", App.CUR.getCamera(), cb.get(0));
		App.CUR.getStateManager().attach(camera);
		app.getInputManager().addRawInputListener(camera);
		
//		App.rally.getRootNode().detachChild(minimap.rootNode); //TODO causes crash
//		minimap = new MiniMap(cb.get(0));
	}
	
	public void update(float tpf) {
		super.update(tpf);
	}
}
