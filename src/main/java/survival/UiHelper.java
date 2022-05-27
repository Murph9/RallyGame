package survival;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.Panel;

import rallygame.helper.H;
import survival.upgrade.UpgradeType;

public class UiHelper {
    
    public static Panel generateTableOfValues(Map<String, Object> map) {
        var panel = new Container();
        for (var entry: map.entrySet()) {
            panel.addChild(new Label(entry.getKey()));
            var value = entry.getValue();
            if (value instanceof Float) {
                panel.addChild(new Label(H.roundDecimal(((Float)value), 2)), 1);
            } else {
                panel.addChild(new Label(value.toString()));
            }
        }
        
        return panel;
    }

    public static Panel generateTableOfValues(List<UpgradeType> types) {
        
        var asCounts = types.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        var panel = new Container();
        
        for (var entry: asCounts.entrySet()) {
            panel.addChild(new Label(entry.getKey().name()));
            panel.addChild(new Label(entry.getValue().toString()), 1);
        }

        return panel;
    }
}
