package game;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.input.KeyInput;
import com.jme3.input.RawInputListener;
import com.jme3.input.event.JoyAxisEvent;
import com.jme3.input.event.JoyButtonEvent;
import com.jme3.input.event.KeyInputEvent;
import com.jme3.input.event.MouseButtonEvent;
import com.jme3.input.event.MouseMotionEvent;
import com.jme3.input.event.TouchEvent;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;

import java.util.HashMap;
import java.util.Map;

import helper.H;

public class DebugAppState extends BaseAppState {

    private boolean debug;
    private Node node;
    private Map<String, Geometry> thingSet;

    public DebugAppState(boolean debug) {
        this.debug = debug;
        this.thingSet = new HashMap<String, Geometry>();
    }

    @Override
    protected void initialize(Application app) {
        this.node = new Node("DebugState");
        ((SimpleApplication)app).getRootNode().attachChild(node);
    }

    @Override
    protected void cleanup(Application app) {
        node.removeFromParent();
        this.node = null;
    }

    @Override
    protected void onEnable() {
    }

    @Override
    protected void onDisable() {
    }

    public boolean DEBUG() {
        return this.debug;
    }

    public void drawArrow(String key, ColorRGBA colour, Vector3f pos, Vector3f dir) {
        if (!debug)
            return;

        addThing(key, H.makeShapeArrow(getApplication().getAssetManager(), colour, dir, pos));
    }

    public void drawBox(String key, ColorRGBA colour, Vector3f pos, float size) {
        if (!debug)
            return;

        addThing(key, H.makeShapeBox(getApplication().getAssetManager(), colour, pos, size));
    }

    private void addThing(String key, Geometry thing) {
        if (thing == null)
            return; //not a valid objet, then not a vaild thing

        Application app = getApplication();
        app.enqueue(() -> {
            // check if key exists, if yes remove it (from view)
            if (thingSet.containsKey(key)) {
                Geometry g = thingSet.get(key);
                this.node.detachChild(g);
                thingSet.remove(key);
            }

            // add obj with key
            this.node.attachChild(thing);
            thingSet.put(key, thing);
        });
    }

    void toggleDebug() {
        this.debug = !this.debug;
    }

    class DebugListener implements RawInputListener {
        
        private DebugAppState state;
        public DebugListener(DebugAppState state) {
            this.state = state;
        }

        public void beginInput() {}
        public void endInput() {}

        public void onKeyEvent(KeyInputEvent arg0) {
            if (arg0.isPressed() && arg0.getKeyCode() == KeyInput.KEY_GRAVE)
                state.toggleDebug();
        }

        public void onMouseButtonEvent(MouseButtonEvent arg0) {}
        public void onMouseMotionEvent(MouseMotionEvent arg0) {}
        public void onTouchEvent(TouchEvent arg0) {}
        public void onJoyAxisEvent(JoyAxisEvent arg0) {}
        public void onJoyButtonEvent(JoyButtonEvent arg0) {}
    }
}