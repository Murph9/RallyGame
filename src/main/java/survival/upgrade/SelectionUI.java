package survival.upgrade;

import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.HAlignment;

import survival.DodgeGameManager;

public class SelectionUI {
    @SuppressWarnings("unchecked") // button checked vargs
    public static Container GenerateSelectionUI(DodgeGameManager manager, UpgradeType[] types) {
        var container = new Container();

        for (var type : types) {
            Button b = container.addChild(new Button(type.label));
            b.setTextHAlignment(HAlignment.Center);
            if (type.ruleFunc != null)
                b.addClickCommands((source) -> {
                    manager.updateState(type.ruleFunc);
                });
            if (type.carFunc != null)
                b.addClickCommands((source) -> {
                    manager.updateCar(type.carFunc);
                });
        }

        return container;
    }
}
