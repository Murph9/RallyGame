package survival;

import com.simsilica.lemur.Button;
import com.simsilica.lemur.Container;
import com.simsilica.lemur.HAlignment;

public class SelectionUI {
    @SuppressWarnings("unchecked") // button checked vargs
    public static Container GenerateSelectionUI(DodgeGameManager manager, UpgradeType[] types) {
        var container = new Container();

        for (var type : types) {
            Button b = container.addChild(new Button(type.label));
            b.setTextHAlignment(HAlignment.Center);
            b.addClickCommands((source) -> {
                manager.updateRules(type.ruleFunc);
            });
        }

        return container;
    }
}
