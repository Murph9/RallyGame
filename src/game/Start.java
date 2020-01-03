package game;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import drive.IFlow;

public class Start extends BaseAppState {

	private final IFlow flow;

	private Container myWindow;
	
	public Start(IFlow flow) {
		super();
		this.flow = flow;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void initialize(Application app) {
		App myapp = ((App) app);

		//UI
		myWindow = new Container();
		myapp.getGuiNode().attachChild(myWindow);
		myWindow.setLocalTranslation(300, 300, 0);
		
        myWindow.addChild(new Label("Main Menu"));
		
		Map<String, Runnable> buttonActions = generateButtonMappings(flow);
		
		for (Entry<String, Runnable> action: buttonActions.entrySet()) {
			final Runnable method = action.getValue();
			Button startFast = myWindow.addChild(new Button(action.getKey()));
			startFast.addClickCommands(source -> {
				method.run();
				myapp.getGuiNode().detachChild(myWindow);
			});
		}
        
        Button exit = myWindow.addChild(new Button("Exit"));
        exit.addClickCommands(source -> myapp.stop());
	}
	
	public void update(float tpf) {
		if (!isEnabled() || !isInitialized())
			return;
		
		super.update(tpf);
	}
	
	@Override
	protected void onEnable() {}
	@Override
    protected void onDisable() {}
    
	@Override
	public void cleanup(Application app) {
        myWindow.removeFromParent();
		myWindow = null;
	}

	private Map<String, Runnable> generateButtonMappings(IFlow flow) {
		Map<String, Runnable> buttonActions = new LinkedHashMap<String, Runnable>();
		for (AppFlow.StartType t: AppFlow.StartType.values()) {
			buttonActions.put(t.name(), () -> { flow.startCallback(t); });
		}
		return buttonActions;
	}
}
