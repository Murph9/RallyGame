package survival.ability;

import java.util.HashMap;
import java.util.Map;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.DefaultRangedValueModel;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.Panel;
import com.simsilica.lemur.ProgressBar;
import com.simsilica.lemur.component.QuadBackgroundComponent;

import rallygame.helper.Colours;
import rallygame.service.Screen;

public class AbilityUI extends BaseAppState {

    private Container container;
    private Map<Ability, Panel> abilityMap;

    @Override
    protected void initialize(Application app) {
        container = new Container();
        abilityMap = new HashMap<>();

        ((SimpleApplication)app).getGuiNode().attachChild(container);
    }

    @Override
    protected void cleanup(Application app) {
        container.removeFromParent();
    }

    @Override
    protected void onEnable() {
        
    }

    @Override
    protected void onDisable() {
        
    }

    @Override
    public void update(float tpf) {
        var abilities = getState(AbilityManager.class).getAbilities();
        for (var ab: abilities) {
            if (!abilityMap.containsKey(ab)) {
                int count = abilities.size();

                var panel = new Container();
                var label = panel.addChild(new Label(""));
                label.setName("mainLabel");

                label = panel.addChild(new Label(""));
                label.setName("buttonLabel");

                var progress = panel.addChild(new ProgressBar());
                progress.setName("progress");

                progress.setProgressPercent(0);
                progress.setModel(new DefaultRangedValueModel(0, 1, 0));
                progress.getValueIndicator().setBackground(new QuadBackgroundComponent(Colours.getOnRGBScale(1 - 0)));

                abilityMap.put(ab, panel);
                container.addChild(panel, count);
            }
            
            var abilityPanel = abilityMap.get(ab);
            var l = (Label)abilityPanel.getChild("mainLabel");
            l.setText(ab.getClass().getSimpleName());

            var p = (ProgressBar)abilityPanel.getChild("progress");
            p.setProgressValue(ab.ready());

            var bl = (Label)abilityPanel.getChild("buttonLabel");
            bl.setText(getInputButtons(ab));
        }

        new Screen(getApplication().getContext().getSettings()).bottomCenterMe(container);
    }

    private static String getInputButtons(Ability ab) {
        if (ab instanceof ExplodeAbility) {
            return "(Key: L ALT or Left Stick)";
        }

        if (ab instanceof StopAbility) {
            return "(Key: R ALT or R Bumper)";
        }

        if (ab instanceof FreezeAbility) {
            return "(Key: Z or L Bumper)";
        }

        if (ab instanceof BlinkAbility) {
            return "(Key: Y or pull on right stick and let go)";
        }

        return null;
    }
}
