package survival.hotmenu;

import java.util.HashMap;
import java.util.Map;

import com.jme3.input.JoystickAxis;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;

public class HotMenuJoystickListener implements RawInputListener {
    
    public static final String ACTION_LEFT = "Left";
    public static final String ACTION_RIGHT = "Right";
    public static final String ACTION_UP = "Up";
    public static final String ACTION_DOWN = "Down";

    private final HotMenu menu;
	private final Map<String, String> dpadToButton = new HashMap<>();
    private final Map<String, String> buttonToAction = new HashMap<>();

    private final Map<Integer, String> keyToButton = new HashMap<>();
	
	public HotMenuJoystickListener(HotMenu menu) {
        this.menu = menu;

		dpadToButton.put(JoystickAxis.POV_X, "LR");
        dpadToButton.put(JoystickAxis.POV_Y, "UD");

        buttonToAction.put("LR-1", ACTION_LEFT);
        buttonToAction.put("LR1", ACTION_RIGHT);
        buttonToAction.put("UD1", ACTION_UP);
        buttonToAction.put("UD-1", ACTION_DOWN);

        keyToButton.put(KeyInput.KEY_I, ACTION_UP);
        keyToButton.put(KeyInput.KEY_K, ACTION_DOWN);
        keyToButton.put(KeyInput.KEY_J, ACTION_LEFT);
        keyToButton.put(KeyInput.KEY_L, ACTION_RIGHT);
    }
    
    @Override
    public void onJoyAxisEvent(JoyAxisEvent arg0) {
        JoystickAxis axis = arg0.getAxis();
        arg0.isConsumed();
        // get only the dpad

        if (axis.isAnalog())
            return;

        var dir = dpadToButton.get(axis.getLogicalId());
        var value = arg0.getValue();
        if (value != 0) {
            var action = buttonToAction.get(dir+(int)value);
            menu.input(action);
        }
    }

    public void onJoyButtonEvent(JoyButtonEvent arg0) { }
    public void beginInput() {}
    public void endInput() {}
    public void onKeyEvent(KeyInputEvent arg0) {
        if (arg0.isReleased())
            return;

        if (keyToButton.containsKey(arg0.getKeyCode()))
            menu.input(keyToButton.get(arg0.getKeyCode()));
    }
    public void onMouseButtonEvent(MouseButtonEvent arg0) {}
    public void onMouseMotionEvent(MouseMotionEvent arg0) {}
    public void onTouchEvent(TouchEvent arg0) {}
}
