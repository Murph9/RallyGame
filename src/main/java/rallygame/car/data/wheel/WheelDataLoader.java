package rallygame.car.data.wheel;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import rallygame.car.ray.GripHelper;
import rallygame.helper.Log;

public class WheelDataLoader {
    
    private static Map<WheelTractionType, WheelTraction> dataCache = new HashMap<>();

    public WheelDataLoader() throws Exception {
        for (var type: WheelTractionType.values()) {
            var data = loadFromFile(type);
            dataCache.put(type, data);
        }

        // validate
        for (var data: dataCache.values()) {
            // generate the slip* max force from the car wheel data, and validate they are 'real'
            data.pjk_lat.max = GripHelper.calcSlipMax(data.pjk_lat);
            if (Float.isNaN(data.pjk_lat.max) || data.pjk_lat.max <= 0)
                throw new IllegalStateException("maxLat was: '" + data.pjk_lat.max + "'.");

            data.pjk_long.max = GripHelper.calcSlipMax(data.pjk_long);
            if (Float.isNaN(data.pjk_long.max) || data.pjk_long.max <= 0)
                throw new IllegalStateException("maxLong was: '" + data.pjk_long.max + "'.");
        }
    }
    
    public WheelTraction get(WheelTractionType type) {
        return dataCache.get(type);
    }

    private WheelTraction loadFromFile(WheelTractionType type) throws Exception {
        InputStream in = getClass().getResourceAsStream(type.getFileName());
        Yaml yaml = new Yaml(new Constructor(WheelTraction.class));
        Object yamlData = yaml.load(in);

        if (yamlData == null) {
            Log.e("Loading data for wheel: " + type + " did not go well.");
            return null;
        }
        if (!(yamlData instanceof WheelTraction)) {
            Log.e("Loading data for wheel: " + type + " did not go that well.");
            return null;
        }

        return (WheelTraction) yamlData;
    }
}
