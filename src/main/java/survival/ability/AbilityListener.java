package survival.ability;

import java.util.HashMap;
import java.util.Map;

import com.jme3.input.JoystickButton;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;

public class AbilityListener implements RawInputListener {

    private final AbilityManager manager;

    private final Map<String, String> buttonToAction = new HashMap<>();
    private final Map<Integer, String> keyToAction = new HashMap<>();

    public AbilityListener(AbilityManager manager) {
        this.manager = manager;

        buttonToAction.put(JoystickButton.BUTTON_8, AbilityManager.TYPE_EXPLODE);
        keyToAction.put(KeyInput.KEY_LMENU, AbilityManager.TYPE_EXPLODE); // left alt

        buttonToAction.put(JoystickButton.BUTTON_5, AbilityManager.TYPE_STOP);
        keyToAction.put(KeyInput.KEY_RMENU, AbilityManager.TYPE_STOP); // right alt
        
        buttonToAction.put(JoystickButton.BUTTON_4, AbilityManager.TYPE_FREEZE);
        keyToAction.put(KeyInput.KEY_Z, AbilityManager.TYPE_FREEZE); // right alt
        //more..
    }

    @Override
    public void beginInput() {}

    @Override
    public void endInput() {}

    @Override
    public void onJoyAxisEvent(JoyAxisEvent evt) {}

    @Override
    public void onJoyButtonEvent(JoyButtonEvent arg0) {
        var button = Integer.toString(arg0.getButtonIndex());
        if (buttonToAction.containsKey(button)) {
		    manager.handlePress(buttonToAction.get(button));
        }
		arg0.isConsumed();
    }

    @Override
    public void onMouseMotionEvent(MouseMotionEvent evt) { }

    @Override
    public void onMouseButtonEvent(MouseButtonEvent evt) { }

    @Override
    public void onKeyEvent(KeyInputEvent evt) {
        if (keyToAction.containsKey(evt.getKeyCode())) {
            manager.handlePress(keyToAction.get(evt.getKeyCode()));
        }
		evt.isConsumed();
    }

    @Override
    public void onTouchEvent(TouchEvent evt) { }
}
