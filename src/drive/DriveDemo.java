package drive;

import world.World;
import world.wp.DefaultBuilder;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;

import car.*;
import car.ai.FollowWorldAI;
import car.data.Car;
import car.ray.RayCarControl;
import game.App;
import helper.Log;

public class DriveDemo extends DriveBase {

	public DriveDemo (Car car, World world) {
    	super(car, world);
    	
    	if (!(world instanceof DefaultBuilder)) {
    		Log.e("DriveDemo has wrong world type.");
    		System.exit(-1);
    	}
    }
	
	@Override
	public void initialize(AppStateManager stateManager, Application app) {
    	super.initialize(stateManager, app);
    	
    	//remove all stuff we want and player from everything
    	
    	this.cb.removePlayer(0);
    	this.cb.addCar(0, car, world.getStartPos(), world.getStartRot(), true, null); //even though they aren't a player

    	RayCarControl car = this.cb.get(0);
    	car.attachAI(new FollowWorldAI(car, (DefaultBuilder)world));
    	
    	uiNode.cleanup(); //TODO why?
    	app.getStateManager().detach(uiNode);
    	uiNode = new CarUI(cb.get(0));
		app.getStateManager().attach(uiNode);
		
		App.rally.getStateManager().detach(camera);
		app.getInputManager().removeRawInputListener(camera);
		camera = new CarCamera("Camera", App.rally.getCamera(), cb.get(0));
		App.rally.getStateManager().attach(camera);
		app.getInputManager().addRawInputListener(camera);
		
//		App.rally.getRootNode().detachChild(minimap.rootNode); //TODO causes crash
//		minimap = new MiniMap(cb.get(0));
	}
	
	public void update(float tpf) {
		super.update(tpf);
	}
}
