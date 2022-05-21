package survival;

import java.util.Map;

import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.Panel;

import rallygame.helper.H;

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
}
