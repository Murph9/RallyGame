package effects;

import java.util.HashMap;
import java.util.Map;

import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.post.Filter;

//Allows management of the visual filters because sometimes they are annoying
public class FilterManager {

    private Filter[] filters;

    public FilterManager(InputManager inputManager, Filter[] filters) {
        this.filters = filters;

        inputManager.addRawInputListener(new FilterListener(this));
    }

    void onAction(Integer i, boolean value, float tpf) {
        if (!value)
            return;
        
        if (i < filters.length) {
            boolean state = filters[i].isEnabled();
            filters[i].setEnabled(!state);
        }
    }

    class FilterListener implements RawInputListener {

        FilterManager fm;
        Map<Integer, Integer> layout = new HashMap<Integer, Integer>();
        
        public FilterListener(FilterManager fm) {
            this.fm = fm;
            
            layout.put(KeyInput.KEY_F1, 0);
            layout.put(KeyInput.KEY_F2, 1);
            layout.put(KeyInput.KEY_F3, 2);
            layout.put(KeyInput.KEY_F4, 3);
            layout.put(KeyInput.KEY_F5, 4);
            layout.put(KeyInput.KEY_F6, 5);
            layout.put(KeyInput.KEY_F7, 6);
            layout.put(KeyInput.KEY_F8, 7);
            layout.put(KeyInput.KEY_F9, 8);
            layout.put(KeyInput.KEY_F10, 9);
            layout.put(KeyInput.KEY_F11, 10);
            layout.put(KeyInput.KEY_F12, 11);
        }
        
        public void beginInput() {}
        public void endInput() {}
        
        public void onKeyEvent(KeyInputEvent arg0) {
            if (layout.containsKey(arg0.getKeyCode())) {
                fm.onAction(layout.get(arg0.getKeyCode()), arg0.isPressed(), arg0.isPressed() ? 1 : 0);
            }
        }
        
        public void onMouseButtonEvent(MouseButtonEvent arg0) {}
        public void onMouseMotionEvent(MouseMotionEvent arg0) {}
        public void onTouchEvent(TouchEvent arg0) {}
        public void onJoyAxisEvent(JoyAxisEvent arg0) {}
        public void onJoyButtonEvent(JoyButtonEvent arg0) {}
    }
}