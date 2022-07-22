package rallygame.car.data;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import rallygame.car.ray.GripHelper;
import rallygame.helper.Log;

public class WheelDataLoader {
    
    private static Map<Wheel, WheelTypeConst> dataCache = new HashMap<>();

    public WheelDataLoader() throws Exception {
        for (var type: Wheel.values()) {
            var data = loadFromFile(type);
            dataCache.put(type, data);
        }

        // validate
        for (var data: dataCache.values()) {
            // generate the slip* max force from the car wheel data, and validate they are 'real'
            data.pjk_lat._max = GripHelper.calcSlipMax(data.pjk_lat);
            data.pjk_long._max = GripHelper.calcSlipMax(data.pjk_long);

            if (Float.isNaN(data.pjk_lat._max))
                throw new IllegalStateException("maxLat was: '" + data.pjk_lat._max + "'.");
            if (Float.isNaN(data.pjk_long._max))
                throw new IllegalStateException("maxLong was: '" + data.pjk_long._max + "'.");
        }
    }
    
    public WheelTypeConst get(Wheel type) {
        return dataCache.get(type);
    }

    private WheelTypeConst loadFromFile(Wheel type) throws Exception {
        InputStream in = getClass().getResourceAsStream(type.getFileName());
        var yaml = new Yaml(new Constructor(WheelTypeConst.class));
        var yamlData = yaml.load(in);

        if (yamlData == null) {
            Log.e("Loading data for wheel: " + type + " did not go well.");
            return null;
        }
        if (!(yamlData instanceof WheelTypeConst)) {
            Log.e("Loading data for wheel: " + type + " did not go that well.");
            return null;
        }

        return (WheelTypeConst) yamlData;
    }
}
