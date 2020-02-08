package drive;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import helper.Log;
import service.Screen;

public class DriveMenu extends BaseAppState implements PauseState.ICallback {

	protected final IDrive drive;
	private PauseState pauseState;
	
	//random Label to print to the screen to show the user, assumed settable by 'Drive*'
	public Label randomthing;
	private Container random;
		
	public DriveMenu(IDrive drive) {
		super();
		this.drive = drive;
	}

	@Override
	public void initialize(Application app) {
        this.pauseState = new PauseState(this);
        getStateManager().attach(this.pauseState);

		
		//init gui
        Screen screen = new Screen(app.getContext().getSettings());
        SimpleApplication sm = (SimpleApplication) app;

		random = new Container();
		randomthing = new Label("");
		random.attachChild(randomthing);
		random.setLocalTranslation(screen.topRight().add(-100, 0, 0));
		sm.getGuiNode().attachChild(random);
	}
	
	@Override
	protected void onEnable() {
	}
	@Override
	protected void onDisable() {
	}
    
    @Override
	public void update(float tpf) {
		super.update(tpf);
	}
    
    @Override
	public void cleanup(Application app) {
        getStateManager().detach(this.pauseState);
        this.pauseState = null;

        SimpleApplication sm = (SimpleApplication) app;
        sm.getGuiNode().detachChild(random);
        random = null;
	}

    @Override
    public void pauseState(boolean value) {
        drive.setEnabled(value);
    }

    @Override
    public void quit() {
        if (drive != null)
            drive.next();
        else {
            Log.e("!!!unknown state");
        }
    }
}
