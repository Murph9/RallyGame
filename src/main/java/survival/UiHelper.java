package survival;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.simsilica.lemur.Container;
import com.simsilica.lemur.Label;
import com.simsilica.lemur.Panel;

import rallygame.helper.Duo;
import rallygame.helper.H;
import survival.upgrade.Upgrade;

public class UiHelper {
    
    public static Panel generateTableOfValues(Map<String, Object> map) {
        var panel = new Container();
        for (var entry: map.entrySet()) {
            panel.addChild(new Label(entry.getKey()));
            panel.addChild(new Label(renderObj(entry.getValue())), 1);
        }
        
        return panel;
    }

    @SuppressWarnings("unchecked")
    private static String renderObj(Object value) {
        if (value instanceof Float) {
            return H.roundDecimal(((Float)value), 2);
        } else if (value instanceof Duo) {
            var duo = (Duo<Object,Object>)value;
            return renderObj(duo.first) + " (" + renderObj(duo.second) + ")";
        } else if (value instanceof Object[]) {
            var array = (Object[])value;
            var result = "";
            for (var a: array) {
                result += renderObj(a)+",";
            }
            return result;
        }
        
        return value.toString();
    }

    public static Panel generateTableOfValues(List<Upgrade<?>> types) {
        
        var asCounts = types.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        var panel = new Container();
        
        for (var entry: asCounts.entrySet()) {
            panel.addChild(new Label(entry.getKey().label));
            panel.addChild(new Label(entry.getValue().toString()), 1);
        }

        return panel;
    }
}
