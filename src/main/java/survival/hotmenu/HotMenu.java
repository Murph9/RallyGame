package survival.hotmenu;

import java.util.LinkedList;
import java.util.List;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;

import rallygame.helper.H;
import rallygame.service.Screen;
import rallygame.service.Screen.HorizontalPos;
import rallygame.service.Screen.VerticalPos;
import survival.DodgeGameManager;
import survival.upgrade.Upgrade;

public class HotMenu extends BaseAppState {

    private final HotMenuJoystickListener listener;
    private Container panel;
    private Container optionPanel;
    
    private int menuSelection = 0;
    private List<MenuItem> menuElements = new LinkedList<>();

    class MenuItem {
        public final String label;
        public final Upgrade<?> type;
        public Container ui;
        public MenuItem(String label, Upgrade<?> type) {
            this.label = label;
            this.type = type;
        }
    }

    public HotMenu() {
        listener = new HotMenuJoystickListener(this);
    }

    @Override
    protected void initialize(Application app) {
        app.getInputManager().addRawInputListener(listener);

        panel = new Container();
        panel.addChild(new Label("Choose an option"));
        optionPanel = panel.addChild(new Container());

        initOptionPanel();
        
        ((SimpleApplication)app).getGuiNode().attachChild(panel);
    }

    @Override
    protected void cleanup(Application app) {
        app.getInputManager().removeRawInputListener(listener);

        panel.removeFromParent();
        panel = null;
    }

    @Override
    protected void onEnable() { }
    @Override
    protected void onDisable() { }
    
    public void addOptions(List<Upgrade<?>> types) {
        for (var t: types) {
            menuElements.add(new MenuItem(t.label, t));
        }
        initOptionPanel();
    }

    public void removeAllOptions() {
        menuElements.clear();
        initOptionPanel();
    }

    private void initOptionPanel() {
        optionPanel.clearChildren();
        for (var el: menuElements) {
            el.ui = optionPanel.addChild(generateRow(el));
        }
    }

    @Override
    public void update(float tpf) {
        var screen = new Screen(getApplication().getContext().getSettings());
        screen.posMe(panel, HorizontalPos.Right, VerticalPos.Middle);
        
        if (menuElements.size() < 1) {
            panel.setLocalScale(0);
        } else {
            panel.setLocalScale(1);
        }

        for (int i = 0; i < menuElements.size(); i++) {
            var el = menuElements.get(i);
            var label = (Label)el.ui.getChild(1);
            if (i == menuSelection)
                label.setText("(selected)");
            else // TODO much better than this
                label.setText("");
        }
    }

    protected void input(String action) {
        if (action == HotMenuJoystickListener.ACTION_DOWN) {
            menuSelection++;
        }
        if (action == HotMenuJoystickListener.ACTION_UP) {
            menuSelection--;
        }

        menuSelection = H.clamp(menuSelection, 0, menuElements.size() - 1);

        if (action == HotMenuJoystickListener.ACTION_RIGHT) {
            // select the thingo
            getState(DodgeGameManager.class).upgrade(menuElements.get(menuSelection).type);
        }
    }

    private static Container generateRow(MenuItem item) {
        var output = new Container();
        output.addChild(new Label(item.label));
        output.addChild(new Label(""), 1);
        return output;
    }
}
