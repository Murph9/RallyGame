package survival.upgrade;

import java.util.List;

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
            b.addClickCommands((source) -> {
                manager.upgrade(type);
            });
        }

        return container;
    }

    public static Container GenerateSelectionUI(DodgeGameManager manager, List<UpgradeType> types) {
        return GenerateSelectionUI(manager, types.toArray(new UpgradeType[0]));
    }
}
