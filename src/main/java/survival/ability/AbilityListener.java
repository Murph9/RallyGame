package survival.ability;

import java.util.HashMap;
import java.util.Map;

import com.jme3.input.JoystickAxis;
import com.jme3.input.JoystickButton;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.Vector3f;

public class AbilityListener implements RawInputListener {

    private final AbilityManager manager;

    private final Map<String, String> axisToAction = new HashMap<>();
    private final Map<String, String> buttonToAction = new HashMap<>();
    private final Map<Integer, String> keyToAction = new HashMap<>();

    private boolean blinkDirectionSet = false;
    private float blinkBufferX = 0;
    private float blinkBufferZ = 0;

    public AbilityListener(AbilityManager manager) {
        this.manager = manager;

        buttonToAction.put(JoystickButton.BUTTON_8, AbilityManager.TYPE_EXPLODE);
        keyToAction.put(KeyInput.KEY_LMENU, AbilityManager.TYPE_EXPLODE); // left alt

        buttonToAction.put(JoystickButton.BUTTON_5, AbilityManager.TYPE_STOP);
        keyToAction.put(KeyInput.KEY_RMENU, AbilityManager.TYPE_STOP); // right alt
        
        buttonToAction.put(JoystickButton.BUTTON_4, AbilityManager.TYPE_FREEZE);
        keyToAction.put(KeyInput.KEY_Z, AbilityManager.TYPE_FREEZE); // right alt

        keyToAction.put(KeyInput.KEY_Y, AbilityManager.TYPE_BLINK);
        axisToAction.put(JoystickAxis.Z_AXIS, AbilityManager.TYPE_BLINK); //right stick
		axisToAction.put(JoystickAxis.Z_ROTATION, AbilityManager.TYPE_BLINK);
    }

    @Override
    public void beginInput() {}

    @Override
    public void endInput() {}

    @Override
    public void onJoyAxisEvent(JoyAxisEvent arg0) {
        JoystickAxis axis = arg0.getAxis();
		float value = arg0.getValue();
		arg0.isConsumed();

        if (!axisToAction.containsKey(axis.getLogicalId())) {
            return;
        }
        if (Math.abs(value) < 0.1f)
            return; //deadzone ew
        
		var action = axisToAction.get(axis.getLogicalId());
        if (action == AbilityManager.TYPE_BLINK) {
            if (axis.getLogicalId().equals(JoystickAxis.Z_AXIS)) {
                blinkBufferX = value;
            } else if (axis.getLogicalId().equals(JoystickAxis.Z_ROTATION)) {
                blinkBufferZ = value;
            }
        }

        if (Math.abs(blinkBufferX) > 0.8f || Math.abs(blinkBufferZ) > 0.8f)
            blinkDirectionSet = true;
        else {
            if (blinkDirectionSet) {
                manager.handlePress(action, new Vector3f(blinkBufferX, 0, blinkBufferZ));
            }
            blinkDirectionSet = false;
        }
    }

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
